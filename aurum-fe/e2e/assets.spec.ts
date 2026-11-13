import { expect, test } from "./fixtures/aurum";

test("lists the grouped assets", async ({ page }) => {
	await page.goto("/assets");

	await expect(page.getByRole("heading", { name: "Assets" })).toBeVisible();
	await expect(page.getByText("Real Estate").filter({ visible: true }).first()).toBeVisible();
	await expect(page.getByText("Apartment Berlin").filter({ visible: true }).first()).toBeVisible();
	await expect(page.getByText("€558,450.00")).toBeVisible();
});

test("adds an asset and shows it in the list", async ({ page }) => {
	await page.goto("/assets");
	await page.getByRole("button", { name: "Add asset" }).click();

	const sheet = page.getByRole("dialog");
	await expect(sheet.getByRole("heading", { name: "New asset" })).toBeVisible();
	await sheet.getByLabel("Name").fill("New Asset");
	await sheet.getByLabel("Category").click();
	await page.getByRole("option", { name: "Investments" }).click();
	await sheet.getByLabel("Initial value").fill("10000");
	await sheet.getByRole("button", { name: "Add asset" }).click();

	await expect(page.getByText("New Asset").filter({ visible: true }).first()).toBeVisible();
});

test("edits a value inline", async ({ page }) => {
	await page.goto("/assets");

	await page
		.getByRole("button", { name: /Edit Apartment Berlin, currently/ })
		.filter({ visible: true })
		.first()
		.click();
	const input = page.getByLabel("Edit Apartment Berlin in EUR");
	await input.fill("440000");
	await input.press("Enter");

	await expect(page.getByText("€440,000.00").filter({ visible: true }).first()).toBeVisible();
});

test("filters the list with the search", async ({ page }) => {
	await page.goto("/assets");
	await page.getByLabel("Search assets").fill("mortgage");

	await expect(page.getByText("Apartment Berlin")).toHaveCount(0);
	await expect(page.getByText("Mortgage").filter({ visible: true }).first()).toBeVisible();
});
