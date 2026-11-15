import type { Page, Request } from "@playwright/test";
import { expect, test } from "./fixtures/aurum";

const ASSETS_PAGE = "/assets";
const DASHBOARD_PAGE = "/dashboard";
const ASSETS_ENDPOINT = "/assets";
const SNAPSHOTS_ENDPOINT = "/snapshots";
const TARGETS_ENDPOINT = "/targets";

const AMOUNT_REQUIRED = "Enter an amount";
const NUMBER_INVALID = "Enter a number";
const NAME_REQUIRED = "Name is required";
// The zod default the client used to echo before the shared vocabulary owned every sentence.
const ZOD_DEFAULT = /Invalid input/;

const COMMA_DECIMAL = "1234,56";
const COMMA_DECIMAL_VALUE = 1234.56;
const COMMA_ASSET_NAME = "Comma Asset";
const COMMA_ASSET_DISPLAY = "€1,234.56";
// A value with more decimals than a step="0.01" number input would let the browser submit.
const STEP_VIOLATING_VALUE = "54654.74765754";
const STEP_ASSET_NAME = "Precise Asset";

const HISTORY_ASSET = "Apartment Berlin";
const HISTORY_TITLE = `History · ${HISTORY_ASSET}`;

const TARGET_NAME = "Retirement";
const TARGET_AMOUNT = "1500,50";
const TARGET_AMOUNT_VALUE = 1500.5;

function isRecord(value: unknown): value is Record<string, unknown> {
	return typeof value === "object" && value !== null;
}

// Reads one numeric field out of the captured body: a payload that stopped carrying the parsed
// amount must fail here instead of passing on an unrelated assertion.
function numericFieldOf(request: Request, field: string): number {
	const body = request.postData();
	const parsed: unknown = body === null ? null : JSON.parse(body);
	if (!isRecord(parsed)) {
		throw new Error(`Not a JSON object: ${String(body)}`);
	}

	const value = parsed[field];
	if (typeof value !== "number") {
		throw new Error(`Body without a numeric ${field}: ${String(body)}`);
	}

	return value;
}

function waitForPost(page: Page, endpoint: string) {
	return page.waitForRequest(
		request => request.method() === "POST" && new URL(request.url()).pathname === endpoint
	);
}

function assetSheet(page: Page) {
	return page.getByRole("dialog", { name: "New asset" });
}

async function openHistory(page: Page) {
	await page.goto(ASSETS_PAGE);
	await page
		.getByRole("button", { name: `Actions for ${HISTORY_ASSET}` })
		.filter({ visible: true })
		.first()
		.click();
	await page.getByRole("menuitem", { name: "History" }).click();

	const sheet = page.getByRole("dialog", { name: HISTORY_TITLE });
	await expect(sheet.getByRole("heading", { name: HISTORY_TITLE })).toBeVisible();
	return sheet;
}

test("asks for an amount in the asset sheet and never lets the browser validate", async ({
	page
}) => {
	await page.goto(ASSETS_PAGE);
	await page.getByRole("button", { name: "Add asset" }).click();

	const sheet = assetSheet(page);
	const amount = sheet.getByLabel("Initial value");

	await expect(amount).toHaveAttribute("type", "text");
	await expect(amount).toHaveAttribute("inputmode", "decimal");
	expect(await amount.getAttribute("min")).toBeNull();
	expect(await amount.getAttribute("step")).toBeNull();
	await expect(sheet.locator("form")).toHaveAttribute("novalidate", "");

	await amount.fill("");
	await expect(sheet.getByText(AMOUNT_REQUIRED, { exact: true })).toBeVisible();

	await amount.fill("abc");
	await expect(sheet.getByText(NUMBER_INVALID, { exact: true })).toBeVisible();
	await expect(page.getByText(ZOD_DEFAULT)).toHaveCount(0);
});

