/* eslint-disable @typescript-eslint/no-explicit-any */

import { getChartColors } from "../../../../app.utils";
import { AnalyticsSummary } from "../../model/dashboard.model";

export function mapSummaryToAssetAllocationChart(
	summary: AnalyticsSummary | null,
	isDarkMode: boolean
) {
	if (!summary || !summary.assetAllocation) return null;

	const labels = Object.keys(summary.assetAllocation);
	const data = Object.values(summary.assetAllocation);

	const colors = getChartColors(isDarkMode);

	return {
		labels,
		datasets: [
			{
				data,
				backgroundColor: colors,
				borderWidth: 0
			}
		]
	};
}

export function getAssetAllocationOptions(isDarkMode: boolean) {
	const textColor = isDarkMode ? "#ffffff" : "#000000";

	return {
		plugins: {
			legend: {
				position: "right",
				labels: {
					color: textColor,
					usePointStyle: true,
					pointStyle: "circle"
				}
			},
			tooltip: {
				backgroundColor: isDarkMode ? "#1f2937" : "#ffffff",
				titleColor: isDarkMode ? "#ffffff" : "#000000",
				bodyColor: isDarkMode ? "#d1d5db" : "#4b5563",
				borderColor: isDarkMode ? "rgba(255, 255, 255, 0.1)" : "rgba(0, 0, 0, 0.1)",
				borderWidth: 1,
				padding: 12,
				boxPadding: 6,
				callbacks: {
					label: (context: any) => {
						const label = context.label || "";
						const value = context.parsed || 0;
						return `${label}: ${value.toFixed(2)}%`;
					}
				}
			}
		},
		responsive: true,
		maintainAspectRatio: false
	};
}
