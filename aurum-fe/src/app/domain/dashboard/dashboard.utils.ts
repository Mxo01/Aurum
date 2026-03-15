import { AnalyticsSummary } from "./model/dashboard.model";

export function mapSummaryToAssetAllocationChart(summary: AnalyticsSummary | null) {
	if (!summary || !summary.assetAllocation) return null;

	const labels = Object.keys(summary.assetAllocation);
	const data = Object.values(summary.assetAllocation);

	return {
		labels,
		datasets: [
			{
				data,
				backgroundColor: ["#42A5F5", "#66BB6A", "#FFA726", "#26C6DA", "#7E57C2", "#EF5350"]
			}
		]
	};
}