test("saves a comma decimal from the asset sheet", async ({ page }) => {
	await page.goto(ASSETS_PAGE);
	await page.getByRole("button", { name: "Add asset" }).click();

	const sheet = assetSheet(page);
	await sheet.getByLabel("Name").fill(COMMA_ASSET_NAME);
	await sheet.getByLabel("Category").click();
	await page.getByRole("option", { name: "Investments" }).click();
	await sheet.getByLabel("Initial value").fill(COMMA_DECIMAL);

	const posted = waitForPost(page, ASSETS_ENDPOINT);
	await sheet.getByRole("button", { name: "Add asset" }).click();

	expect(numericFieldOf(await posted, "initialValue")).toBe(COMMA_DECIMAL_VALUE);
	await expect(page.getByText(COMMA_ASSET_NAME).filter({ visible: true }).first()).toBeVisible();
	await expect(
		page.getByText(COMMA_ASSET_DISPLAY, { exact: true }).filter({ visible: true })
	).toHaveCount(1);
});

test("submits an amount the old number input would have blocked", async ({ page }) => {
	await page.goto(ASSETS_PAGE);
	await page.getByRole("button", { name: "Add asset" }).click();

	const sheet = assetSheet(page);
	await sheet.getByLabel("Name").fill(STEP_ASSET_NAME);
	await sheet.getByLabel("Category").click();
	await page.getByRole("option", { name: "Cash" }).click();
	await sheet.getByLabel("Initial value").fill(STEP_VIOLATING_VALUE);

	const posted = waitForPost(page, ASSETS_ENDPOINT);
	await sheet.getByRole("button", { name: "Add asset" }).click();

	expect(numericFieldOf(await posted, "initialValue")).toBe(Number(STEP_VIOLATING_VALUE));
	await expect(page.getByText(STEP_ASSET_NAME).filter({ visible: true }).first()).toBeVisible();
	await expect(page.getByText(ZOD_DEFAULT)).toHaveCount(0);
});

test("asks for an amount and saves a comma decimal in the snapshot history", async ({ page }) => {
	const sheet = await openHistory(page);
	const value = sheet.getByLabel("Snapshot value");
	const add = sheet.getByRole("button", { name: "Add", exact: true });

	await add.click();
	await expect(sheet.getByText(AMOUNT_REQUIRED, { exact: true })).toBeVisible();

	await value.fill("abc");
	await add.click();
	await expect(sheet.getByText(NUMBER_INVALID, { exact: true })).toBeVisible();

	await value.fill(COMMA_DECIMAL);
	const posted = waitForPost(page, SNAPSHOTS_ENDPOINT);
	await add.click();

	expect(numericFieldOf(await posted, "amountOriginalCurrency")).toBe(COMMA_DECIMAL_VALUE);
	await expect(page.getByText(ZOD_DEFAULT)).toHaveCount(0);
});

test("asks for an amount and saves a comma decimal in the target sheet", async ({ page }) => {
	await page.goto(DASHBOARD_PAGE);
	await page.getByRole("button", { name: "Target actions" }).click();
	await page.getByRole("menuitem", { name: "Add target" }).click();

	const sheet = page.getByRole("dialog", { name: "New target" });
	const amount = sheet.getByLabel("Target amount");

	await amount.fill("");
	await expect(sheet.getByText(AMOUNT_REQUIRED, { exact: true })).toBeVisible();

	await amount.fill("abc");
	await expect(sheet.getByText(NUMBER_INVALID, { exact: true })).toBeVisible();

	await sheet.getByLabel("Name").fill(TARGET_NAME);
	await amount.fill(TARGET_AMOUNT);
	const posted = waitForPost(page, TARGETS_ENDPOINT);
	await sheet.getByRole("button", { name: "Create target" }).click();

	expect(numericFieldOf(await posted, "targetAmount")).toBe(TARGET_AMOUNT_VALUE);
	await expect(page.getByText(ZOD_DEFAULT)).toHaveCount(0);
});

test("asks for a category name and never lets the browser validate", async ({ page }) => {
	await page.goto(ASSETS_PAGE);
	await page.getByRole("button", { name: "Categories" }).click();

	const categoriesSheet = page.getByRole("dialog", { name: "Categories" });
	await categoriesSheet.getByRole("button", { name: "Add category" }).click();

	const form = page.getByRole("dialog", { name: "New category" });
	await expect(form.locator("form")).toHaveAttribute("novalidate", "");

	await form.getByRole("button", { name: "Create category" }).click();
	await expect(form.getByText(NAME_REQUIRED, { exact: true })).toBeVisible();
	await expect(page.getByText(ZOD_DEFAULT)).toHaveCount(0);
});
