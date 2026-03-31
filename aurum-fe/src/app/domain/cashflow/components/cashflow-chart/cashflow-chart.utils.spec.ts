import {
	mapCashFlowToChartData,
	buildBarLabelsPlugin,
	getCashFlowChartOptions
} from "./cashflow-chart.utils";
import { CashFlowEntry } from "../../model/cashflow.model";

const buildMockEntry = (month: number, earned: number, spent: number): CashFlowEntry => ({
	id: null,
	month,
	earned,
	spent
});

describe("mapCashFlowToChartData", () => {
	it("should map entries to correct month labels", () => {
		// GIVEN
		const stubbedEntries = [buildMockEntry(1, 1000, 500), buildMockEntry(6, 2000, 800)];

		// WHEN
		const result = mapCashFlowToChartData(stubbedEntries);

		// THEN
		expect(result.labels).toEqual(["JAN", "JUN"]);
	});

	it("should produce two datasets labeled Earned and Spent", () => {
		// GIVEN
		const stubbedEntries = [buildMockEntry(3, 1500, 900)];

		// WHEN
		const result = mapCashFlowToChartData(stubbedEntries);

		// THEN
		expect(result.datasets).toHaveLength(2);
		expect(result.datasets[0].label).toBe("Earned");
		expect(result.datasets[1].label).toBe("Spent");
	});

	it("should map earned values to first dataset and spent values to second", () => {
		// GIVEN
		const stubbedEntries = [buildMockEntry(1, 1000, 500), buildMockEntry(2, 2000, 300)];

		// WHEN
		const result = mapCashFlowToChartData(stubbedEntries);

		// THEN
		expect(result.datasets[0].data).toEqual([1000, 2000]);
		expect(result.datasets[1].data).toEqual([500, 300]);
	});

	it("should produce labels for all provided entries", () => {
		// GIVEN
		const stubbedEntries = Array.from({ length: 12 }, (_, i) => buildMockEntry(i + 1, 100, 50));

		// WHEN
		const result = mapCashFlowToChartData(stubbedEntries);

		// THEN
		expect(result.labels).toHaveLength(12);
		expect(result.labels[11]).toBe("DEC");
	});
});

describe("buildBarLabelsPlugin", () => {
	it("should return a plugin with id cashflowBarLabels", () => {
		// WHEN
		const result = buildBarLabelsPlugin("€", false, false);

		// THEN
		expect(result.id).toBe("cashflowBarLabels");
	});

	it("should include afterDatasetsDraw hook", () => {
		// WHEN
		const result = buildBarLabelsPlugin("€", false, false);

		// THEN
		expect(typeof result.afterDatasetsDraw).toBe("function");
	});

	it("should return a different plugin instance each call", () => {
		// WHEN
		const first = buildBarLabelsPlugin("€", false, false);
		const second = buildBarLabelsPlugin("$", true, true);

		// THEN
		expect(first).not.toBe(second);
	});
});

describe("getCashFlowChartOptions", () => {
	it("should hide the y axis", () => {
		// WHEN
		const result = getCashFlowChartOptions("€", false, "en-US") as {
			scales: { y: { display: boolean } };
		};

		// THEN
		expect(result.scales.y.display).toBe(false);
	});

	it("should set responsive to true", () => {
		// WHEN
		const result = getCashFlowChartOptions("€", false, "en-US");

		// THEN
		expect(result?.responsive).toBe(true);
	});

	it("should disable the legend", () => {
		// WHEN
		const result = getCashFlowChartOptions("€", false, "en-US") as {
			plugins: { legend: { display: boolean } };
		};

		// THEN
		expect(result.plugins.legend.display).toBe(false);
	});

	it("should hide x axis grid", () => {
		// WHEN
		const result = getCashFlowChartOptions("€", false, "en-US") as {
			scales: { x: { grid: { display: boolean } } };
		};

		// THEN
		expect(result.scales.x.grid.display).toBe(false);
	});

	it("should set beginAtZero on y axis", () => {
		// WHEN
		const result = getCashFlowChartOptions("€", false, "en-US") as {
			scales: { y: { beginAtZero: boolean } };
		};

		// THEN
		expect(result.scales.y.beginAtZero).toBe(true);
	});

	it("should set layout top padding to 20", () => {
		// WHEN
		const result = getCashFlowChartOptions("€", false, "en-US") as {
			layout: { padding: { top: number } };
		};

		// THEN
		expect(result.layout.padding.top).toBe(20);
	});
});
