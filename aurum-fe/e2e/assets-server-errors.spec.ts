import type { Locator, Page } from "@playwright/test";
import { expect, test } from "./fixtures/aurum";

const ASSETS_PAGE = "/assets";
const ASSETS_ENDPOINT = "http://api.test/assets";
const SUBMIT_LABEL = "Add asset";
const JSON_CONTENT_TYPE = "application/json";
// The stub answers a cross-origin request, so the browser only accepts it with this header.
const CORS_HEADERS = { "access-control-allow-origin": "*" };

// `.opencode/tasks/aurum-75/envelope/payment-amount.json` — 400, blames the payment amount input.
const PAYMENT_AMOUNT_BODY = `{"status":400,"error":"Bad Request","errors":[{"field":"paymentAmount","message":"Payment amount cannot be negative"}]}`;

// `.opencode/tasks/aurum-75/envelope/exchange-rate.json` — 503, names no field, so no input can own it.
const EXCHANGE_RATE_BODY = `{"status":503,"error":"Service Unavailable","errors":[{"message":"Exchange rates are temporarily unavailable, please retry later"}]}`;

// `.opencode/tasks/aurum-75/envelope/reference-date.json` — 400, blames a field the first attempt did not.
const REFERENCE_DATE_BODY = `{"status":400,"error":"Bad Request","errors":[{"field":"referenceDate","message":"Value date cannot be in the future"}]}`;

// The toast copy the failed create used to show before the sheet rendered the server's own sentence.
const LEGACY_CREATE_TOAST = "Could not create the asset";
const CREATE_SUCCESS_TOAST = "Asset created";

const NEW_LIABILITY = {
	name: "Holiday home",
	category: "Mortgage",
	liabilityType: "Automatic",
	paymentFrequency: "Monthly",
	paymentAmount: "500",
	correctedPaymentAmount: "600",
	initialValue: "12000"
};

const PAYMENT_AMOUNT_FIELD = "Payment amount";
const INITIAL_VALUE_FIELD = "Initial value";
const VALUE_DATE_FIELD = "Value date";

interface Envelope {
	status: number;
	message: string;
}

