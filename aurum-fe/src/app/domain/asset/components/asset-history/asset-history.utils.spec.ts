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
});
