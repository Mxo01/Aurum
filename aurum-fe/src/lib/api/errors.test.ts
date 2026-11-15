import { describe, expect, it } from "vitest";
import { ApiError } from "./client";
import { getApiErrorDetails, getApiErrorMessage } from "./errors";
import type { ApiErrorDetail } from "./types";

const VALIDATION_MESSAGE = "Name is required";
const SECONDARY_MESSAGE = "Category is required";
const FALLBACK_MESSAGE = "Could not save the new value";
const NAME_FIELD = "name";
const CATEGORY_FIELD = "categoryId";
const VALIDATION_STATUS = 400;

function buildDetail(overrides: Partial<ApiErrorDetail> = {}): ApiErrorDetail {
	return { field: NAME_FIELD, message: VALIDATION_MESSAGE, ...overrides };
}

function buildEnvelope(errors: unknown): Record<string, unknown> {
	return { status: VALIDATION_STATUS, error: "Bad Request", errors };
}

function buildApiError(body: unknown): ApiError {
	return new ApiError(VALIDATION_STATUS, body);
}

interface DetailCase {
	readonly label: string;
	readonly error: unknown;
	readonly expectedDetails: ApiErrorDetail[];
}

const DETAIL_CASES: DetailCase[] = [
	{
		label: "keep a detail that carries a field",
		error: buildApiError(buildEnvelope([buildDetail()])),
		expectedDetails: [buildDetail()]
	},
	{
		label: "keep a detail without a field",
		error: buildApiError(buildEnvelope([{ message: VALIDATION_MESSAGE }])),
		expectedDetails: [{ message: VALIDATION_MESSAGE }]
	},
	{
		label: "keep only the renderable details of a mixed list",
		error: buildApiError(
			buildEnvelope([null, buildDetail(), { message: SECONDARY_MESSAGE, field: CATEGORY_FIELD }])
		),
		expectedDetails: [buildDetail(), { message: SECONDARY_MESSAGE, field: CATEGORY_FIELD }]
	},
	{
		label: "drop a blank message",
		error: buildApiError(buildEnvelope([buildDetail({ message: "   " })])),
		expectedDetails: []
	},
	{
		label: "drop a non-string field",
		error: buildApiError(buildEnvelope([{ field: 7, message: VALIDATION_MESSAGE }])),
		expectedDetails: [{ message: VALIDATION_MESSAGE }]
	},
	{
		label: "drop a non-string message",
		error: buildApiError(buildEnvelope([{ field: NAME_FIELD, message: 7 }])),
		expectedDetails: []
	},
	{
		label: "drop a detail that is not an object",
		error: buildApiError(buildEnvelope([null, "boom"])),
		expectedDetails: []
	},
	{
		label: "drop a missing errors list",
		error: buildApiError({ status: VALIDATION_STATUS, error: "Bad Request" }),
		expectedDetails: []
	},
	{
		label: "drop an errors value that is not an array",
		error: buildApiError({ status: VALIDATION_STATUS, errors: buildDetail() }),
		expectedDetails: []
	},
	{
		label: "drop a null body",
		error: buildApiError(null),
		expectedDetails: []
	},
	{
		label: "drop a string body",
		error: buildApiError("boom"),
		expectedDetails: []
	},
	{
		label: "drop an error that is not an ApiError",
		error: new Error("nope"),
		expectedDetails: []
	},
	{
		label: "drop an envelope on a plain object",
		error: { body: buildEnvelope([buildDetail()]) },
		expectedDetails: []
	}
];

interface MessageCase {
	readonly label: string;
	readonly error: unknown;
	readonly expectedMessage: string;
}

const MESSAGE_CASES: MessageCase[] = [
	{
		label: "return the first detail text",
		error: buildApiError(
			buildEnvelope([buildDetail(), { field: CATEGORY_FIELD, message: SECONDARY_MESSAGE }])
		),
		expectedMessage: VALIDATION_MESSAGE
	},
	{
		label: "fall back when the list is empty",
		error: buildApiError(buildEnvelope([])),
		expectedMessage: FALLBACK_MESSAGE
	},
	{
		label: "fall back when every detail is dropped",
		error: buildApiError(buildEnvelope([{ message: "  " }])),
		expectedMessage: FALLBACK_MESSAGE
	},
	{
		label: "fall back for a non-ApiError",
		error: new Error("nope"),
		expectedMessage: FALLBACK_MESSAGE
	}
];

describe("getApiErrorDetails", () => {
	it.each(DETAIL_CASES)("should $label", ({ error, expectedDetails }) => {
		// GIVEN
		const testSubject = getApiErrorDetails;

		// WHEN
		const expected = testSubject(error);

		// THEN
		expect(expected).toEqual(expectedDetails);
	});
});

describe("getApiErrorMessage", () => {
	it.each(MESSAGE_CASES)("should $label", ({ error, expectedMessage }) => {
		// GIVEN
		const testSubject = getApiErrorMessage;

		// WHEN
		const expected = testSubject(error, FALLBACK_MESSAGE);

		// THEN
		expect(expected).toBe(expectedMessage);
	});
});
