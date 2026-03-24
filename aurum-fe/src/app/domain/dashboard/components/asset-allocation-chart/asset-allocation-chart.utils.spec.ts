import { faker } from "@faker-js/faker";
import { AnalyticsSummary } from "../../model/dashboard.model";
import { mapSummaryToMeterItems } from "./asset-allocation-chart.utils";

const buildMockSummary = (
	assetAllocation: Record<string, number>
): Pick<AnalyticsSummary, "assetAllocation"> => ({
	assetAllocation
});

describe("mapSummaryToMeterItems", () => {
	it("should return an empty array when summary is null", () => {
		// GIVEN / WHEN
		const result = mapSummaryToMeterItems(null);

		// THEN
		expect(result).toHaveLength(0);
	});

	it("should return an empty array when all allocation values are zero", () => {
		// GIVEN
		const summary = buildMockSummary({ Stocks: 0, Cash: 0 }) as AnalyticsSummary;

		// WHEN
		const result = mapSummaryToMeterItems(summary);

		// THEN
		expect(result).toHaveLength(0);
	});

	it("should filter out zero-value allocations and map the rest", () => {
		// GIVEN
		const summary = buildMockSummary({ Stocks: 60, Cash: 0, Bonds: 40 }) as AnalyticsSummary;

		// WHEN
		const result = mapSummaryToMeterItems(summary);

		// THEN
		expect(result).toHaveLength(2);
		expect(result.every(item => item.value > 0)).toBe(true);
	});

	it("should sort items by value in descending order", () => {
		// GIVEN
		const allocation = {
			[faker.lorem.word()]: 10,
			[faker.lorem.word()]: 50,
			[faker.lorem.word()]: 30
		};
		const summary = buildMockSummary(allocation) as AnalyticsSummary;

		// WHEN
		const result = mapSummaryToMeterItems(summary);

		// THEN
		expect(result[0].value).toBe(50);
		expect(result[1].value).toBe(30);
		expect(result[2].value).toBe(10);
	});

	it("should assign a color to every item", () => {
		// GIVEN
		const summary = buildMockSummary({ A: 40, B: 60 }) as AnalyticsSummary;

		// WHEN
		const result = mapSummaryToMeterItems(summary);

		// THEN
		expect(result.every(item => !!item.color)).toBe(true);
	});
});
