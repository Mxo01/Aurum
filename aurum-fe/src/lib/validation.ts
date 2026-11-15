import { z } from "zod";
import { todayISO } from "@/lib/format/date";

/** The single copy of every client-side form sentence, so no surface can drift from another. */
export const MESSAGE = {
	nameRequired: "Name is required",
	nameTooLong: (max: number) => `Maximum ${max} characters`,
	categoryRequired: "Category is required",
	currencyRequired: "Select a currency",
	liabilityTypeRequired: "Select a type",
	frequencyRequired: "Select a frequency",
	paymentAmountRequired: "Enter the payment amount",
	amountRequired: "Enter an amount",
	numberInvalid: "Enter a number",
	amountNotNegative: "Cannot be negative",
	amountPositive: "Enter an amount greater than zero",
	dateRequired: "Pick a date",
	dateNotFuture: "The date cannot be in the future"
} as const;

export interface DecimalRule {
	optional?: boolean;
	required?: string;
	invalid?: string;
	min?: number;
	minMessage?: string;
	exclusiveMin?: boolean;
}

// A decimal keyboard types either separator, depending on the locale, and both mean the same amount.
const DECIMAL_PATTERN = /^[+-]?(?:\d+[.,]?\d*|[.,]\d+)$/;

export function parseDecimalInput(raw: string): number | null {
	const trimmed = raw.trim();
	if (!DECIMAL_PATTERN.test(trimmed)) return null;

	const value = Number(trimmed.replace(",", "."));
	// A digit string long enough to overflow the number range still matches the pattern.
	return Number.isFinite(value) ? value : null;
}

export function decimalError(raw: string, rule: DecimalRule = {}): string | null {
	if (raw.trim() === "") return rule.optional ? null : (rule.required ?? MESSAGE.amountRequired);

	const value = parseDecimalInput(raw);
	if (value === null) return rule.invalid ?? MESSAGE.numberInvalid;

	if (rule.min !== undefined) {
		const belowMin = rule.exclusiveMin ? value <= rule.min : value < rule.min;
		if (belowMin) return rule.minMessage ?? MESSAGE.amountNotNegative;
	}

	return null;
}

export function decimalText(rule: DecimalRule = {}) {
	return z.string().superRefine((raw, context) => {
		const message = decimalError(raw, rule);
		if (message) context.addIssue({ code: "custom", message });
	});
}

/** Only call once the matching `decimalText` rule has passed. */
export function decimalValue(raw: string): number {
	return parseDecimalInput(raw) ?? 0;
}

export function isFutureDate(value: string, today = todayISO()): boolean {
	return value > today;
}
