import type { Locator, Page } from "@playwright/test";
import { expect, test } from "./fixtures/aurum";

const ASSETS_PAGE = "/assets";
const SNAPSHOTS_ENDPOINT = "http://api.test/snapshots";
const HISTORY_ASSET = "Apartment Berlin";
const HISTORY_SHEET_TITLE = `History · ${HISTORY_ASSET}`;
const SNAPSHOT_VALUE_FIELD = "Snapshot value";
const SUBMIT_LABEL = "Add";
const JSON_CONTENT_TYPE = "application/json";
// The stub answers a cross-origin request, so the browser only accepts it with this header.
const CORS_HEADERS = { "access-control-allow-origin": "*" };

// `.opencode/tasks/aurum-89/t14-e2e-sheets.md` — 400, blames the amount input.
const AMOUNT_BODY = `{"status":400,"error":"Bad Request","errors":[{"field":"amountOriginalCurrency","message":"Amount cannot be negative"}]}`;

// The toast copy a rejected add used to show before the hook read the envelope.
const LEGACY_SAVE_TOAST = "Could not save the snapshot";
const SAVE_SUCCESS_TOAST = "Snapshot saved";

// The sheet refuses to send a negative amount (`snapshots-sheet.tsx` returns before mutating when the
// parsed value is below zero), so the rejection is provoked by the server answering a value the sheet
// does release, and the payload below is the one the screen must render.
const SNAPSHOT_VALUE = "450000";

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
const AMOUNT_REJECTION = envelopeOf(AMOUNT_BODY);

// Answers only the create call with the captured envelope, under the status the envelope declares;
// the read of the same path and every other request stay with the fixture's catch-all.
async function stubCreateRejection(page: Page, body: string): Promise<RejectionStub> {
	const stub: RejectionStub = { requests: 0 };

	await page.route(SNAPSHOTS_ENDPOINT, async route => {
		if (route.request().method() !== "POST") return route.fallback();

		stub.requests += 1;
		await route.fulfill({
			status: AMOUNT_REJECTION.status,
			contentType: JSON_CONTENT_TYPE,
			headers: CORS_HEADERS,
			body
		});
	});

	return stub;
}

// Same interaction shape as the assets spec: row menu, then the history sheet, stopping before the
// submit so the test registers its own route first.
async function openHistory(page: Page): Promise<Locator> {
	await page.goto(ASSETS_PAGE);
	await page
		.getByRole("button", { name: `Actions for ${HISTORY_ASSET}` })
		.filter({ visible: true })
		.first()
		.click();
	await page.getByRole("menuitem", { name: "History" }).click();

	const sheet = page.getByRole("dialog");
	await expect(sheet.getByRole("heading", { name: HISTORY_SHEET_TITLE })).toBeVisible();

	return sheet;
}

test("shows the rejection sentence and never the save fallback", async ({ page }) => {
	const stub = await stubCreateRejection(page, AMOUNT_BODY);

	const sheet = await openHistory(page);
	const value = sheet.getByLabel(SNAPSHOT_VALUE_FIELD);
	const submit = sheet.getByRole("button", { name: SUBMIT_LABEL, exact: true });

	await value.fill(SNAPSHOT_VALUE);
	await submit.click();

	// The attempt must reach the server: a spec that passed without a request would prove nothing.
	await expect.poll(() => stub.requests).toBe(1);
	await expect(page.getByText(AMOUNT_REJECTION.message, { exact: true })).toBeVisible();
	await expect(page.getByText(LEGACY_SAVE_TOAST, { exact: true })).toHaveCount(0);
	await expect(page.getByText(SAVE_SUCCESS_TOAST, { exact: true })).toHaveCount(0);
	// A rejection is display state: the sheet stays open, keeps the typed value and stays submittable.
	await expect(sheet.getByRole("heading", { name: HISTORY_SHEET_TITLE })).toBeVisible();
	await expect(value).toHaveValue(SNAPSHOT_VALUE);
	await expect(submit).toBeEnabled();
});
