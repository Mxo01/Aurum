import type { Locator, Page } from "@playwright/test";
import { expect, test } from "./fixtures/aurum";

/**
 * Geometry of the installed-PWA bottom bar, measured in a real browser.
 *
 * The baseline was measured on the mobile project's 390x844 viewport, in a
 * browser tab where `env(safe-area-inset-bottom)` resolves to 0px: a 60.703125px
 * nav holding a 59.703125px icon row plus its 1px border, and 96px of clearance
 * under the page content. Fractional values carry sub-pixel layout, so they are
 * compared with a tolerance; the paddings are integers and are compared exactly.
 */
const SURFACE_HEIGHT_PX = 60.703125;
const NAV_HEIGHT_AT_ZERO_INSET_PX = 60.703125;
const MAIN_PADDING_AT_ZERO_INSET_PX = 96;
const NAV_HEIGHT_AT_INSET_PX = 94.703125;
const MAIN_PADDING_AT_INSET_PX = 130;
const STRIP_HEIGHT_AT_INSET_PX = 34;

/** Stands in for the home indicator: Chromium cannot emulate env(), so the token is overridden. */
const SAFE_AREA_INSET_PX = 34;
const ZERO_PX = 0;
const FRACTIONAL_TOLERANCE_PX = 0.5;
const MIN_END_OF_SCROLL_CLEARANCE_PX = 30;
const CLEARANCE_PROBE_HEIGHT_PX = 2000;

const MOBILE_PROJECT = "mobile";
const BOTTOM_BAR_LABEL = "Main";
const PROFILE_LABEL = "Profile";
const THEME_OPTION_LABEL = /theme/i;

type Theme = "light" | "dark";

/** The option offers the *other* theme: it reads "Dark theme" while the app is light. */
const THEME_OPTION_NAMES: Record<Theme, string> = { light: "Dark theme", dark: "Light theme" };

interface BarGeometry {
	navHeight: number;
	surfaceHeight: number;
	navPaddingBottom: string;
	stripHeight: number;
	stripAriaHidden: string | null;
	stripPointerEvents: string;
	mainPaddingBottom: string;
}

interface EndOfScrollMeasurement {
	gap: number;
	navHeight: number;
	scrollY: number;
}

/** The bottom bar only renders below `md`; the desktop project would fail on a hidden element. */
function requireMobileProject() {
	test.skip(
		test.info().project.name !== MOBILE_PROJECT,
		"The bottom bar only exists below the md breakpoint."
	);
}

/**
 * Two navs carry the label: the desktop top bar, first in the DOM and display:none
 * at 390px, and this mobile bar. Pick the rendered one rather than an index.
 */
function bottomBar(page: Page): Locator {
	return page.getByRole("navigation", { name: BOTTOM_BAR_LABEL }).filter({ visible: true });
}

/**
 * A single task after a style change still reports the pre-substitution value in
 * Chromium, so every measurement waits two animation frames first.
 */
async function flushFrames(page: Page) {
	await page.evaluate(
		() =>
			new Promise<void>(resolve => {
				requestAnimationFrame(() => requestAnimationFrame(() => resolve()));
			})
	);
}

async function applySafeAreaInset(page: Page, insetPx: number) {
	await page.evaluate(value => {
		document.documentElement.style.setProperty("--safe-area-bottom", `${value}px`);
	}, insetPx);
	await flushFrames(page);
}

async function readBarGeometry(page: Page): Promise<BarGeometry> {
	return bottomBar(page).evaluate(bar => {
		const surface = bar.firstElementChild;
		const strip = bar.lastElementChild;
		const main = document.querySelector("main");
		if (!surface || !strip || !main) throw new Error("The page shell changed shape.");

		return {
			navHeight: bar.getBoundingClientRect().height,
			surfaceHeight: surface.getBoundingClientRect().height,
			navPaddingBottom: getComputedStyle(bar).paddingBottom,
			stripHeight: strip.getBoundingClientRect().height,
			stripAriaHidden: strip.getAttribute("aria-hidden"),
			stripPointerEvents: getComputedStyle(strip).pointerEvents,
			mainPaddingBottom: getComputedStyle(main).paddingBottom
		};
	});
}

/** Proves the browser itself reports no home indicator: that is why the token override exists. */
async function readSafeAreaInsetBottom(page: Page) {
	return page.evaluate(() => {
		const probe = document.createElement("div");
		probe.style.height = "env(safe-area-inset-bottom, 0px)";
		document.body.append(probe);
		const resolved = getComputedStyle(probe).height;
		probe.remove();
		return resolved;
	});
}

/**
 * Grows the page past the viewport with a block of our own, scrolls to the very end
 * and measures how much room the bar leaves under it. Independent of fixture content.
 */
