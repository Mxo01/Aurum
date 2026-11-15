import type { Page } from "@playwright/test";
import { expect, test } from "./fixtures/aurum";

const ASSETS_PAGE = "/assets";
const CATEGORIES_ENDPOINT = "http://api.test/categories";
const CATEGORIES_BUTTON = "Categories";
const CONFIRM_LABEL = "Delete";
const JSON_CONTENT_TYPE = "application/json";
// The stub answers a cross-origin request, so the browser only accepts it with this header.
const CORS_HEADERS = { "access-control-allow-origin": "*" };

// The fixture's list never carries a custom category, and only a custom one offers a delete control.
const SEEDED_CATEGORY = {
	id: "c9",
	name: "Stocks",
	type: "ASSET",
	icon: "pi-chart-line",
	isDefault: false
};

const DEFAULT_CATEGORIES = [
	{ id: "c1", name: "Real Estate", type: "ASSET", icon: "pi-home", isDefault: true },
	{ id: "c2", name: "Investments", type: "ASSET", icon: "pi-chart-line", isDefault: true },
	{ id: "c3", name: "Cash", type: "ASSET", icon: "pi-wallet", isDefault: true },
	{ id: "c6", name: "Mortgage", type: "LIABILITY", icon: "pi-building", isDefault: true }
];

// `.opencode/tasks/aurum-89/t14-e2e-sheets.md` — 409, a category that still holds assets.
const CATEGORY_BODY = `{"status":409,"error":"Conflict","errors":[{"message":"This category still has assets, move or delete them first"}]}`;

// The toast copy a rejected delete used to show before the hook read the envelope.
const LEGACY_DELETE_TOAST = "Could not delete the category";
const DELETE_SUCCESS_TOAST = "Category deleted";
const DELETE_CATEGORY_CONTROL = `Delete ${SEEDED_CATEGORY.name}`;

interface Envelope {
	status: number;
	message: string;
}

interface RejectionStub {
	requests: number;
}

function isRecord(value: unknown): value is Record<string, unknown> {
	return typeof value === "object" && value !== null;
}

// The captured bytes are the assertion's source of truth: a payload that stopped carrying a status
// and a sentence must break this spec instead of quietly weakening it.
function envelopeOf(body: string): Envelope {
	const parsed: unknown = JSON.parse(body);
	if (!isRecord(parsed) || typeof parsed.status !== "number") {
		throw new Error(`Not a rejection envelope: ${body}`);
	}

	const detail: unknown = Array.isArray(parsed.errors) ? parsed.errors[0] : undefined;
	if (!isRecord(detail) || typeof detail.message !== "string") {
		throw new Error(`Rejection envelope without an errors[].message: ${body}`);
	}

	return { status: parsed.status, message: detail.message };
}

// Parsed once at load: a captured body that stopped carrying a status and a sentence fails the file
// before any test runs, and the test reads both the status and the message from here.
const CATEGORY_REJECTION = envelopeOf(CATEGORY_BODY);

// Seeds the custom category with one read override and answers the delete with the captured
// envelope; every other request stays with the fixture's catch-all.
async function stubCategoryRejection(page: Page, body: string): Promise<RejectionStub> {
	const stub: RejectionStub = { requests: 0 };

	await page.route(CATEGORIES_ENDPOINT, route => {
		if (route.request().method() !== "GET") return route.fallback();

		return route.fulfill({
			status: 200,
			contentType: JSON_CONTENT_TYPE,
			headers: CORS_HEADERS,
			body: JSON.stringify([...DEFAULT_CATEGORIES, SEEDED_CATEGORY])
		});
	});

	await page.route(`${CATEGORIES_ENDPOINT}/${SEEDED_CATEGORY.id}`, async route => {
		if (route.request().method() !== "DELETE") return route.fallback();

		stub.requests += 1;
		await route.fulfill({
			status: CATEGORY_REJECTION.status,
			contentType: JSON_CONTENT_TYPE,
			headers: CORS_HEADERS,
			body
		});
	});

	return stub;
}

test("shows the rejection sentence and never the delete fallback", async ({ page }) => {
	const stub = await stubCategoryRejection(page, CATEGORY_BODY);

	await page.goto(ASSETS_PAGE);
	await page.getByRole("button", { name: CATEGORIES_BUTTON }).click();

	const sheet = page.getByRole("dialog");
	const row = sheet.getByText(SEEDED_CATEGORY.name, { exact: true });
	await expect(row).toBeVisible();

	await sheet.getByRole("button", { name: DELETE_CATEGORY_CONTROL }).click();

	// The confirm step names the row, so the delete that follows is unambiguously the seeded one.
	const confirm = page.getByRole("alertdialog");
	await expect(
		confirm.getByRole("heading", { name: `Delete “${SEEDED_CATEGORY.name}”?` })
	).toBeVisible();
	await confirm.getByRole("button", { name: CONFIRM_LABEL, exact: true }).click();

	// The attempt must reach the server: a spec that passed without a request would prove nothing.
	await expect.poll(() => stub.requests).toBe(1);
	await expect(page.getByText(CATEGORY_REJECTION.message, { exact: true })).toBeVisible();
	await expect(page.getByText(LEGACY_DELETE_TOAST, { exact: true })).toHaveCount(0);
	await expect(page.getByText(DELETE_SUCCESS_TOAST, { exact: true })).toHaveCount(0);
	// A rejection is display state: the confirmation closes and the category survives on screen.
	await expect(confirm).toHaveCount(0);
	await expect(row).toBeVisible();
	await expect(sheet.getByRole("button", { name: DELETE_CATEGORY_CONTROL })).toBeEnabled();
});
