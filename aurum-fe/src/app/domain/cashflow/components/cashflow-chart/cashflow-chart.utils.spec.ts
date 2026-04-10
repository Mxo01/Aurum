import {
	mapCashFlowToChartData,
	buildBarLabelsPlugin,
	getCashFlowChartOptions,
	BarLabelPluginState
} from "./cashflow-chart.utils";
import { CashFlowEntry } from "../../model/cashflow.model";
import { PRIVACY_PLACEHOLDER } from "../../../../shared/pipes/privacy-currency.pipe";
import type { Chart } from "chart.js";

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
	const buildMockState = (overrides: Partial<BarLabelPluginState> = {}): BarLabelPluginState => ({
		currencySymbol: "€",
		isDarkMode: false,
		isPrivacyMode: false,
		...overrides
	});

	const buildMockChart = (labels: string[]): Chart<"bar"> =>
		({
			width: 600,
			ctx: {
				save: vi.fn(),
				restore: vi.fn(),
				fillText: vi.fn((...args: unknown[]) => labels.push(args[0] as string))
			} as unknown as CanvasRenderingContext2D,
			data: {
				datasets: [{ label: "Earned", data: [1000] }]
			},
			getDatasetMeta: vi.fn().mockReturnValue({
				hidden: false,
				data: [{ x: 100, y: 50 }]
			})
		}) as unknown as Chart<"bar">;

	it("should return a plugin with id cashflowBarLabels", () => {
		// WHEN
		const result = buildBarLabelsPlugin(buildMockState());

		// THEN
		expect(result.id).toBe("cashflowBarLabels");
	});

	it("should include afterDatasetsDraw hook", () => {
		// WHEN
		const result = buildBarLabelsPlugin(buildMockState());

		// THEN
		expect(typeof result.afterDatasetsDraw).toBe("function");
	});

	it("should return a different plugin instance each call", () => {
		// WHEN
		const first = buildBarLabelsPlugin(buildMockState());
		const second = buildBarLabelsPlugin(
			buildMockState({ currencySymbol: "$", isDarkMode: true, isPrivacyMode: true })
		);

		// THEN
		expect(first).not.toBe(second);
	});

	it("should use PRIVACY_PLACEHOLDER as the bar label when privacy mode is on", () => {
		// GIVEN
		const state = buildMockState({ isPrivacyMode: true });
		const plugin = buildBarLabelsPlugin(state);
		const labels: string[] = [];
		const mockChart = buildMockChart(labels);

		// WHEN
		plugin.afterDatasetsDraw!(mockChart, {}, {}, false);

		// THEN
		expect(labels).toContain(PRIVACY_PLACEHOLDER);
	});

	it("should show currency value when privacy mode is off", () => {
		// GIVEN
		const state = buildMockState({ isPrivacyMode: false, currencySymbol: "€" });
		const plugin = buildBarLabelsPlugin(state);
		const labels: string[] = [];
		const mockChart = buildMockChart(labels);

		// WHEN
		plugin.afterDatasetsDraw!(mockChart, {}, {}, false);

		// THEN
		expect(labels[0]).toMatch(/^€/);
		expect(labels[0]).not.toBe(PRIVACY_PLACEHOLDER);
	});

	it("should reflect updated state without recreating the plugin", () => {
		// GIVEN
		const state = buildMockState({ isPrivacyMode: false });
		const plugin = buildBarLabelsPlugin(state);
		const labelsBeforeToggle: string[] = [];
		const labelsAfterToggle: string[] = [];

		plugin.afterDatasetsDraw!(buildMockChart(labelsBeforeToggle), {}, {}, false);
		expect(labelsBeforeToggle[0]).not.toBe(PRIVACY_PLACEHOLDER);

		// WHEN — mutate state to enable privacy mode
		state.isPrivacyMode = true;
		plugin.afterDatasetsDraw!(buildMockChart(labelsAfterToggle), {}, {}, false);

		// THEN — same plugin instance now emits the placeholder
		expect(labelsAfterToggle).toContain(PRIVACY_PLACEHOLDER);
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

	it("should show x axis ticks when not mobile", () => {
		// WHEN
		const result = getCashFlowChartOptions("€", false, "en-US", false, [], false) as {
			scales: { x: { ticks: { display?: boolean } } };
		};

		// THEN
		expect(result.scales.x.ticks.display).not.toBe(false);
	});

	it("should hide x axis ticks when mobile", () => {
		// WHEN
		const result = getCashFlowChartOptions("€", false, "en-US", false, [], true) as {
			scales: { x: { ticks: { display: boolean } } };
		};

		// THEN
		expect(result.scales.x.ticks.display).toBe(false);
	});
});
