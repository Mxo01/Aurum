import { expect, test, type APIResponse, type Page } from "@playwright/test";

const MANIFEST_PATH = "/site.webmanifest";
const APPLE_TOUCH_ICON_PATH = "/apple-touch-icon.png";
const OG_IMAGE_PATH = "/og-image.png";
const FAVICON_PATH = "/favicon.ico";
const FAVICON_SVG_PATH = "/favicon.svg";

const MANIFEST_CONTENT_TYPE = "application/manifest+json";
const PNG_CONTENT_TYPE = "image/png";
const ICO_CONTENT_TYPES = ["image/x-icon", "image/vnd.microsoft.icon"];

const PNG_SIGNATURE = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);
const ICO_SIGNATURE = Buffer.from([0x00, 0x00, 0x01, 0x00]);

const HTML_FALLBACK_CONTENT_TYPE = "text/html";
const HTML_FALLBACK_PREFIX = "<!doctype html";

const MINIMUM_PNG_BYTES = 512;
const MINIMUM_ICO_BYTES = 512;

const APP_NAME = "Aurum";
const MANIFEST_DISPLAY = "standalone";
const MANIFEST_START_URL = "/";
const BRAND_BACKGROUND_COLOR = "#09090b";
const BRAND_THEME_COLOR = "#09090b";

const OG_IMAGE_URL = "https://aurum-networth.com/og-image.png";

interface ManifestIcon {
	src: string;
	sizes: string;
	type: string;
	purpose?: string;
}

interface WebAppManifest {
	name: string;
	short_name: string;
	display: string;
	start_url: string;
	background_color: string;
	theme_color: string;
	icons: ManifestIcon[];
}

const EXPECTED_MANIFEST_ICONS: readonly ManifestIcon[] = [
	{ src: APPLE_TOUCH_ICON_PATH, sizes: "180x180", type: PNG_CONTENT_TYPE },
	{ src: FAVICON_PATH, sizes: "16x16 32x32", type: ICO_CONTENT_TYPES[0] },
	{ src: FAVICON_SVG_PATH, sizes: "any", type: "image/svg+xml", purpose: "any maskable" }
];

interface BinaryAssetFixture {
	readonly kind: "binary";
	readonly path: string;
	readonly acceptedContentTypes: readonly string[];
	readonly signature: Buffer;
	readonly minimumBytes: number;
}

interface ManifestAssetFixture {
	readonly kind: "manifest";
	readonly path: string;
	readonly acceptedContentTypes: readonly string[];
}

type StaticAssetFixture = BinaryAssetFixture | ManifestAssetFixture;

const STATIC_ASSET_FIXTURES: readonly StaticAssetFixture[] = [
	{ kind: "manifest", path: MANIFEST_PATH, acceptedContentTypes: [MANIFEST_CONTENT_TYPE] },
	{
		kind: "binary",
		path: APPLE_TOUCH_ICON_PATH,
		acceptedContentTypes: [PNG_CONTENT_TYPE],
		signature: PNG_SIGNATURE,
		minimumBytes: MINIMUM_PNG_BYTES
	},
	{
		kind: "binary",
		path: OG_IMAGE_PATH,
		acceptedContentTypes: [PNG_CONTENT_TYPE],
		signature: PNG_SIGNATURE,
		minimumBytes: MINIMUM_PNG_BYTES
	},
	{
		kind: "binary",
		path: FAVICON_PATH,
		acceptedContentTypes: ICO_CONTENT_TYPES,
		signature: ICO_SIGNATURE,
		minimumBytes: MINIMUM_ICO_BYTES
	}
];

interface DeclaredAssets {
	manifestUrl: string;
	appleTouchIconUrl: string;
	ogImageContent: string;
}

async function readDeclaredAssets(page: Page): Promise<DeclaredAssets> {
	const manifestUrl = await page
		.locator('link[rel="manifest"]')
		.evaluate((element: HTMLLinkElement) => element.href);
	const appleTouchIconUrl = await page
		.locator('link[rel="apple-touch-icon"]')
		.evaluate((element: HTMLLinkElement) => element.href);
	const ogImageContent = await page
		.locator('meta[property="og:image"]')
		.evaluate((element: HTMLMetaElement) => element.content);

	return { manifestUrl, appleTouchIconUrl, ogImageContent };
}

