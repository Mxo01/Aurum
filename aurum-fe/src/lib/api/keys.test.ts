import { describe, expect, it } from "vitest";
import { queryKeys } from "./keys";

const ALL_CHART_YEARS = "all";

describe("queryKeys", () => {
	it("should expose the static resource keys", () => {
		// GIVEN
		const testSubject = queryKeys;

		// THEN
		expect(testSubject.profile).toEqual(["profile"]);
		expect(testSubject.summary).toEqual(["analytics", "summary"]);
		expect(testSubject.projections).toEqual(["analytics", "projections"]);
		expect(testSubject.assets).toEqual(["assets"]);
		expect(testSubject.categories).toEqual(["categories"]);
		expect(testSubject.targets).toEqual(["targets"]);
	});

	it("should build the chart key for a given year", () => {
		// GIVEN
		const testSubject = queryKeys;
		const year = 2026;

		// WHEN
		const expected = testSubject.chart(year);

		// THEN
		expect(expected).toEqual(["analytics", "chart", year]);
	});

	it("should fall back to every year when the chart year is missing", () => {
		// GIVEN
		const testSubject = queryKeys;

		// WHEN
		const expected = testSubject.chart();

		// THEN
		expect(expected).toEqual(["analytics", "chart", ALL_CHART_YEARS]);
	});

	it("should scope the snapshots key to the asset", () => {
		// GIVEN
		const testSubject = queryKeys;
		const assetId = "asset-1";

		// WHEN
		const expected = testSubject.snapshots(assetId);

		// THEN
		expect(expected).toEqual(["snapshots", assetId]);
	});

	it("should scope the cashflow key to the year", () => {
		// GIVEN
		const testSubject = queryKeys;
		const year = 2026;

		// WHEN
		const expected = testSubject.cashflow(year);

		// THEN
		expect(expected).toEqual(["cashflow", year]);
	});
});
