import type { Page } from "@playwright/test";
import { expect, test } from "./fixtures/aurum";

const LANDING_SURFACE_CLASS = "landing-surface";
const LANDING_HEADING = /Your net worth/;
const DASHBOARD_URL = /\/dashboard$/;
const LOGOUT_URL_PATTERN = "**/v2/logout**";
const REDIRECT = 302;

declare global {
	interface Window {
		__landingSurfaceMounts?: number;
	}
}

// A flash of the landing would already be gone by the time the redirect settles,
// so count every node that enters the DOM with the landing class instead.
async function recordLandingSurfaceMounts(page: Page) {
	await page.addInitScript((className: string) => {
		window.__landingSurfaceMounts = 0;

		const entersLandingSurface = (node: Node) =>
			node instanceof Element &&
			(node.classList.contains(className) || node.querySelector(`.${className}`) !== null);

		new MutationObserver(records => {
			for (const record of records) {
				if (record.type === "attributes") {
					if (entersLandingSurface(record.target)) {
						window.__landingSurfaceMounts = (window.__landingSurfaceMounts ?? 0) + 1;
					}
					continue;
				}

				for (const node of Array.from(record.addedNodes)) {
					if (entersLandingSurface(node)) {
						window.__landingSurfaceMounts = (window.__landingSurfaceMounts ?? 0) + 1;
					}
				}
			}
		}).observe(document, {
			childList: true,
			subtree: true,
			attributes: true,
			attributeFilter: ["class"]
		});
	}, LANDING_SURFACE_CLASS);
}

async function logOut(page: Page) {
	const accountMenu = page.getByRole("button", { name: "Account menu" });

	if (await accountMenu.isVisible()) {
		await accountMenu.click();
		await page.getByRole("menuitem", { name: "Log out" }).click();
		return;
	}

	await page.getByRole("button", { name: "Profile" }).click();
	await page.getByRole("dialog").getByRole("button", { name: "Log out" }).click();
}

test("signed in, a visit to the landing redirects to the dashboard without painting it", async ({
	page
}) => {
	// GIVEN
	await recordLandingSurfaceMounts(page);

	// WHEN
	await page.goto("/");

	// THEN
	await expect(page).toHaveURL(DASHBOARD_URL);
	await expect(page.getByRole("heading", { name: "Overview" })).toBeVisible();
	expect(await page.evaluate(() => window.__landingSurfaceMounts)).toBe(0);
});

test("signed in, the privacy policy still renders from its own URL", async ({ page }) => {
	await page.goto("/privacy");

	await expect(page.getByRole("heading", { name: "Privacy Policy" })).toBeVisible();
	await expect(page).toHaveURL(/\/privacy$/);
});

test("signed in, a protected deep link opens without a login bounce", async ({ page }) => {
	await page.goto("/assets");

	await expect(page.getByRole("heading", { name: "Assets" })).toBeVisible();
	await expect(page).toHaveURL(/\/assets$/);
});

test("signed in, logging out returns to the landing at the root", async ({ page }) => {
	// GIVEN
	await page.goto("/dashboard");
	await expect(page.getByRole("heading", { name: "Overview" })).toBeVisible();

	const appOrigin = new URL(page.url()).origin;
	await page.route(LOGOUT_URL_PATTERN, route =>
		route.fulfill({ status: REDIRECT, headers: { location: `${appOrigin}/` } })
	);

	// WHEN
	await logOut(page);

	// THEN
	await expect(page).toHaveURL(/\/$/);
	await expect(page.getByRole("heading", { name: LANDING_HEADING })).toBeVisible();
	await expect(page).not.toHaveURL(DASHBOARD_URL);
});

test.describe("guest", () => {
	test.use({ signedIn: false });

	test("signed out, the landing stays at the root instead of redirecting", async ({ page }) => {
		// GIVEN
		await recordLandingSurfaceMounts(page);

		// WHEN
		await page.goto("/");

		// THEN
		await expect(page.getByRole("heading", { name: LANDING_HEADING })).toBeVisible();
		await expect(page).toHaveURL(/\/$/);
		await expect(page.getByRole("button", { name: "Get started", exact: true })).toBeVisible();
		// Positive control: the observer does see the landing when it is painted.
		expect(await page.evaluate(() => window.__landingSurfaceMounts)).toBeGreaterThan(0);
	});
});
