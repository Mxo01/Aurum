import { AnalyticsSummary } from "../../model/dashboard.model";

export function mapSummaryToAssetAllocationChart(summary: AnalyticsSummary | null) {
	if (!summary || !summary.assetAllocation) return null;

	const labels = Object.keys(summary.assetAllocation);
	const data = Object.values(summary.assetAllocation);

	return {
		labels,
		datasets: [
			{
				data,
				backgroundColor: [
					"#f9fafb",
					"#e5e7eb",
					"#d1d5db",
					"#9ca3af",
					"#6b7280",
					"#4b5563",
					"#1f2937",
					"#030712"
				]
			}
		]
	};
}
