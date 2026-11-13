import { describe, expect, it } from "vitest";
import { buildAsset } from "@/test/fixtures/portfolio";
import { groupAssets, portfolioTotals } from "./assets.utils";

describe("groupAssets", () => {
	it("should group assets by category and compute the total", () => {
		// GIVEN
		const testSubject = groupAssets;
		const mockAssets = [
			buildAsset({
				id: "1",
				name: "Apartment",
				categoryName: "Real Estate",
				categoryIcon: "pi-home",
				latestValueBase: 300
			}),
			buildAsset({
				id: "2",
				name: "House",
				categoryName: "Real Estate",
				categoryIcon: "pi-home",
				latestValueBase: 200
			}),
			buildAsset({
				id: "3",
				name: "ETF",
				categoryName: "Investments",
				categoryIcon: "pi-chart-line",
				latestValueBase: 100
			})
		];

		// WHEN
		const expected = testSubject(mockAssets);

		// THEN
		expect(expected).toHaveLength(2);
		expect(expected[0]?.name).toBe("Investments");
		expect(expected[1]?.name).toBe("Real Estate");
		expect(expected[1]?.total).toBe(500);
		expect(expected[1]?.assets.map(asset => asset.name)).toEqual(["Apartment", "House"]);
	});

	it("should fall back to the original value when the base value is missing", () => {
		// GIVEN
		const testSubject = groupAssets;

		// WHEN
		const expected = testSubject([
			buildAsset({ categoryName: "Cash", latestValue: 42, latestValueBase: null })
		]);

		// THEN
		expect(expected[0]?.total).toBe(42);
	});
});

describe("portfolioTotals", () => {
	it("should split assets and liabilities and compute the net", () => {
		// GIVEN
		const testSubject = portfolioTotals;
		const mockAssets = [
			buildAsset({ type: "ASSET", latestValueBase: 1000 }),
			buildAsset({ type: "ASSET", latestValueBase: 500 }),
			buildAsset({ type: "LIABILITY", latestValueBase: 300 })
		];

		// WHEN
		const expected = testSubject(mockAssets);

		// THEN
		expect(expected).toEqual({ assetTotal: 1500, liabilityTotal: 300, netTotal: 1200 });
	});
});
