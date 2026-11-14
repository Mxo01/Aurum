import { expect, test } from "./fixtures/aurum";

test("shows the product and the primary call to action", async ({ page }) => {
	await page.goto("/");

	await expect(page.getByRole("heading", { name: /Your net worth/ })).toBeVisible();
	await expect(page.getByText("finally legible.")).toBeVisible();
	await expect(page.getByRole("link", { name: "Open dashboard" })).toBeVisible();
	await expect(page.getByText("31 currencies").first()).toBeVisible();
});

test("toggles the theme", async ({ page }) => {
	await page.goto("/");
	const html = page.locator("html");
	const before = await html.getAttribute("class");

	await page.getByRole("button", { name: /Switch to (light|dark) theme/ }).click();

	await expect(html).not.toHaveAttribute("class", before ?? "");
});

test("opens the privacy policy", async ({ page }) => {
	await page.goto("/privacy");

	await expect(page.getByRole("heading", { name: "Privacy Policy" })).toBeVisible();
	await expect(page.getByText("Cookies and local storage")).toBeVisible();
});
