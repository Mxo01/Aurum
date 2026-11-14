import { expect, test } from "./fixtures/aurum";

test("keeps the header actions at the same height", async ({ page }) => {
	await page.goto("/assets");

	const categories = page.getByRole("button", { name: "Categories" });
	const addAsset = page.getByRole("button", { name: "Add asset" });
	const [categoriesBox, addAssetBox] = await Promise.all([
		categories.boundingBox(),
		addAsset.boundingBox()
	]);

	expect(categoriesBox?.height).toBe(addAssetBox?.height);
});

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
		.getByRole("button", { name: /Edit Apartment Berlin, currently €430,000\.00/ })
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

test("bounds the asset value date at today", async ({ page }) => {
	await page.goto("/assets");
	await page.getByRole("button", { name: "Add asset" }).click();
	await page.getByRole("button", { name: "Value date" }).click();

	const calendar = page.locator("[data-radix-popper-content-wrapper]");
	const month = calendar.locator("select").first();
	const year = calendar.locator("select").nth(1);

	await expect(page.getByRole("button", { name: "Go to the Next Month" })).toHaveAttribute(
		"aria-disabled",
		"true"
	);

	await month.selectOption({ label: "March" });
	await expect(month).toHaveValue("2");

	await year.selectOption("2001");
	await expect(year).toHaveValue("2001");
});
