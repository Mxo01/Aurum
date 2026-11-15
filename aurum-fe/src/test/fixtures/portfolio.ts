import { faker } from "@faker-js/faker";
import { ASSET_TYPE, LIABILITY_TYPE } from "@/lib/api/types";
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
		currency: "EUR",
		exchangeRateToBase: 1,
		amountBaseCurrency: faker.number.float({ min: 1000, max: 50000, fractionDigits: 2 }),
		source: "USER",
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

export const LIABILITY_INCIDENT_NET_WORTH = 47979.66;

const LIABILITY_INCIDENT_LIABILITIES = 0;
const LIABILITY_INCIDENT_LEGACY_PAYMENT_AMOUNT = 0;
const LIABILITY_INCIDENT_FAVORITE_NAMES = ["VWCE", "Fondo pensione"];
const LIABILITY_INCIDENT_CHART_LABELS = ["SEP 25", "OCT 25", "NOV 25"];
const LIABILITY_INCIDENT_SERIES_PROGRESS = [0.94, 0.97, 1];

interface LiabilityIncidentAssetSeed {
	categoryName: string;
	hasLegacyLiabilityType: boolean;
	latestValue: number;
	name: string;
}

const LIABILITY_INCIDENT_ASSET_SEEDS: LiabilityIncidentAssetSeed[] = [
	{
		name: "VWCE",
		latestValue: 18420.35,
		categoryName: "Investments",
		hasLegacyLiabilityType: true
	},
	{
		name: "Fondo pensione",
		latestValue: 12340,
		categoryName: "Retirement",
		hasLegacyLiabilityType: true
	},
	{ name: "Conto deposito", latestValue: 5000, categoryName: "Cash", hasLegacyLiabilityType: true },
	{ name: "BTP Valore", latestValue: 4219.31, categoryName: "Bonds", hasLegacyLiabilityType: true },
	{ name: "Liquidità", latestValue: 2500, categoryName: "Cash", hasLegacyLiabilityType: true },
	{ name: "ETC Oro", latestValue: 1800, categoryName: "Commodities", hasLegacyLiabilityType: true },
	{ name: "Azioni Enel", latestValue: 1200, categoryName: "Stocks", hasLegacyLiabilityType: true },
	{
		name: "Obbligazioni Romania",
		latestValue: 900,
		categoryName: "Bonds",
		hasLegacyLiabilityType: true
	},
	{ name: "Bitcoin", latestValue: 700, categoryName: "Crypto", hasLegacyLiabilityType: true },
	{
		name: "Fondo obbligazionario",
		latestValue: 600,
		categoryName: "Bonds",
		hasLegacyLiabilityType: true
	},
	{
		name: "ETF Europa",
		latestValue: 150,
		categoryName: "Investments",
		hasLegacyLiabilityType: true
	},
	{ name: "Buoni fruttiferi", latestValue: 80, categoryName: "Cash", hasLegacyLiabilityType: true },
	{
		name: "Fondo emergenti",
		latestValue: 50,
		categoryName: "Investments",
		hasLegacyLiabilityType: false
	},
	{
		name: "Collezione vini",
		latestValue: 15,
		categoryName: "Collectibles",
		hasLegacyLiabilityType: false
	},
	{
		name: "P2P lending",
		latestValue: 5,
		categoryName: "Investments",
		hasLegacyLiabilityType: false
	}
];

function incidentSeries(latestValue: number) {
	return LIABILITY_INCIDENT_SERIES_PROGRESS.map(progress =>
		Number((latestValue * progress).toFixed(2))
	);
}

function buildIncidentAsset(seed: LiabilityIncidentAssetSeed): AssetDTO {
	const category = buildCategory({ name: seed.categoryName, type: ASSET_TYPE.ASSET });

	return buildAsset({
		name: seed.name,
		categoryId: category.id,
		categoryName: category.name,
		categoryIcon: category.icon,
		type: ASSET_TYPE.ASSET,
		isFavorite: LIABILITY_INCIDENT_FAVORITE_NAMES.includes(seed.name),
		latestValue: seed.latestValue,
		latestValueBase: seed.latestValue,
		previousValue: seed.latestValue,
		initialValue: seed.latestValue,
		// Twelve rows in ASSET categories still carry the legacy liability type: the incident itself.
		liabilityType: seed.hasLegacyLiabilityType ? LIABILITY_TYPE.MANUAL : null,
		paymentAmount: seed.hasLegacyLiabilityType ? LIABILITY_INCIDENT_LEGACY_PAYMENT_AMOUNT : null
	});
}

export interface LiabilityClassificationIncident {
	assets: AssetDTO[];
	chart: ChartDataDTO;
	summary: AnalyticsSummaryDTO;
}

export function buildLiabilityClassificationIncident(): LiabilityClassificationIncident {
	const assets = LIABILITY_INCIDENT_ASSET_SEEDS.map(buildIncidentAsset);
	const favoriteSeeds = LIABILITY_INCIDENT_ASSET_SEEDS.filter(seed =>
		LIABILITY_INCIDENT_FAVORITE_NAMES.includes(seed.name)
	);
	const netWorthSeries = incidentSeries(LIABILITY_INCIDENT_NET_WORTH);

	return {
		assets,
		chart: buildChart({
			labels: LIABILITY_INCIDENT_CHART_LABELS,
			totalNetWorth: netWorthSeries,
			totalAssetsOnly: [...netWorthSeries],
			favoriteAssetsValues: Object.fromEntries(
				favoriteSeeds.map(
					seed => [seed.name, incidentSeries(seed.latestValue)] as [string, number[]]
				)
			),
			favoriteAssetsTypes: Object.fromEntries(
				favoriteSeeds.map(seed => [seed.name, ASSET_TYPE.ASSET] as [string, string])
			)
		}),
		summary: buildSummary({
			totalNetWorth: LIABILITY_INCIDENT_NET_WORTH,
			totalGrossAssets: LIABILITY_INCIDENT_NET_WORTH,
			totalLiabilities: LIABILITY_INCIDENT_LIABILITIES,
			debtToAssetRatio: 0,
			topAssets: assets
		})
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
