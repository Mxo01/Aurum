import { Asset } from "../../asset/model/asset.model";

export interface Delta {
	absolute: number;
	percentage: number;
}

export interface Variation {
	oneMonth: Delta;
	sixMonths: Delta;
	oneYear: Delta;
}

export interface AnalyticsSummary {
	totalNetWorth: number;
	totalGrossAssets: number;
	variations: Variation;
	assetVariations: Variation;
	assetAllocation: { [key: string]: number };
	currencyImpact: number;
	totalLiabilities: number;
	debtToAssetRatio: number;
	topAssets: Asset[];
	oldestSnapshotYear: number | null;
}

export type Projections = { [year: number]: number };

export interface ChartData {
	labels: string[];
	totalNetWorth: number[];
	totalAssetsOnly: number[];
	favoriteAssetsValues: Record<string, number[]>;
	favoriteAssetsTypes: Record<string, string>;
}
