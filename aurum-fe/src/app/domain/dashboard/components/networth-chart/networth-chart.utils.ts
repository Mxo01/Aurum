/* eslint-disable @typescript-eslint/no-explicit-any */

import { getChartColors } from "../../../../app.utils";
import { ChartData } from "../../model/dashboard.model";
import { ChartConfiguration, ChartDataset } from "chart.js";

export function mapDataIntoNetworthChartData(data: ChartData | null, isDarkMode: boolean) {
	const labels = data?.labels?.map(date =>
		new Date(date).toLocaleDateString("it-IT", {
			day: "2-digit",
			month: "2-digit",
			year: "numeric"
		})
	);

	const colors = getChartColors(isDarkMode);

	const datasets: ChartDataset[] = [
		{
			type: "line",
			label: "Total Net Worth",
			data: data?.totalNetWorth || [],
			borderColor: isDarkMode ? "#ffffff" : "#000000",
			backgroundColor: "transparent",
			tension: 0.4,
			pointRadius: 4,
			pointBackgroundColor: isDarkMode ? "#ffffff" : "#000000",
			yAxisID: "y",
			order: -1
		} as any
	];

	if (data?.favoriteAssetsValues) {
		Object.entries(data.favoriteAssetsValues).forEach(([name, values], index) => {
			datasets.push({
				type: "bar",
				label: name,
				backgroundColor: colors[index % colors.length],
				data: values,
				borderRadius: 4,
				stack: "a"
			} as any);
		});
	}

	return {
		labels: labels || [],
		datasets
	};
}

export function getNetworthChartOptions(
	currencySymbol: string,
	isDarkMode: boolean
): ChartConfiguration["options"] {
	const textColor = isDarkMode ? "#ffffff" : "#000000";
	const gridColor = isDarkMode ? "rgba(255, 255, 255, 0.1)" : "rgba(0, 0, 0, 0.1)";

	return {
		responsive: true,
		maintainAspectRatio: false,
		interaction: {
			mode: "index",
			intersect: false
		},
		plugins: {
			legend: {
				display: true,
				position: "bottom",
				labels: {
					color: textColor,
					usePointStyle: true,
					pointStyle: "circle",
					padding: 20
				}
			},
			tooltip: {
				backgroundColor: isDarkMode ? "#1f2937" : "#ffffff",
				titleColor: isDarkMode ? "#ffffff" : "#000000",
				bodyColor: isDarkMode ? "#d1d5db" : "#4b5563",
				footerColor: isDarkMode ? "#9ca3af" : "#6b7280",
				borderColor: gridColor,
				borderWidth: 1,
				padding: 12,
				boxPadding: 6,
				usePointStyle: true,
				callbacks: {
					label: (context: any) => {
						let label = context.dataset.label || "";
						if (label) label += ": ";
						if (context.parsed.y !== null) {
							label += `${currencySymbol} ${context.parsed.y.toLocaleString("it-IT", { minimumFractionDigits: 2 })}`;
						}
						return label;
					},
					footer: (tooltipItems: any[]) => {
						const index = tooltipItems[0].dataIndex;
						if (index === 0) return "";

						const totalDataset = tooltipItems.find(i => i.dataset.label === "Total Net Worth");
						if (!totalDataset) return "";

						const currentVal = totalDataset.parsed.y;
						const prevVal = totalDataset.dataset.data[index - 1];

						if (prevVal === undefined || prevVal === null || prevVal === 0) {
							if (currentVal === 0) return "";
							return prevVal === 0
								? `\nInitial Value: ${currencySymbol} ${currentVal.toLocaleString("it-IT")}`
								: "";
						}

						const diff = currentVal - prevVal;
						const percent = (diff / prevVal) * 100;

						const sign = diff > 0 ? "+" : "";
						const formattedDiff = diff.toLocaleString("it-IT", { minimumFractionDigits: 2 });
						const formattedPercent = (isNaN(percent) || !isFinite(percent) ? 0 : percent).toFixed(
							2
						);

						return `\nVariation: ${sign}${currencySymbol} ${formattedDiff} (${sign}${formattedPercent}%)`;
					}
				}
			}
		},
		scales: {
			x: {
				stacked: true,
				grid: { display: false },
				ticks: { color: textColor }
			},
			y: {
				beginAtZero: false,
				grid: { color: gridColor },
				ticks: {
					color: textColor,
					callback: (value: any) => `${currencySymbol} ${value.toLocaleString("it-IT")}`
				}
			}
		}
	} as any;
}
