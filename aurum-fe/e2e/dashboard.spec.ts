import { expect, test } from "./fixtures/aurum";

test("shows the overview with the live summary", async ({ page }) => {
	await page.goto("/dashboard");

	await expect(page.getByRole("heading", { name: "Overview" })).toBeVisible();
	await expect(page.getByText("€290,450.00").filter({ visible: true }).first()).toBeVisible();
	await expect(page.getByText("Projected net worth")).toBeVisible();
	await expect(page.getByText("Total liabilities")).toBeVisible();
	await expect(page.getByText("Top assets")).toBeVisible();
	await expect(page.getByText("Financial target")).toBeVisible();
});

test("opens the command palette with the keyboard", async ({ page }) => {
	await page.goto("/dashboard");
	await expect(page.getByRole("heading", { name: "Overview" })).toBeVisible();

	await page.keyboard.press("Control+k");

	await expect(page.getByPlaceholder("Type a command or search…")).toBeVisible();
	await page.keyboard.press("Escape");
	await expect(page.getByPlaceholder("Type a command or search…")).toBeHidden();
});

test("shows the financial target after the top assets on mobile", async ({ page }) => {
	await page.setViewportSize({ width: 390, height: 844 });
	await page.goto("/dashboard");

	const topAssets = await page.getByText("Top assets").boundingBox();
	const financialTarget = await page.getByText("Financial target").boundingBox();

	expect(topAssets).not.toBeNull();
	expect(financialTarget).not.toBeNull();
	expect(financialTarget!.y).toBeGreaterThan(topAssets!.y);
});

test("shows the net worth chart before the stat tiles on mobile", async ({ page }) => {
	await page.setViewportSize({ width: 390, height: 844 });
	await page.goto("/dashboard");

	const chart = await page.getByRole("combobox", { name: "Select year" }).boundingBox();
	const firstTile = await page.getByText("Projected net worth").boundingBox();

	expect(chart).not.toBeNull();
	expect(firstTile).not.toBeNull();
	expect(chart!.y).toBeLessThan(firstTile!.y);
});

test("does not allow a past target deadline", async ({ page }) => {
	await page.goto("/dashboard");
	await page.getByRole("button", { name: "Target actions" }).click();
	await page.getByRole("menuitem", { name: "Add target" }).click();
	await page.getByRole("button", { name: "Deadline" }).click();

	await expect(page.getByRole("button", { name: "Go to the Previous Month" })).toHaveAttribute(
		"aria-disabled",
		"true"
	);
});
