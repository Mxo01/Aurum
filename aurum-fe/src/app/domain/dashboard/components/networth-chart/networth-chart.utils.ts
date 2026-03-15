import { ChartData } from "../../model/dashboard.model";

export function mapDataIntoNetworthChartData(data: ChartData | null) {
	return {
		labels: data?.labels?.map(date =>
			new Date(date).toLocaleDateString("it-IT", {
				day: "2-digit",
				month: "2-digit",
				year: "numeric"
			})
		),
		datasets: [
			{
				type: "line",
				label: "Total Net Worth",
				data: data?.totalNetWorth,
				borderColor: "#ffffff",
				backgroundColor: "rgba(255, 255, 255, 0.1)",
				tension: 0.4,
				pointRadius: 4,
				pointBackgroundColor: "#ffffff",
				yAxisID: "y"
			},
			{
				type: "bar",
				label: "Top Assets",
				backgroundColor: "#f9fafb",
				borderRadius: 8,
				borderSkipped: false,
				data: data?.favoriteAssetsValue,
				yAxisID: "y"
			}
		]
	};
}
