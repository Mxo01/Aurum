import { expect, test } from "./fixtures/aurum";

test("does not overflow horizontally on mobile", async ({ page }) => {
	await page.setViewportSize({ width: 390, height: 844 });

	for (const path of ["/dashboard", "/assets", "/cashflow", "/profile"]) {
		await page.goto(path);
		const width = await page.evaluate(() => document.documentElement.scrollWidth);
		expect(width, `${path} overflows`).toBeLessThanOrEqual(391);
	}
});

test("shows the bottom navigation on mobile", async ({ page }) => {
	await page.setViewportSize({ width: 390, height: 844 });
	await page.goto("/dashboard");

	await expect(page.getByRole("navigation", { name: "Main" }).last()).toBeVisible();
	await expect(page.getByRole("link", { name: "Cash flow" }).last()).toBeVisible();
});
