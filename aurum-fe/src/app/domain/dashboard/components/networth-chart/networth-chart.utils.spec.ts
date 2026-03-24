import { faker } from "@faker-js/faker";
import { ChartData } from "../../model/dashboard.model";
import { getNetworthChartOptions, mapDataIntoNetworthChartData } from "./networth-chart.utils";

const buildMockChartData = (overrides: Partial<ChartData> = {}): ChartData => ({
	labels: [faker.date.month(), faker.date.month()],
	totalNetWorth: [faker.number.float(), faker.number.float()],
	totalAssetsOnly: [faker.number.float(), faker.number.float()],
	favoriteAssetsValues: {},
	favoriteAssetsTypes: {},
	...overrides
});

describe("mapDataIntoNetworthChartData", () => {
	it("should return empty labels and a single bar dataset when data is null", () => {
		// GIVEN / WHEN
		const result = mapDataIntoNetworthChartData(null);

		// THEN
		expect(result.labels).toHaveLength(0);
		expect(result.datasets).toHaveLength(1);
		expect(result.datasets[0].type).toBe("bar");
	});

	it("should map labels from ChartData", () => {
		// GIVEN
		const data = buildMockChartData({ labels: ["Jan", "Feb", "Mar"] });

		// WHEN
		const result = mapDataIntoNetworthChartData(data);

		// THEN
		expect(result.labels).toEqual(["Jan", "Feb", "Mar"]);
	});

	it("should create the bar dataset with totalAssetsOnly data", () => {
		// GIVEN
		const totals = [1000, 2000, 3000];
		const data = buildMockChartData({ totalAssetsOnly: totals });

		// WHEN
		const result = mapDataIntoNetworthChartData(data);
		const barDataset = result.datasets.find(d => d.type === "bar");

		// THEN
		expect(barDataset?.data).toEqual(totals);
	});

	it("should create a line dataset for each entry in favoriteAssetsValues", () => {
		// GIVEN
		const data = buildMockChartData({
			favoriteAssetsValues: {
				"Asset A": [100, 200],
				"Asset B": [300, 400]
			}
		});

		// WHEN
		const result = mapDataIntoNetworthChartData(data);
		const lineDatasets = result.datasets.filter(d => d.type === "line");

		// THEN
		expect(lineDatasets).toHaveLength(2);
	});

	it("should return only the bar dataset when there are no favoriteAssetsValues", () => {
		// GIVEN
		const data = buildMockChartData({ favoriteAssetsValues: {} });

		// WHEN
		const result = mapDataIntoNetworthChartData(data);

		// THEN
		expect(result.datasets).toHaveLength(1);
		expect(result.datasets[0].type).toBe("bar");
	});
});

describe("getNetworthChartOptions", () => {
	it("should return chart options with responsive true", () => {
		// WHEN
		const result = getNetworthChartOptions("€", false, "en-US");

		// THEN
		expect(result?.responsive).toBe(true);
	});

	it("should set white tick color in dark mode", () => {
		// WHEN
		const result = getNetworthChartOptions("€", true, "en-US");
		const scales = result?.scales as { x: { ticks: { color: string } } };

		// THEN
		expect(scales.x.ticks.color).toBe("#ffffff");
	});

	it("should set black tick color in light mode", () => {
		// WHEN
		const result = getNetworthChartOptions("€", false, "en-US");
		const scales = result?.scales as { x: { ticks: { color: string } } };

		// THEN
		expect(scales.x.ticks.color).toBe("#000000");
	});

	it("should disable the legend", () => {
		// WHEN
		const result = getNetworthChartOptions("€", false, "en-US");
		const plugins = result?.plugins as { legend: { display: boolean } };

		// THEN
		expect(plugins.legend.display).toBe(false);
	});
});
