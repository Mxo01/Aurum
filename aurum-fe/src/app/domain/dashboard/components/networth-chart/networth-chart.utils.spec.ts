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

const buildMockCanvas = () => document.createElement("canvas");

type ExternalFn = (params: {
	chart: { id: number; canvas: HTMLCanvasElement };
	tooltip: {
		opacity: number;
		title?: string[];
		dataPoints?: {
			dataIndex: number;
			dataset: { label?: string; data: number[]; borderColor?: string };
			parsed: { y: number };
		}[];
		caretX: number;
		caretY: number;
	};
}) => void;

const getExternalFn = (
	currencySymbol: string,
	isDarkMode: boolean,
	locale: string,
	isPrivacyMode = false
): ExternalFn => {
	const options = getNetworthChartOptions(currencySymbol, isDarkMode, locale, isPrivacyMode);
	return (options?.plugins as unknown as { tooltip: { external: ExternalFn } }).tooltip.external;
};

describe("mapDataIntoNetworthChartData", () => {
	it("should return empty labels and a single bar dataset when data is null", () => {
		// GIVEN / WHEN
		const result = mapDataIntoNetworthChartData(null, false);

		// THEN
		expect(result.labels).toHaveLength(0);
		expect(result.datasets).toHaveLength(1);
		expect(result.datasets[0].type).toBe("bar");
	});

	it("should map labels from ChartData", () => {
		// GIVEN
		const data = buildMockChartData({ labels: ["Jan", "Feb", "Mar"] });

		// WHEN
		const result = mapDataIntoNetworthChartData(data, false);

		// THEN
		expect(result.labels).toEqual(["Jan", "Feb", "Mar"]);
	});

	it("should create the bar dataset with totalAssetsOnly data", () => {
		// GIVEN
		const totals = [1000, 2000, 3000];
		const data = buildMockChartData({ totalAssetsOnly: totals });

		// WHEN
		const result = mapDataIntoNetworthChartData(data, false);
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
		const result = mapDataIntoNetworthChartData(data, false);
		const lineDatasets = result.datasets.filter(d => d.type === "line");

		// THEN
		expect(lineDatasets).toHaveLength(2);
	});

	it("should return only the bar dataset when there are no favoriteAssetsValues", () => {
		// GIVEN
		const data = buildMockChartData({ favoriteAssetsValues: {} });

		// WHEN
		const result = mapDataIntoNetworthChartData(data, false);

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

	it("should set tooltip opacity to 0 when tooltip opacity is 0", () => {
		// GIVEN
		const external = getExternalFn("€", false, "en-US");
		const mockCanvas = buildMockCanvas();

		// WHEN
		external({
			chart: { id: 9001, canvas: mockCanvas },
			tooltip: { opacity: 0, caretX: 0, caretY: 0 }
		});

		// THEN
		const el = document.getElementById("chartjs-tooltip-9001");
		expect(el?.style.opacity).toBe("0");
	});

	it("should render tooltip at index 0 without a variation row", () => {
		// GIVEN
		const external = getExternalFn("€", false, "en-US");
		const mockCanvas = buildMockCanvas();

		// WHEN / THEN — no exception thrown
		expect(() =>
			external({
				chart: { id: 9002, canvas: mockCanvas },
				tooltip: {
					opacity: 1,
					title: ["Jan"],
					dataPoints: [
						{
							dataIndex: 0,
							dataset: { label: "Total Assets", data: [1000], borderColor: "#fff" },
							parsed: { y: 1000 }
						}
					],
					caretX: 0,
					caretY: 0
				}
			})
		).not.toThrow();
	});

	it("should render a positive variation row when current value exceeds previous", () => {
		// GIVEN
		const external = getExternalFn("$", false, "en-US");
		const mockCanvas = buildMockCanvas();

		// WHEN
		external({
			chart: { id: 9003, canvas: mockCanvas },
			tooltip: {
				opacity: 1,
				title: ["Feb"],
				dataPoints: [
					{
						dataIndex: 1,
						dataset: { label: "Total Assets", data: [1000, 1200], borderColor: "#fff" },
						parsed: { y: 1200 }
					}
				],
				caretX: 0,
				caretY: 0
			}
		});

		// THEN
		const el = document.getElementById("chartjs-tooltip-9003");
		expect(el?.innerHTML).toContain("Variation");
	});

	it("should render a negative variation row when current value is below previous", () => {
		// GIVEN
		const external = getExternalFn("$", false, "en-US");
		const mockCanvas = buildMockCanvas();

		// WHEN
		external({
			chart: { id: 9004, canvas: mockCanvas },
			tooltip: {
				opacity: 1,
				title: ["Feb"],
				dataPoints: [
					{
						dataIndex: 1,
						dataset: { label: "Total Assets", data: [1200, 1000], borderColor: "#fff" },
						parsed: { y: 1000 }
					}
				],
				caretX: 0,
				caretY: 0
			}
		});

		// THEN
		const el = document.getElementById("chartjs-tooltip-9004");
		expect(el?.innerHTML).toContain("Variation");
	});

	it("should skip variation row when prevVal is 0", () => {
		// GIVEN
		const external = getExternalFn("€", false, "en-US");
		const mockCanvas = buildMockCanvas();

		// WHEN / THEN — no exception thrown
		expect(() =>
			external({
				chart: { id: 9005, canvas: mockCanvas },
				tooltip: {
					opacity: 1,
					dataPoints: [
						{
							dataIndex: 1,
							dataset: { label: "Total Assets", data: [0, 1000] },
							parsed: { y: 1000 }
						}
					],
					caretX: 0,
					caretY: 0
				}
			})
		).not.toThrow();
	});

	it("should skip variation row when no Total Assets data point is found", () => {
		// GIVEN
		const external = getExternalFn("€", false, "en-US");
		const mockCanvas = buildMockCanvas();

		// WHEN / THEN — no exception thrown
		expect(() =>
			external({
				chart: { id: 9006, canvas: mockCanvas },
				tooltip: {
					opacity: 1,
					dataPoints: [
						{
							dataIndex: 1,
							dataset: { label: "Line Asset", data: [100, 200], borderColor: "#aaa" },
							parsed: { y: 200 }
						}
					],
					caretX: 0,
					caretY: 0
				}
			})
		).not.toThrow();
	});

	it("should use 0 when percent calculation yields NaN", () => {
		// GIVEN
		const external = getExternalFn("€", false, "en-US");
		const mockCanvas = buildMockCanvas();

		// WHEN / THEN — no exception thrown
		expect(() =>
			external({
				chart: { id: 9007, canvas: mockCanvas },
				tooltip: {
					opacity: 1,
					dataPoints: [
						{
							dataIndex: 1,
							dataset: { label: "Total Assets", data: [NaN, 1000] },
							parsed: { y: 1000 }
						}
					],
					caretX: 0,
					caretY: 0
				}
			})
		).not.toThrow();
	});

	it("should show privacy placeholder in body rows when privacy mode is on", () => {
		// GIVEN
		const external = getExternalFn("€", false, "en-US", true);
		const mockCanvas = buildMockCanvas();

		// WHEN
		external({
			chart: { id: 9009, canvas: mockCanvas },
			tooltip: {
				opacity: 1,
				title: ["Jan"],
				dataPoints: [
					{
						dataIndex: 0,
						dataset: { label: "Total Assets", data: [1000], borderColor: "#fff" },
						parsed: { y: 1000 }
					}
				],
				caretX: 0,
				caretY: 0
			}
		});

		// THEN
		const el = document.getElementById("chartjs-tooltip-9009");
		expect(el?.innerHTML).toContain("••••••");
	});

	it("should show privacy placeholder in variation row when privacy mode is on", () => {
		// GIVEN
		const external = getExternalFn("$", false, "en-US", true);
		const mockCanvas = buildMockCanvas();

		// WHEN
		external({
			chart: { id: 9010, canvas: mockCanvas },
			tooltip: {
				opacity: 1,
				title: ["Feb"],
				dataPoints: [
					{
						dataIndex: 1,
						dataset: { label: "Total Assets", data: [1000, 1200], borderColor: "#fff" },
						parsed: { y: 1200 }
					}
				],
				caretX: 0,
				caretY: 0
			}
		});

		// THEN
		const el = document.getElementById("chartjs-tooltip-9010");
		expect(el?.innerHTML).toContain("••••••");
	});

	it("should use fallback dot color from muted style when borderColor is absent", () => {
		// GIVEN
		const external = getExternalFn("€", false, "en-US");
		const mockCanvas = buildMockCanvas();

		// WHEN / THEN — no exception thrown
		expect(() =>
			external({
				chart: { id: 9008, canvas: mockCanvas },
				tooltip: {
					opacity: 1,
					dataPoints: [
						{
							dataIndex: 0,
							dataset: { label: "Total Assets", data: [500] },
							parsed: { y: 500 }
						}
					],
					caretX: 0,
					caretY: 0
				}
			})
		).not.toThrow();
	});
});
