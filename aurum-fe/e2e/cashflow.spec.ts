import { expect, test } from "./fixtures/aurum";

test("shows the year summary and the table", async ({ page }) => {
	await page.goto("/cashflow");

	await expect(page.getByRole("heading", { name: "Cash flow" })).toBeVisible();
	await expect(page.getByText("€66,600").filter({ visible: true }).first()).toBeVisible();
	await expect(page.getByText("€51,300").filter({ visible: true }).first()).toBeVisible();
});

test("shows the year-over-year columns", async ({ page }, testInfo) => {
	test.skip(testInfo.project.name === "mobile", "YoY columns are desktop-only");

	await page.goto("/cashflow");

	await expect(page.getByText("2025 earned")).toBeVisible();
	await expect(page.getByText("2025 spent")).toBeVisible();
});

test("edits a month and updates the total", async ({ page }) => {
	await page.goto("/cashflow");

	await page
		.getByRole("button", { name: /Edit JAN earned/ })
		.filter({ visible: true })
		.first()
		.click();
	const input = page.getByLabel("Edit JAN earned in EUR");
	await input.fill("6000");
	await input.press("Enter");

	await expect(page.getByText("€67,600").filter({ visible: true }).first()).toBeVisible();
});

test("switches to the previous year", async ({ page, api }) => {
	await page.goto("/cashflow");

	await page.getByRole("button", { name: "Previous year" }).click();

	await expect(page).toHaveURL(/year=2025/);
	await expect.poll(() => api.lastCashflowYear).toBe(2025);
});