interface RejectionStub {
	// Re-pointing this between submits is how a test answers a second attempt differently: another
	// `page.route` on the same URL would leave the first handler in place.
	body: string;
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

function stubOf(body: string): RejectionStub {
	return { body, requests: 0 };
}

// Parsed once at load: a captured body that stopped carrying a status and a sentence fails the file
// before any test runs, and every test reads its message from here instead of re-parsing it.
const PAYMENT_AMOUNT = envelopeOf(PAYMENT_AMOUNT_BODY);
const EXCHANGE_RATE = envelopeOf(EXCHANGE_RATE_BODY);
const REFERENCE_DATE = envelopeOf(REFERENCE_DATE_BODY);

// Answers only the create call with the captured envelope, under the status the envelope declares;
// the CORS preflight and every other request stay with the fixture's catch-all.
async function stubCreateRejection(page: Page, body: string): Promise<RejectionStub> {
	const stub = stubOf(body);

	await page.route(ASSETS_ENDPOINT, async route => {
		if (route.request().method() !== "POST") return route.fallback();

		stub.requests += 1;
		await route.fulfill({
			status: envelopeOf(stub.body).status,
			contentType: JSON_CONTENT_TYPE,
			headers: CORS_HEADERS,
			body: stub.body
		});
	});

	return stub;
}

// The block a message must land in: the control, its label and its error paragraph share one parent.
function fieldContainer(sheet: Locator, label: string): Locator {
	return sheet.getByLabel(label, { exact: true }).locator("..");
}

// Same interaction shape as `addAutomaticLiability` in `assets.spec.ts`, stopping before the submit so
// each test registers its own route first and clicks the button itself.
async function openNewLiabilityForm(page: Page): Promise<Locator> {
	await page.goto(ASSETS_PAGE);
	await page.getByRole("button", { name: SUBMIT_LABEL }).click();

	const sheet = page.getByRole("dialog");
	await sheet.getByLabel("Name").fill(NEW_LIABILITY.name);
	await sheet.getByLabel("Category").click();
	await page.getByRole("option", { name: NEW_LIABILITY.category }).click();
	await sheet.getByLabel("Liability type").click();
	await page.getByRole("option", { name: NEW_LIABILITY.liabilityType }).click();
	await sheet.getByLabel("Payment frequency").click();
	await page.getByRole("option", { name: NEW_LIABILITY.paymentFrequency }).click();
	await sheet.getByLabel(PAYMENT_AMOUNT_FIELD).fill(NEW_LIABILITY.paymentAmount);
	await sheet.getByLabel(INITIAL_VALUE_FIELD).fill(NEW_LIABILITY.initialValue);

	return sheet;
}

test("shows a field-attributed rejection under the input it names", async ({ page }) => {
	const stub = await stubCreateRejection(page, PAYMENT_AMOUNT_BODY);

	const sheet = await openNewLiabilityForm(page);
	const submit = sheet.getByRole("button", { name: SUBMIT_LABEL });
	const paymentAmount = fieldContainer(sheet, PAYMENT_AMOUNT_FIELD);
	const initialValue = fieldContainer(sheet, INITIAL_VALUE_FIELD);

	await submit.click();

	await expect(paymentAmount.getByText(PAYMENT_AMOUNT.message, { exact: true })).toBeVisible();
	// Both blocks must be the field containers and not an ancestor of the whole form, otherwise the
	// negative below could pass against a wrapper that legitimately holds the message.
	await expect(paymentAmount).toContainText(PAYMENT_AMOUNT_FIELD);
	await expect(initialValue).toContainText(INITIAL_VALUE_FIELD);
	await expect(initialValue.getByText(PAYMENT_AMOUNT.message, { exact: true })).toHaveCount(0);
	// A message naming a rendered field belongs to that field, not to the form-wide alert.
	await expect(sheet.getByRole("alert")).toHaveCount(0);
	// A rejection is display state, so the button never goes dead.
	await expect(submit).toBeEnabled();
	await expect.poll(() => stub.requests).toBe(1);
});

test("shows a fieldless rejection once, in the alert", async ({ page }) => {
	const stub = await stubCreateRejection(page, EXCHANGE_RATE_BODY);

	const sheet = await openNewLiabilityForm(page);
	const submit = sheet.getByRole("button", { name: SUBMIT_LABEL });
	const alert = sheet.getByRole("alert");

	await submit.click();

	await expect(alert).toHaveCount(1);
	await expect(alert).toHaveText(EXCHANGE_RATE.message);
	await expect(fieldContainer(sheet, PAYMENT_AMOUNT_FIELD)).toContainText(PAYMENT_AMOUNT_FIELD);
	await expect(fieldContainer(sheet, INITIAL_VALUE_FIELD)).toContainText(INITIAL_VALUE_FIELD);
	await expect(
		fieldContainer(sheet, PAYMENT_AMOUNT_FIELD).getByText(EXCHANGE_RATE.message, { exact: true })
	).toHaveCount(0);
	await expect(
		fieldContainer(sheet, INITIAL_VALUE_FIELD).getByText(EXCHANGE_RATE.message, { exact: true })
	).toHaveCount(0);
	await expect(submit).toBeEnabled();

	// A second attempt must reach the server again, with no edit in between to unlock it.
	await submit.click();
	await expect.poll(() => stub.requests).toBe(2);
	await expect(alert).toHaveText(EXCHANGE_RATE.message);
	await expect(submit).toBeEnabled();
});

test("shows the rejection sentence once on the page", async ({ page }) => {
	await stubCreateRejection(page, EXCHANGE_RATE_BODY);

	const sheet = await openNewLiabilityForm(page);

	await sheet.getByRole("button", { name: SUBMIT_LABEL }).click();

	await expect(sheet.getByRole("alert")).toHaveText(EXCHANGE_RATE.message);
	// The sheet is the only place the sentence may live: a failure toast repeating it would double it.
	await expect(page.getByText(EXCHANGE_RATE.message, { exact: true })).toHaveCount(1);
	await expect(page.getByText(LEGACY_CREATE_TOAST, { exact: true })).toHaveCount(0);
	await expect(page.getByText(CREATE_SUCCESS_TOAST, { exact: true })).toHaveCount(0);
});

test("retires a rejection on edit and re-attributes the next one", async ({ page }) => {
	const stub = await stubCreateRejection(page, PAYMENT_AMOUNT_BODY);

	const sheet = await openNewLiabilityForm(page);
	const submit = sheet.getByRole("button", { name: SUBMIT_LABEL });
	const paymentAmount = fieldContainer(sheet, PAYMENT_AMOUNT_FIELD);
	const valueDate = fieldContainer(sheet, VALUE_DATE_FIELD);

	await submit.click();

	await expect(paymentAmount.getByText(PAYMENT_AMOUNT.message, { exact: true })).toBeVisible();

	// An edit retires the rejection of the attempt that produced it.
	await sheet.getByLabel(PAYMENT_AMOUNT_FIELD).fill(NEW_LIABILITY.correctedPaymentAmount);
	await expect(page.getByText(PAYMENT_AMOUNT.message, { exact: true })).toHaveCount(0);
	await expect(sheet.getByRole("alert")).toHaveCount(0);

	// Same handler, new captured payload: only the outcome of the latest attempt may be on screen.
	stub.body = REFERENCE_DATE_BODY;
	await submit.click();

	await expect(valueDate.getByText(REFERENCE_DATE.message, { exact: true })).toBeVisible();
	await expect(valueDate).toContainText(VALUE_DATE_FIELD);
	await expect(paymentAmount.getByText(REFERENCE_DATE.message, { exact: true })).toHaveCount(0);
	await expect(page.getByText(REFERENCE_DATE.message, { exact: true })).toHaveCount(1);
	await expect(page.getByText(PAYMENT_AMOUNT.message, { exact: true })).toHaveCount(0);
	await expect(sheet.getByRole("alert")).toHaveCount(0);
	await expect(submit).toBeEnabled();
	await expect.poll(() => stub.requests).toBe(2);
});
