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
	variations: Variation;
	assetAllocation: { [key: string]: number };
	currencyImpact: number;
	savingsRate: number;
}

export interface ChartData {
	labels: string[];
	totalNetWorth: number[];
	favoriteAssetsValue: number[];
}

export interface Target {
	id: string;
	name: string;
	targetAmount: number;
	currentAmount: number;
	completionPercentage: number;
	deadline: string;
	isCompleted: boolean;
}
