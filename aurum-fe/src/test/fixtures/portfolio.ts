import { faker } from "@faker-js/faker";
import type {
	AnalyticsSummaryDTO,
	AssetCategoryDTO,
	AssetDTO,
	CashFlowYearDTO,
	ChartDataDTO,
	SnapshotDTO,
	TargetDTO,
	UserDTO,
	VariationDTO
} from "@/lib/api/types";

function dateOnly(value: Date) {
	return value.toISOString().slice(0, 10);
}

export function buildVariation(overrides: Partial<VariationDTO> = {}): VariationDTO {
	return {
		oneMonth: { absolute: 1200, percentage: 0.4 },
		sixMonths: { absolute: 8400, percentage: 2.8 },
		oneYear: { absolute: 21000, percentage: 7.2 },
		...overrides
	};
}

export function buildProfile(overrides: Partial<UserDTO> = {}): UserDTO {
	return {
		id: faker.string.uuid(),
		currency: "EUR",
		locale: "en-US",
		...overrides
	};
}

export function buildCategory(overrides: Partial<AssetCategoryDTO> = {}): AssetCategoryDTO {
	return {
		id: faker.string.uuid(),
		name: faker.commerce.department(),
		type: "ASSET",
		icon: "wallet",
		isDefault: false,
		...overrides
	};
}

export function buildSnapshot(overrides: Partial<SnapshotDTO> = {}): SnapshotDTO {
	return {
		id: faker.string.uuid(),
		assetId: faker.string.uuid(),
		assetName: faker.commerce.productName(),
		referenceDate: dateOnly(faker.date.recent()),
		amountOriginalCurrency: faker.number.float({ min: 1000, max: 50000, fractionDigits: 2 }),
		exchangeRateToBase: 1,
		amountBaseCurrency: faker.number.float({ min: 1000, max: 50000, fractionDigits: 2 }),
		...overrides
	};
}

export function buildAsset(overrides: Partial<AssetDTO> = {}): AssetDTO {
	const category = buildCategory();
	const value = faker.number.float({ min: 1000, max: 50000, fractionDigits: 2 });

	return {
		id: faker.string.uuid(),
		name: faker.commerce.productName(),
		categoryId: category.id,
		categoryName: category.name,
		categoryIcon: category.icon,
		type: "ASSET",
		originalCurrency: "EUR",
		isFavorite: false,
		snapshots: [],
		latestValue: value,
		latestValueBase: value,
		previousValue: value * 0.97,
		initialValue: value * 0.9,
		referenceDate: dateOnly(faker.date.recent()),
		liabilityType: null,
		paymentFrequency: null,
		paymentAmount: null,
		...overrides
	};
}

export function buildTarget(overrides: Partial<TargetDTO> = {}): TargetDTO {
	return {
		id: faker.string.uuid(),
		name: faker.commerce.productName(),
		targetAmount: 50000,
		currentAmount: 20000,
		completionPercentage: 40,
		deadline: dateOnly(faker.date.future()),
		isCompleted: false,
		type: "MANUAL",
		status: "IN_PROGRESS",
		...overrides
	};
}

export function buildSummary(overrides: Partial<AnalyticsSummaryDTO> = {}): AnalyticsSummaryDTO {
	const asset = buildAsset();

	return {
		totalNetWorth: 570000,
		totalGrossAssets: 758000,
		variations: buildVariation(),
		assetVariations: buildVariation(),
		assetAllocation: { "Real Estate": 56.7, Investments: 22.3, Cash: 5.6, Retirement: 11.4 },
		currencyImpact: 1830,
		totalLiabilities: 188000,
		debtToAssetRatio: 24.8,
		topAssets: [asset],
		oldestSnapshotYear: 2024,
		...overrides
	};
}

export function buildChart(overrides: Partial<ChartDataDTO> = {}): ChartDataDTO {
	return {
		labels: ["JAN 26", "FEB 26", "MAR 26"],
		totalNetWorth: [520000, 545000, 570000],
		totalAssetsOnly: [700000, 720000, 758000],
		favoriteAssetsValues: { "Apartment Berlin": [400000, 410000, 430000] },
		favoriteAssetsTypes: { "Apartment Berlin": "ASSET" },
		...overrides
	};
}

export function buildCashflowYear(overrides: Partial<CashFlowYearDTO> = {}): CashFlowYearDTO {
	const entries = Array.from({ length: 12 }, (_, index) => ({
		id: faker.string.uuid(),
		month: index + 1,
		earned: 5400 + index * 20,
		spent: 4100 + index * 15
	}));

	return {
		year: 2026,
		entries,
		previousYearEntries: entries,
		availableYears: [2024, 2025, 2026],
		...overrides
	};
}