function contentTypesOf(response: APIResponse) {
	const header = response.headers()["content-type"] ?? "";

	return header.split(";").map(value => value.trim().toLowerCase());
}

for (const fixture of STATIC_ASSET_FIXTURES) {
	test(`serves ${fixture.path} as a real ${fixture.acceptedContentTypes.join(" / ")} asset`, async ({
		request
	}) => {
		// GIVEN
		const expectedContentTypes = fixture.acceptedContentTypes;

		// WHEN
		const testSubject = await request.get(fixture.path);
		const body = await testSubject.body();
		const contentTypes = contentTypesOf(testSubject);

		// THEN
		expect(
			contentTypes.some(contentType => expectedContentTypes.includes(contentType)),
			`${fixture.path} must be served as ${expectedContentTypes.join(" or ")}, got "${contentTypes.join(";")}"`
		).toBe(true);
		expect(
			contentTypes.includes(HTML_FALLBACK_CONTENT_TYPE),
			`${fixture.path} was answered by the SPA rewrite with the index.html fallback`
		).toBe(false);
		expect(
			body.subarray(0, HTML_FALLBACK_PREFIX.length).toString("utf8").toLowerCase(),
			`${fixture.path} body starts with the HTML fallback instead of its own bytes`
		).not.toBe(HTML_FALLBACK_PREFIX);

		if (fixture.kind === "binary") {
			expect(
				body.subarray(0, fixture.signature.length),
				`${fixture.path} must start with the file signature`
			).toEqual(fixture.signature);
			expect(body.length, `${fixture.path} must not be a stub`).toBeGreaterThan(
				fixture.minimumBytes
			);
			return;
		}

		const testSubjectManifest = JSON.parse(body.toString("utf8")) as WebAppManifest;

		expect(testSubjectManifest.name).toBe(APP_NAME);
		expect(testSubjectManifest.short_name).toBe(APP_NAME);
		expect(testSubjectManifest.display).toBe(MANIFEST_DISPLAY);
		expect(testSubjectManifest.start_url).toBe(MANIFEST_START_URL);
		expect(testSubjectManifest.background_color).toBe(BRAND_BACKGROUND_COLOR);
		expect(testSubjectManifest.theme_color).toBe(BRAND_THEME_COLOR);
		expect(testSubjectManifest.icons).toHaveLength(EXPECTED_MANIFEST_ICONS.length);
		for (const expectedIcon of EXPECTED_MANIFEST_ICONS) {
			const expectedDeclaredIcon = testSubjectManifest.icons.find(
				icon => icon.src === expectedIcon.src
			);

			expect(expectedDeclaredIcon, `manifest must declare icon ${expectedIcon.src}`).toBeDefined();
			expect(expectedDeclaredIcon?.sizes).toBe(expectedIcon.sizes);
			expect(expectedDeclaredIcon?.type).toBe(expectedIcon.type);
			if (expectedIcon.purpose !== undefined) {
				expect(expectedDeclaredIcon?.purpose).toBe(expectedIcon.purpose);
			}
		}
	});
}

test("declares the manifest, the apple touch icon and a served og image on the document", async ({
	page,
	request
}) => {
	// GIVEN
	await page.goto("/");

	// WHEN
	const testSubject = await readDeclaredAssets(page);
	const ogImageResponse = await request.get(new URL(testSubject.ogImageContent).pathname);
	const ogImageBody = await ogImageResponse.body();

	// THEN
	expect(new URL(testSubject.manifestUrl).pathname).toBe(MANIFEST_PATH);
	expect(new URL(testSubject.appleTouchIconUrl).pathname).toBe(APPLE_TOUCH_ICON_PATH);
	expect(testSubject.ogImageContent).toBe(OG_IMAGE_URL);
	expect(contentTypesOf(ogImageResponse)).toContain(PNG_CONTENT_TYPE);
	expect(ogImageBody.subarray(0, PNG_SIGNATURE.length)).toEqual(PNG_SIGNATURE);
	expect(ogImageBody.length).toBeGreaterThan(MINIMUM_PNG_BYTES);
});
