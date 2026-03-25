import { faker } from "@faker-js/faker";
import { Locale } from "../../../profile/model/locale.model";
import { Snapshot } from "../../../snapshot/model/snapshot.model";
import { getChartOptions, mapSnapshotsToChartData } from "./asset-history.utils";

const buildMockSnapshot = (referenceDate: string, overrides: Partial<Snapshot> = {}): Snapshot => ({
	id: faker.string.uuid(),
	assetId: faker.string.uuid(),
	referenceDate,
	amountOriginalCurrency: faker.number.float({ min: 100, max: 10000 }),
	amountBaseCurrency: faker.number.float({ min: 100, max: 10000 }),
	...overrides
});

const buildMockCanvas = () => {
	const canvas = document.createElement("canvas");
	return canvas;
};

describe("mapSnapshotsToChartData", () => {
	it("should sort snapshots in chronological order by referenceDate", () => {
		// GIVEN
		const snapshots = [
			buildMockSnapshot("2024-03-01"),
			buildMockSnapshot("2024-01-01"),
			buildMockSnapshot("2024-02-01")
		];

		// WHEN
		const result = mapSnapshotsToChartData(snapshots, false, Locale.EN_US);

		// THEN
		const dates = result.labels as string[];
		expect(new Date(dates[0]) <= new Date(dates[1])).toBe(true);
		expect(new Date(dates[1]) <= new Date(dates[2])).toBe(true);
	});

	it("should produce one label per snapshot", () => {
		// GIVEN
		const snapshots = [
			buildMockSnapshot("2024-01-01"),
			buildMockSnapshot("2024-02-01"),
			buildMockSnapshot("2024-03-01")
		];

		// WHEN
		const result = mapSnapshotsToChartData(snapshots, false, Locale.EN_US);

		// THEN
		expect(result.labels).toHaveLength(3);
	});

	it("should map amountBaseCurrency to the dataset data points", () => {
		// GIVEN
		const amounts = [1000, 2000, 3000];
		const snapshots = [
			buildMockSnapshot("2024-01-01", { amountBaseCurrency: 1000 }),
			buildMockSnapshot("2024-02-01", { amountBaseCurrency: 2000 }),
			buildMockSnapshot("2024-03-01", { amountBaseCurrency: 3000 })
		];

		// WHEN
		const result = mapSnapshotsToChartData(snapshots, false, Locale.EN_US);

		// THEN
		expect(result.datasets[0].data).toEqual(amounts);
	});

	it("should set a white border color in dark mode", () => {
		// GIVEN
		const snapshots = [buildMockSnapshot("2024-01-01")];

		// WHEN
		const result = mapSnapshotsToChartData(snapshots, true, Locale.EN_US);

		// THEN
		expect(result.datasets[0].borderColor).toBe("#ffffff");
	});

	it("should set a black border color in light mode", () => {
		// GIVEN
		const snapshots = [buildMockSnapshot("2024-01-01")];

		// WHEN
		const result = mapSnapshotsToChartData(snapshots, false, Locale.EN_US);

		// THEN
		expect(result.datasets[0].borderColor).toBe("#000000");
	});
});

describe("getChartOptions", () => {
	it("should return chart options with responsive true", () => {
		// WHEN
		const result = getChartOptions("€", false, Locale.EN_US);

		// THEN
		expect(result.responsive).toBe(true);
	});

	it("should set white tick color in dark mode", () => {
		// WHEN
		const result = getChartOptions("€", true, Locale.EN_US);

		// THEN
		expect((result.scales as { x: { ticks: { color: string } } }).x.ticks.color).toBe("#ffffff");
	});

	it("should set black tick color in light mode", () => {
		// WHEN
		const result = getChartOptions("€", false, Locale.EN_US);

		// THEN
		expect((result.scales as { x: { ticks: { color: string } } }).x.ticks.color).toBe("#000000");
	});

	it("should set tooltip opacity to 0 when tooltip opacity is 0", () => {
		// GIVEN
		const options = getChartOptions("€", false, Locale.EN_US);
		const external = options.plugins.tooltip.external;
		const mockCanvas = buildMockCanvas();

		// WHEN
		external({
			chart: { id: 8001, canvas: mockCanvas },
			tooltip: { opacity: 0, caretX: 0, caretY: 0 }
		});

		// THEN
		const el = document.getElementById("chartjs-tooltip-8001");
		expect(el?.style.opacity).toBe("0");
	});

	it("should render tooltip HTML when opacity is non-zero", () => {
		// GIVEN
		const options = getChartOptions("€", false, Locale.EN_US);
		const external = options.plugins.tooltip.external;
		const mockCanvas = buildMockCanvas();

		// WHEN
		external({
			chart: { id: 8002, canvas: mockCanvas },
			tooltip: {
				opacity: 1,
				title: ["15/01/2024"],
				dataPoints: [
					{ dataset: { label: "Value (Base)", borderColor: "#ff0000" }, parsed: { y: 5000 } }
				],
				caretX: 10,
				caretY: 10
			}
		});

		// THEN
		const el = document.getElementById("chartjs-tooltip-8002");
		expect(el?.innerHTML).toBeTruthy();
	});

	it("should fall back to muted color when dataset borderColor is undefined", () => {
		// GIVEN
		const options = getChartOptions("€", false, Locale.EN_US);
		const external = options.plugins.tooltip.external;
		const mockCanvas = buildMockCanvas();

		// WHEN / THEN — no exception thrown
		expect(() =>
			external({
				chart: { id: 8003, canvas: mockCanvas },
				tooltip: {
					opacity: 1,
					dataPoints: [{ dataset: { label: "Value (Base)" }, parsed: { y: 100 } }],
					caretX: 0,
					caretY: 0
				}
			})
		).not.toThrow();
	});

	it("should use empty string fallbacks when title and dataPoints are absent", () => {
		// GIVEN
		const options = getChartOptions("€", false, Locale.EN_US);
		const external = options.plugins.tooltip.external;
		const mockCanvas = buildMockCanvas();

		// WHEN / THEN — no exception thrown
		expect(() =>
			external({
				chart: { id: 8004, canvas: mockCanvas },
				tooltip: { opacity: 1, caretX: 0, caretY: 0 }
			})
		).not.toThrow();
	});
});
