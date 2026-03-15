import { ChartData } from "../../model/dashboard.model";

export function mapDataIntoNetworthChartData(data: ChartData | null) {
	return {
		labels: data?.labels,
		datasets: [
			{
				type: "line",
				label: "Total Net Worth",
				borderColor: "#ffffff",
				borderWidth: 2,
				fill: false,
				tension: 0.4,
				data: data?.totalNetWorth,
				yAxisID: "y"
			},
			{
				type: "bar",
				label: "Top Assets",
				backgroundColor: "rgba(120, 144, 156, 0.5)",
				data: data?.favoriteAssetsValue,
				yAxisID: "y"
			}
		]
	};
}

export const netWorthChartOptions = {
	responsive: true,
	maintainAspectRatio: false,
	plugins: { legend: { position: "bottom" } },
	scales: { x: { grid: { display: false } }, y: { beginAtZero: false } }
};
