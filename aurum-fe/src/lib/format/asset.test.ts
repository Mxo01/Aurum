import { describe, expect, it } from "vitest";
import type { AssetDTO } from "@/lib/api/types";
import { buildAsset } from "@/test/fixtures/portfolio";
import { assetTrend, assetValue } from "./asset";

const PREVIOUS_VALUE = 100;
const LATEST_VALUE = 110;
const GROWTH_PERCENT = 10;

interface CurrencyCase {
	readonly label: string;
	readonly latestValueCurrency: AssetDTO["latestValueCurrency"];
	readonly previousValueCurrency: AssetDTO["previousValueCurrency"];
	readonly expectedTrend: number | null;
}

const CURRENCY_CASES: CurrencyCase[] = [
	{
		label: "both values in the same currency",
		latestValueCurrency: "USD",
		previousValueCurrency: "USD",
		expectedTrend: GROWTH_PERCENT
	},
	{
		label: "values in different currencies",
		latestValueCurrency: "USD",
		previousValueCurrency: "EUR",
		expectedTrend: null
	},
	{
		label: "no recorded currencies",
		latestValueCurrency: null,
		previousValueCurrency: null,
		expectedTrend: GROWTH_PERCENT
	},
	{
		label: "only the latest value currency recorded",
		latestValueCurrency: "USD",
		previousValueCurrency: null,
		expectedTrend: GROWTH_PERCENT
	},
	{
		label: "only the previous value currency recorded",
		latestValueCurrency: null,
		previousValueCurrency: "EUR",
		expectedTrend: GROWTH_PERCENT
	}
];

describe("assetTrend", () => {
	it("should compute the percentage change", () => {
		// GIVEN
		const testSubject = assetTrend;
		const asset = buildAsset({ latestValue: 110, previousValue: 100 });

		// WHEN
		const expected = testSubject(asset);

		// THEN
		expect(expected).toBeCloseTo(10);
	});

	it("should return null without both values", () => {
		// GIVEN
		const testSubject = assetTrend;
		const asset = buildAsset({ latestValue: 110, previousValue: null });

		// THEN
		expect(testSubject(asset)).toBeNull();
	});

	it("should return null when the previous value is zero", () => {
		// GIVEN
		const testSubject = assetTrend;
		const asset = buildAsset({ latestValue: 110, previousValue: 0 });

		// THEN
		expect(testSubject(asset)).toBeNull();
	});

	it.each(CURRENCY_CASES)(
		"should handle $label",
		({ latestValueCurrency, previousValueCurrency, expectedTrend }) => {
			// GIVEN
			const testSubject = assetTrend;
			const asset = buildAsset({
				latestValue: LATEST_VALUE,
				previousValue: PREVIOUS_VALUE,
				latestValueCurrency,
				previousValueCurrency
			});

			// WHEN
			const actualTrend = testSubject(asset);

			// THEN
			if (expectedTrend === null) expect(actualTrend).toBeNull();
			else expect(actualTrend).toBeCloseTo(expectedTrend);
		}
	);
});

describe("assetValue", () => {
	it("should prefer the base currency value", () => {
		// GIVEN
		const testSubject = assetValue;
		const asset = buildAsset({ latestValue: 100, latestValueBase: 120 });

		// THEN
		expect(testSubject(asset)).toBe(120);
	});

	it("should fall back to the original value", () => {
		// GIVEN
		const testSubject = assetValue;
		const asset = buildAsset({ latestValue: 100, latestValueBase: null });

		// THEN
		expect(testSubject(asset)).toBe(100);
	});

	it("should fall back to zero", () => {
		// GIVEN
		const testSubject = assetValue;
		const asset = buildAsset({ latestValue: null, latestValueBase: null });

		// THEN
		expect(testSubject(asset)).toBe(0);
	});
});
