import { isApiError } from "./client";

// What the reader hands back: a detail that is renderable by construction — `message` always carries
// text, `field` is present only when the wire held a real string. The generated detail stays the
// wire shape; this is the narrowed output, so consumers never re-check a guarantee already made.
export type ApiErrorViolation = { field?: string; message: string };

function isRecord(value: unknown): value is Record<string, unknown> {
	return typeof value === "object" && value !== null;
}

// The envelope is a contract, but a proxy or a gateway can still answer without it: a detail is kept
// only when it carries renderable text, and `field` only when it really is the string the sheet
// matches against its inputs.
function toApiErrorDetail(value: unknown): ApiErrorViolation | null {
	if (!isRecord(value)) return null;

	const { field, message } = value;
	if (typeof message !== "string" || message.trim().length === 0) return null;

	return typeof field === "string" ? { field, message } : { message };
}

function detailsOf(value: unknown): ApiErrorViolation[] {
	if (!Array.isArray(value)) return [];

	const entries: unknown[] = value;
	return entries
		.map(toApiErrorDetail)
		.filter((detail): detail is ApiErrorViolation => detail !== null);
}

export function getApiErrorDetails(error: unknown): ApiErrorViolation[] {
	if (!isApiError(error)) return [];

	const { body } = error;
	if (!isRecord(body)) return [];

	return detailsOf(body.errors);
}

export function getApiErrorMessage(error: unknown, fallback: string): string {
	return getApiErrorDetails(error)[0]?.message ?? fallback;
}
