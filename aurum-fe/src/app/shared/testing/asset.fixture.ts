import { faker } from "@faker-js/faker";
import type { Asset } from "../../domain/asset/model/asset.model";
import { AssetType } from "../../domain/asset/model/asset.model";
import { Currency } from "../../domain/profile/model/currency.model";

/**
 * A complete asset that satisfies the API contract. Tests override only the
 * fields they assert on; every other field keeps a realistic, typed default so
 * the fixture never drifts from the generated contract.
 */
export const buildMockAsset = (overrides: Partial<Asset> = {}): Asset => ({
	id: faker.string.uuid(),
	name: faker.lorem.word(),
	categoryId: faker.string.uuid(),
	categoryName: faker.lorem.word(),
	categoryIcon: null,
	type: AssetType.ASSET,
	originalCurrency: Currency.EUR,
	isFavorite: false,
	initialValue: null,
	latestValue: null,
	latestValueBase: null,
	previousValue: null,
	referenceDate: null,
	liabilityType: null,
	paymentFrequency: null,
	paymentAmount: null,
	...overrides
});
