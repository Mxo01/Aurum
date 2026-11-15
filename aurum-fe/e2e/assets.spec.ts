import type { Page } from "@playwright/test";
import { expect, test } from "./fixtures/aurum";

const BASE_CURRENCY = "EUR";
const GROUP_TOTAL_TITLE = /^Group total in EUR, your base currency/;
const SCHEDULE_DESCRIPTION = "Follows the automatic payment schedule";
const SCHEDULE_TITLE = /^Value follows the automatic payment schedule/;
const GROUP_NAMES = ["Investments", "Mortgage", "Real Estate"];

const FIXED_NOW = new Date("2026-03-15T12:00:00");
const TODAY = "2026-03-15";
const FUTURE_DAY = "2026-04-15";
const DATE_NOT_FUTURE = "The date cannot be in the future";

const AUTOMATIC_LIABILITY = {
	name: "Car Loan",
	category: "Mortgage",
	liabilityType: "Automatic",
	paymentFrequency: "Monthly",
	paymentAmount: "500",
	initialValue: "12000",
	displayValue: "€12,000.00",
	groupTotal: "€280,000.00"
};

const MANUAL_LIABILITY = { name: "Mortgage", displayValue: "€268,000.00" };

async function addAutomaticLiability(page: Page) {
	await page.getByRole("button", { name: "Add asset" }).click();

	const sheet = page.getByRole("dialog");
	await sheet.getByLabel("Name").fill(AUTOMATIC_LIABILITY.name);
	await sheet.getByLabel("Category").click();
	await page.getByRole("option", { name: AUTOMATIC_LIABILITY.category }).click();
	await sheet.getByLabel("Liability type").click();
	await page.getByRole("option", { name: AUTOMATIC_LIABILITY.liabilityType }).click();
	await sheet.getByLabel("Payment frequency").click();
	await page.getByRole("option", { name: AUTOMATIC_LIABILITY.paymentFrequency }).click();
	await sheet.getByLabel("Payment amount").fill(AUTOMATIC_LIABILITY.paymentAmount);
	await sheet.getByLabel("Initial value").fill(AUTOMATIC_LIABILITY.initialValue);
	await sheet.getByRole("button", { name: "Add asset" }).click();

	await expect(
		page.getByText(AUTOMATIC_LIABILITY.name).filter({ visible: true }).first()
	).toBeVisible();
}

function groupHeader(page: Page, name: string) {
	return page.locator("button[aria-expanded]").filter({ hasText: name }).filter({ visible: true });
}

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

test("rejects a future value date and accepts today again", async ({ page }) => {
	await page.clock.install({ time: FIXED_NOW });
	await page.goto("/assets");
	await page.getByRole("button", { name: "Add asset" }).click();

	const valueDate = page.getByRole("button", { name: "Value date" });
	await valueDate.click();

	const nextMonth = page.getByRole("button", { name: "Go to the Next Month" });
	await expect(nextMonth).not.toHaveAttribute("aria-disabled", "true");
	await nextMonth.click();
	await page.locator(`[data-day="${FUTURE_DAY}"] button`).click();

	const dateError = page.getByText(DATE_NOT_FUTURE);
	await expect(dateError).toBeVisible();

	await valueDate.click();
	await page.getByRole("button", { name: "Go to the Previous Month" }).click();
	await page.locator(`[data-day="${TODAY}"] button`).click();

	await expect(dateError).toHaveCount(0);

	await valueDate.click();
	const calendar = page.locator("[data-radix-popper-content-wrapper]");
	await expect(calendar).toBeVisible();
	const month = calendar.locator("select").first();
	const year = calendar.locator("select").nth(1);

	await month.selectOption({ label: "March" });
	await expect(month).toHaveValue("2");

	await year.selectOption("2001");
	await expect(year).toHaveValue("2001");
});

test("keeps the automatic liability value read-only", async ({ page }) => {
	await page.goto("/assets");
	await addAutomaticLiability(page);

	const editControl = page.getByRole("button", {
		name: new RegExp(
			`Edit ${AUTOMATIC_LIABILITY.name}, currently ${AUTOMATIC_LIABILITY.displayValue}`
		)
	});
	await expect(editControl).toHaveCount(0);

	await expect(
		page
			.getByRole("button", {
				name: new RegExp(
					`Edit ${MANUAL_LIABILITY.name}, currently ${MANUAL_LIABILITY.displayValue}`
				)
			})
			.filter({ visible: true })
	).toHaveCount(1);

	await expect(
		page.getByText(AUTOMATIC_LIABILITY.displayValue, { exact: true }).filter({ visible: true })
	).toHaveCount(1);
	await expect(page.getByText(SCHEDULE_DESCRIPTION).filter({ visible: true })).toHaveCount(1);
	await expect(page.getByTitle(SCHEDULE_TITLE).filter({ visible: true })).toHaveCount(1);
});

test("shows the base currency on the group totals", async ({ page }) => {
	await page.goto("/assets");
	await addAutomaticLiability(page);

	const mortgage = groupHeader(page, AUTOMATIC_LIABILITY.category);
	await expect(mortgage).toContainText(AUTOMATIC_LIABILITY.groupTotal);
	await expect(mortgage.getByText(BASE_CURRENCY, { exact: true })).toBeVisible();

	for (const name of GROUP_NAMES) {
		const header = groupHeader(page, name);
		await expect(header).toHaveCount(1);
		await expect(header.getByText(BASE_CURRENCY, { exact: true })).toBeVisible();
		await expect(header.getByTitle(GROUP_TOTAL_TITLE)).toBeVisible();
	}
});
