import { describe, expect, it } from "vitest";
import { buildAsset } from "@/test/fixtures/portfolio";
import { assetTrend, assetValue } from "./asset";

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