async function measureEndOfScroll(page: Page): Promise<EndOfScrollMeasurement> {
	await expect(bottomBar(page)).toBeVisible();

	await page.evaluate(height => {
		const main = document.querySelector("main");
		if (!main) throw new Error("The page shell has no main element.");
		const probe = document.createElement("div");
		probe.style.height = `${height}px`;
		main.append(probe);
	}, CLEARANCE_PROBE_HEIGHT_PX);
	await flushFrames(page);

	await page.evaluate(() => window.scrollTo(0, document.documentElement.scrollHeight));
	await flushFrames(page);

	return page.evaluate(() => {
		const bar = Array.from(document.querySelectorAll('nav[aria-label="Main"]')).find(
			element => getComputedStyle(element).display !== "none"
		);
		const block = document.querySelector("main")?.lastElementChild;
		if (!bar || !block) throw new Error("The bar or the probe block is missing.");

		const barRect = bar.getBoundingClientRect();
		return {
			gap: barRect.top - block.getBoundingClientRect().bottom,
			navHeight: barRect.height,
			scrollY: window.scrollY
		};
	});
}

async function readTheme(page: Page): Promise<Theme> {
	return page.evaluate(() =>
		document.documentElement.classList.contains("dark") ? "dark" : "light"
	);
}

/** Soft so a single run reports every measurement that drifted, not just the first. */
function expectWithinTolerance(actual: number, expected: number, label: string) {
	expect
		.soft(
			Math.abs(actual - expected),
			`${label}: expected ${expected} ± ${FRACTIONAL_TOLERANCE_PX}px, measured ${actual}`
		)
		.toBeLessThanOrEqual(FRACTIONAL_TOLERANCE_PX);
}

test("keeps today's bar and clearance at zero inset", async ({ page }) => {
	requireMobileProject();
	await page.goto("/dashboard");

	const envInset = await readSafeAreaInsetBottom(page);
	const geometry = await readBarGeometry(page);

	expect(envInset, "the fixture device reports a home indicator").toBe(`${ZERO_PX}px`);
	expectWithinTolerance(geometry.navHeight, NAV_HEIGHT_AT_ZERO_INSET_PX, "nav height");
	expectWithinTolerance(geometry.surfaceHeight, SURFACE_HEIGHT_PX, "surface height");
	expect.soft(geometry.navPaddingBottom).toBe(`${ZERO_PX}px`);
	expect.soft(geometry.stripHeight).toBe(ZERO_PX);
	expect.soft(geometry.mainPaddingBottom).toBe(`${MAIN_PADDING_AT_ZERO_INSET_PX}px`);
});

test("grows the bar and the clearance by exactly one inset", async ({ page }) => {
	requireMobileProject();
	await page.goto("/dashboard");
	await applySafeAreaInset(page, SAFE_AREA_INSET_PX);

	const geometry = await readBarGeometry(page);

	expectWithinTolerance(geometry.surfaceHeight, SURFACE_HEIGHT_PX, "surface height");
	expectWithinTolerance(geometry.navHeight, NAV_HEIGHT_AT_INSET_PX, "nav height");
	expect.soft(geometry.navPaddingBottom).toBe(`${SAFE_AREA_INSET_PX}px`);
	expect.soft(geometry.stripHeight).toBe(STRIP_HEIGHT_AT_INSET_PX);
	expect.soft(geometry.stripAriaHidden).toBe("true");
	expect.soft(geometry.stripPointerEvents).toBe("none");
	expect.soft(geometry.mainPaddingBottom).toBe(`${MAIN_PADDING_AT_INSET_PX}px`);
});

test("keeps the end of a scroll clear of the bar with and without the inset", async ({ page }) => {
	requireMobileProject();
	await page.goto("/dashboard");
	const browserTab = await measureEndOfScroll(page);

	await page.reload();
	await applySafeAreaInset(page, SAFE_AREA_INSET_PX);
	const standalone = await measureEndOfScroll(page);

	expect(browserTab.scrollY, "the page did not scroll").toBeGreaterThan(ZERO_PX);
	expect(standalone.scrollY, "the page did not scroll").toBeGreaterThan(ZERO_PX);
	expect.soft(browserTab.gap).toBeGreaterThanOrEqual(MIN_END_OF_SCROLL_CLEARANCE_PX);
	expect.soft(standalone.gap).toBeGreaterThanOrEqual(MIN_END_OF_SCROLL_CLEARANCE_PX);
	expectWithinTolerance(standalone.gap, browserTab.gap, "end-of-scroll clearance");
	expectWithinTolerance(
		standalone.navHeight - browserTab.navHeight,
		SAFE_AREA_INSET_PX,
		"the bar carries the inset"
	);
});

test("keeps the profile sheet usable in a browser tab", async ({ page }) => {
	requireMobileProject();
	await page.goto("/dashboard");

	const bar = bottomBar(page);
	const sheet = page.getByRole("dialog");

	await bar.getByRole("button", { name: PROFILE_LABEL, exact: true }).click();
	await expect(sheet).toBeVisible();
	await sheet.getByRole("button", { name: PROFILE_LABEL, exact: true }).click();
	await expect(page).toHaveURL(/\/profile$/);
	await expect(sheet).toBeHidden();

	const themeBefore = await readTheme(page);
	const themeAfter: Theme = themeBefore === "dark" ? "light" : "dark";

	await bar.getByRole("button", { name: PROFILE_LABEL, exact: true }).click();
	await expect(sheet).toBeVisible();

	const themeOption = sheet.getByRole("button", { name: THEME_OPTION_LABEL });
	await themeOption.click();

	await expect(sheet).toBeVisible();
	await expect(themeOption).toHaveAccessibleName(THEME_OPTION_NAMES[themeAfter]);
	await expect.poll(() => readTheme(page)).toBe(themeAfter);
});
