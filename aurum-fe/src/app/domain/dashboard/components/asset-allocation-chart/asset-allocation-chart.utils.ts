import { chartColors } from "../../../../app.utils";
import { AnalyticsSummary } from "../../model/dashboard.model";

export interface MeterItem {
	label: string;
	value: number;
	color: string;
}

export function mapSummaryToMeterItems(summary: AnalyticsSummary | null): MeterItem[] {
	if (!summary?.assetAllocation) return [];

	return Object.entries(summary.assetAllocation)
		.filter(([, v]) => (v as number) > 0)
		.sort((a, b) => (b[1] as number) - (a[1] as number))
		.map(([label, value], i) => ({
			label,
			value: value as number,
			color: chartColors[i % chartColors.length]
		}));
}
