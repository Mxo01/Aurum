/* eslint-disable @typescript-eslint/no-explicit-any */

import { Snapshot } from "../../../snapshot/model/snapshot.model";

export function mapSnapshotsToChartData(snapshots: Snapshot[]) {
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
				borderColor: "#ffffff",
				backgroundColor: "rgba(255, 255, 255, 0.1)",
				tension: 0.4,
				pointRadius: 4,
				pointBackgroundColor: "#ffffff"
			}
		]
	};
}

export function getChartOptions(currencySymbol: string) {
	return {
		responsive: true,
		maintainAspectRatio: false,
		plugins: {
			legend: { display: false },
			tooltip: {
				callbacks: {
					label: (context: any) => {
						let label = context.dataset.label || "";

						if (label) label += ": ";
						if (context.parsed.y !== null)
							label += `${currencySymbol} ${context.parsed.y.toLocaleString()}`;

						return label;
					}
				}
			}
		},
		scales: {
			x: { ticks: { color: "#ffffff" } },
			y: {
				beginAtZero: false,
				ticks: {
					color: "#ffffff",
					callback: (value: number) => `${currencySymbol} ${value.toLocaleString()}`
				}
			}
		}
	};
}
