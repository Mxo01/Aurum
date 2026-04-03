import { faker } from "@faker-js/faker";
import { AssetType, Asset } from "../../model/asset.model";
import { Currency } from "../../../profile/model/currency.model";
import { mapAssetsToAssetsWithBalance } from "./asset-table.utils";

const buildMockAsset = (overrides: Partial<Asset> = {}): Asset => ({
	id: faker.string.uuid(),
	name: faker.lorem.word(),
	categoryId: faker.string.uuid(),
	categoryName: faker.lorem.word(),
	categoryIcon: null,
	type: AssetType.ASSET,
	originalCurrency: Currency.EUR,
	isFavorite: false,
	...overrides
});

describe("mapAssetsToAssetsWithBalance", () => {
	it("should use latestValue as currentValue when set", () => {
		// GIVEN
		const asset = buildMockAsset({ latestValue: 5000, latestValueBase: 5500 });

		// WHEN
		const [result] = mapAssetsToAssetsWithBalance([asset]);

		// THEN
		expect(result.currentValue).toBe(5000);
		expect(result.currentValueBase).toBe(5500);
	});

	it("should default currentValue to 0 when latestValue is null", () => {
		// GIVEN
		const asset = buildMockAsset({ latestValue: null, latestValueBase: null });

		// WHEN
		const [result] = mapAssetsToAssetsWithBalance([asset]);

		// THEN
		expect(result.currentValue).toBe(0);
		expect(result.currentValueBase).toBe(0);
	});

	it("should return null trend and trendPercentage when previousValue is null", () => {
		// GIVEN
		const asset = buildMockAsset({ latestValue: 1000, previousValue: null });

		// WHEN
		const [result] = mapAssetsToAssetsWithBalance([asset]);

		// THEN
		expect(result.trend).toBeNull();
		expect(result.trendPercentage).toBeNull();
	});

	it("should compute trend and trendPercentage when previousValue is non-zero", () => {
		// GIVEN
		const asset = buildMockAsset({ latestValue: 1200, previousValue: 1000 });

		// WHEN
		const [result] = mapAssetsToAssetsWithBalance([asset]);

		// THEN
		expect(result.trend).toBe(200);
		expect(result.trendPercentage).toBe(20);
	});

	it("should set trendPercentage to 100 and trend to currentValue when previousValue is 0 and currentValue is positive", () => {
		// GIVEN
		const asset = buildMockAsset({ latestValue: 500, previousValue: 0 });

		// WHEN
		const [result] = mapAssetsToAssetsWithBalance([asset]);

		// THEN
		expect(result.trend).toBe(500);
		expect(result.trendPercentage).toBe(100);
	});

	it("should map all assets in the input array", () => {
		// GIVEN
		const assets = [buildMockAsset(), buildMockAsset(), buildMockAsset()];

		// WHEN
		const results = mapAssetsToAssetsWithBalance(assets);

		// THEN
		expect(results).toHaveLength(3);
	});
});
