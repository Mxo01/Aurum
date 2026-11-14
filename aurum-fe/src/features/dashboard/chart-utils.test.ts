import { describe, expect, it } from "vitest";
import type { ChartDataDTO } from "@/lib/api/types";
import {
	buildChartRows,
	hasChartData,
	liabilitiesSparkline,
	monthlyAverage,
	monthlyAveragePercent,
	projectedGrowth,
	yearOptions
} from "./chart-utils";

function buildChartData(overrides: Partial<ChartDataDTO> = {}): ChartDataDTO {
	return {
		labels: ["JAN", "FEB"],
		totalNetWorth: [50, 60],
		totalAssetsOnly: [100, 140],
		favoriteAssetsValues: { Apartment: [100, 110] },
		favoriteAssetsTypes: { Apartment: "ASSET" },
		...overrides
	};
}

const CURRENT_YEAR = 2026;

describe("hasChartData", () => {
	const mockChartWithZeros = buildChartData({ totalAssetsOnly: [0, 0] });
	const mockChartWithValues = buildChartData({ totalAssetsOnly: [0, 1] });

	it.each([
		{ title: "an undefined chart", chart: undefined, expected: false },
		{ title: "a chart with only zeros", chart: mockChartWithZeros, expected: false },
		{ title: "a chart with a non-zero value", chart: mockChartWithValues, expected: true }
	])("should return $expected for $title", ({ chart, expected }) => {
		// GIVEN
		const testSubject = hasChartData;

		// WHEN
		const expectedResult = testSubject(chart);

		// THEN
		expect(expectedResult).toBe(expected);
	});
});

describe("buildChartRows", () => {
	it("should return empty rows and favorites without a chart", () => {
		// GIVEN
		const testSubject = buildChartRows;

		// WHEN
		const expectedResult = testSubject(undefined);

		// THEN
		expect(expectedResult).toEqual({ rows: [], favorites: [] });
	});

	it("should map labels, net worth and favorite values per row", () => {
		// GIVEN
		const mockChart = buildChartData({
			favoriteAssetsValues: { Apartment: [100, 110], Mortgage: [300, 320] },
			favoriteAssetsTypes: { Apartment: "ASSET", Mortgage: "LIABILITY" }
		});
		const testSubject = buildChartRows;

		// WHEN
		const expectedResult = testSubject(mockChart);

		// THEN
		expect(expectedResult.rows).toEqual([
			{ label: "JAN", netWorth: 50, favorite0: 100, favorite1: 300 },
			{ label: "FEB", netWorth: 60, favorite0: 110, favorite1: 320 }
		]);
		expect(expectedResult.favorites).toEqual([
			{ key: "favorite0", name: "Apartment", isLiability: false },
			{ key: "favorite1", name: "Mortgage", isLiability: true }
		]);
	});

	it("should fall back to zero when the arrays are shorter than the labels", () => {
		// GIVEN
		const mockChart = buildChartData({
			totalNetWorth: [50],
			favoriteAssetsValues: { Apartment: [100] }
		});
		const testSubject = buildChartRows;

		// WHEN
		const expectedResult = testSubject(mockChart);

		// THEN
		expect(expectedResult.rows).toEqual([
			{ label: "JAN", netWorth: 50, favorite0: 100 },
			{ label: "FEB", netWorth: 0, favorite0: 0 }
		]);
	});

	it("should return no rows when the chart has no labels", () => {
		// GIVEN
		const mockChart = buildChartData({ labels: [] });
		const testSubject = buildChartRows;

		// WHEN
		const expectedResult = testSubject(mockChart);

		// THEN
		expect(expectedResult.rows).toEqual([]);
		expect(expectedResult.favorites).toHaveLength(1);
	});
});

describe("liabilitiesSparkline", () => {
	it("should return an empty series without a chart", () => {
		// GIVEN
		const testSubject = liabilitiesSparkline;

		// WHEN
		const expectedResult = testSubject(undefined);

		// THEN
		expect(expectedResult).toEqual([]);
	});

	it("should collapse consecutive equal values", () => {
		// GIVEN
		const mockChart = buildChartData({
			totalAssetsOnly: [100, 110, 110, 130],
			totalNetWorth: [60, 65, 65, 90]
		});
		const testSubject = liabilitiesSparkline;

		// WHEN
		const expectedResult = testSubject(mockChart);

		// THEN
		expect(expectedResult).toEqual([40, 45, 40]);
	});

	it("should treat a missing net worth as zero liabilities", () => {
		// GIVEN
		const mockChart = buildChartData({ totalAssetsOnly: [100, 120], totalNetWorth: [] });
		const testSubject = liabilitiesSparkline;

		// WHEN
		const expectedResult = testSubject(mockChart);

		// THEN
		expect(expectedResult).toEqual([100, 120]);
	});
});

describe("projectedGrowth", () => {
	it.each([
		{ title: "no projection", projected: undefined, current: 100, expected: undefined },
		{ title: "no current value", projected: 120, current: undefined, expected: undefined },
		{ title: "a zero projection", projected: 0, current: 100, expected: undefined },
		{ title: "a zero current value", projected: 120, current: 0, expected: undefined },
		{ title: "a higher projection", projected: 120, current: 100, expected: 20 },
		{ title: "a lower projection", projected: 80, current: 100, expected: -20 }
	])("should return $expected with $title", ({ projected, current, expected }) => {
		// GIVEN
		const testSubject = projectedGrowth;

		// WHEN
		const expectedResult = testSubject(projected, current);

		// THEN
		expect(expectedResult).toBe(expected);
	});
});

describe("monthlyAverage", () => {
	it.each([
		{ title: "an undefined total", absolute: undefined, expected: undefined },
		{ title: "a yearly total", absolute: 1200, expected: 100 }
	])("should return $expected for $title", ({ absolute, expected }) => {
		// GIVEN
		const testSubject = monthlyAverage;

		// WHEN
		const expectedResult = testSubject(absolute);

		// THEN
		expect(expectedResult).toBe(expected);
	});
});

describe("monthlyAveragePercent", () => {
	it("should return undefined without a yearly percentage", () => {
		// GIVEN
		const testSubject = monthlyAveragePercent;

		// WHEN
		const expectedResult = testSubject(undefined);

		// THEN
		expect(expectedResult).toBeUndefined();
	});

	it("should compound a yearly percentage down to a monthly one", () => {
		// GIVEN
		const mockYearlyPercentage = 12;
		const testSubject = monthlyAveragePercent;

		// WHEN
		const expectedResult = testSubject(mockYearlyPercentage);

		// THEN
		expect(expectedResult).toBeCloseTo(0.9489, 3);
	});
});

describe("yearOptions", () => {
	it.each([
		{
			title: "no oldest snapshot",
			oldestYear: null,
			expected: [CURRENT_YEAR]
		},
		{
			title: "an oldest snapshot in the past",
			oldestYear: 2024,
			expected: [2026, 2025, 2024]
		},
		{
			title: "an oldest snapshot after the current year",
			oldestYear: 2027,
			expected: []
		}
	])("should list the years for $title", ({ oldestYear, expected }) => {
		// GIVEN
		const testSubject = yearOptions;

		// WHEN
		const expectedResult = testSubject(oldestYear, CURRENT_YEAR);

		// THEN
		expect(expectedResult).toEqual(expected);
	});
});
