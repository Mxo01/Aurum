/* eslint-disable @typescript-eslint/no-explicit-any */

import { Snapshot } from "../../../snapshot/model/snapshot.model";

export function mapSnapshotsToChartData(snapshots: Snapshot[], isDarkMode: boolean) {
	const data = snapshots.toSorted(
		(a, b) => new Date(a.referenceDate).getTime() - new Date(b.referenceDate).getTime()
	);

	return {
		labels: data.map(({ referenceDate }) =>
			new Date(referenceDate).toLocaleDateString("it-IT", {
				day: "2-digit",
				month: "2-digit",
				year: "numeric"
			})
		),
		datasets: [
			{
				label: "Value (Base)",
				data: data.map(({ amountBaseCurrency }) => amountBaseCurrency),
				fill: true,
				borderColor: isDarkMode ? "#ffffff" : "#000000",
				backgroundColor: isDarkMode ? "rgba(255, 255, 255, 0.1)" : "rgba(0, 0, 0, 0.05)",
				tension: 0.4,
				pointRadius: 4,
				pointBackgroundColor: isDarkMode ? "#ffffff" : "#000000"
			}
		]
	};
}

export function getChartOptions(currencySymbol: string, isDarkMode: boolean) {
	const textColor = isDarkMode ? "#ffffff" : "#000000";
	const gridColor = isDarkMode ? "rgba(255, 255, 255, 0.1)" : "rgba(0, 0, 0, 0.1)";

	return {
		responsive: true,
		maintainAspectRatio: false,
		plugins: {
			legend: { display: false },
			tooltip: {
				backgroundColor: isDarkMode ? "#1f2937" : "#ffffff",
				titleColor: isDarkMode ? "#ffffff" : "#000000",
				bodyColor: isDarkMode ? "#d1d5db" : "#4b5563",
				borderColor: gridColor,
				borderWidth: 1,
				padding: 12,
				boxPadding: 6,
				usePointStyle: true,
				callbacks: {
					label: (context: any) => {
						let label = context.dataset.label || "";

						if (label) label += ": ";
						if (context.parsed.y !== null)
							label += `${currencySymbol} ${context.parsed.y.toLocaleString("it-IT", { minimumFractionDigits: 2 })}`;

						return label;
					}
				}
			}
		},
		scales: {
			x: { ticks: { color: textColor } },
			y: {
				beginAtZero: false,
				grid: { color: gridColor },
				ticks: {
					color: textColor,
					callback: (value: number) => `${currencySymbol} ${value.toLocaleString("it-IT")}`
				}
			}
		}
	};
}
