import { describe, expect, it } from "vitest";
import {
	PRIVACY_MASK,
	formatCurrency,
	formatNumber,
	formatPercent,
	getCurrencySymbol,
	safeDivide,
	toAmount
} from "./money";

describe("money formatting", () => {
	describe("formatCurrency", () => {
		it("should format an amount with the user currency and locale", () => {
			// GIVEN
			const testSubject = formatCurrency;
			const amount = 1234.5;

			// WHEN
			const expected = testSubject(amount, { currency: "EUR", locale: "en-US" });

			// THEN
			expect(expected).toBe("€1,234.50");
		});

		it("should use the requested number of fraction digits", () => {
			// GIVEN
			const testSubject = formatCurrency;

			// WHEN
			const expected = testSubject(1234.5, { currency: "EUR", locale: "en-US", digits: 0 });

			// THEN
			expect(expected).toBe("€1,235");
		});

		it("should use compact notation when asked", () => {
			// GIVEN
			const testSubject = formatCurrency;

			// WHEN
			const expected = testSubject(570000, {
				currency: "EUR",
				locale: "en-US",
				compact: true
			});

			// THEN
			expect(expected).toBe("€570K");
		});

		it("should show an explicit plus sign when requested", () => {
			// GIVEN
			const testSubject = formatCurrency;

			// WHEN
			const expected = testSubject(15, {
				currency: "EUR",
				locale: "en-US",
				signDisplay: "always"
			});

			// THEN
			expect(expected).toBe("+€15.00");
		});

		it("should mask the amount in privacy mode", () => {
			// GIVEN
			const testSubject = formatCurrency;

			// WHEN
			const expected = testSubject(999, { currency: "EUR", locale: "en-US", privacy: true });

			// THEN
			expect(expected).toBe(PRIVACY_MASK);
		});
	});

	describe("formatNumber", () => {
		it("should format with the locale separators", () => {
			// GIVEN
			const testSubject = formatNumber;

			// WHEN
			const expected = testSubject(1234.5, "it-IT");

			// THEN
			expect(expected).toBe("1234,50");
		});
	});

	describe("formatPercent", () => {
		it("should always show the absolute value with a percent sign", () => {
			// GIVEN
			const testSubject = formatPercent;

			// WHEN
			const expected = testSubject(-4.2, "en-US", 1);

			// THEN
			expect(expected).toBe("4.2%");
		});
	});

	describe("getCurrencySymbol", () => {
		it("should return the symbol of a supported currency", () => {
			// GIVEN
			const testSubject = getCurrencySymbol;

			// WHEN
			const expected = testSubject("USD");

			// THEN
			expect(expected).toBe("$");
		});

		it("should fall back to the currency code when the symbol cannot be resolved", () => {
			// GIVEN
			const testSubject = getCurrencySymbol;

			// WHEN
			const expected = testSubject("NOT_A_CURRENCY");

			// THEN
			expect(expected).toBe("NOT_A_CURRENCY");
		});
	});

	describe("toAmount", () => {
		it.each([
			{ input: null, expected: 0 },
			{ input: undefined, expected: 0 },
			{ input: Number.NaN, expected: 0 },
			{ input: 42, expected: 42 }
		])("should map $input to $expected", ({ input, expected }) => {
			// GIVEN
			const testSubject = toAmount;

			// WHEN
			const result = testSubject(input);

			// THEN
			expect(result).toBe(expected);
		});
	});

	describe("safeDivide", () => {
		it("should divide when the divisor is not zero", () => {
			// GIVEN
			const testSubject = safeDivide;

			// WHEN
			const expected = testSubject(10, 4);

			// THEN
			expect(expected).toBe(2.5);
		});

		it("should return null when the divisor is zero", () => {
			// GIVEN
			const testSubject = safeDivide;

			// WHEN
			const expected = testSubject(10, 0);

			// THEN
			expect(expected).toBeNull();
		});
	});
});
