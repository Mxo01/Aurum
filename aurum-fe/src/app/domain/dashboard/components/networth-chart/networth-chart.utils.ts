import {
	chartColors,
	getChartTooltipStyle,
	getOrCreateTooltipEl,
	positionTooltipEl,
	tooltipContainer,
	tooltipRow
} from "../../../../shared/utils";
import { ChartData } from "../../model/dashboard.model";
import { ChartConfiguration, ChartDataset } from "chart.js";

export function mapDataIntoNetworthChartData(
	data: ChartData | null,
	isDarkMode: boolean
): { labels: string[]; datasets: ChartDataset[] } {
	const labels = data?.labels ?? [];
	const colors = chartColors;

	const datasets: ChartDataset[] = [
		{
			type: "bar",
			label: "Total Assets",
			data: data?.totalAssetsOnly ?? [],
			backgroundColor: isDarkMode ? "rgba(255,255,255,0.7)" : "rgba(0,0,0,0.1)",
			borderRadius: 16,
			borderSkipped: false,
			order: 1
		} as ChartDataset<"bar">
	];

	if (data?.favoriteAssetsValues) {
		let colorIndex = 0;
		Object.entries(data.favoriteAssetsValues).forEach(([name, values]) => {
			const color = colors[colorIndex++ % colors.length];
			datasets.push({
				type: "line",
				label: name,
				data: values,
				borderColor: color,
				backgroundColor: "transparent",
				tension: 0.4,
				pointRadius: 0,
				pointHoverRadius: 5,
				pointBackgroundColor: color,
				order: 0
			} as ChartDataset<"line">);
		});
	}

	return { labels, datasets };
}

export function getNetworthChartOptions(
	currencySymbol: string,
	isDarkMode: boolean,
	locale: string
): ChartConfiguration["options"] {
	const textColor = isDarkMode ? "#ffffff" : "#000000";
	const t = getChartTooltipStyle(isDarkMode);

	return {
		responsive: true,
		maintainAspectRatio: false,
		interaction: {
			mode: "index",
			intersect: false
		},
		plugins: {
			legend: { display: false },
			tooltip: {
				enabled: false,
				external: ({ chart, tooltip }) => {
					const el = getOrCreateTooltipEl(chart.id.toString());

					if (tooltip.opacity === 0) {
						el.style.opacity = "0";
						return;
					}

					const dataPoints = tooltip.dataPoints ?? [];
					const index = dataPoints[0]?.dataIndex ?? 0;
					const title = tooltip.title?.[0] ?? "";

					// Variation row
					const totalPoint = dataPoints.find(p => p.dataset.label === "Total Assets");
					let variationHtml = "";
					if (index > 0 && totalPoint) {
						const currentVal = (totalPoint.parsed as { y: number }).y;
						const prevVal = totalPoint.dataset.data[index - 1] as number | undefined;
						if (prevVal !== undefined && prevVal !== null && prevVal !== 0) {
							const diff = currentVal - prevVal;
							const percent = (diff / Math.abs(prevVal)) * 100;
							const sign = diff >= 0 ? "+" : "";
							const color = diff >= 0 ? "#22c55e" : "#ef4444";
							const formattedDiff = Math.abs(diff).toLocaleString(locale, {
								minimumFractionDigits: 2
							});
							const formattedPercent = (
								isNaN(percent) || !isFinite(percent) ? 0 : Math.abs(percent)
							).toFixed(2);
							variationHtml = `<div style="margin-top:8px;padding-top:8px;border-top:1px solid ${t.border};font-size:12px;font-family:${t.font};color:${t.body};">Variation:&nbsp;<strong style="color:${color};font-family:${t.monoFont};">${sign}${currencySymbol}${(diff >= 0 ? "" : "-") + formattedDiff}&nbsp;(${sign}${formattedPercent}%)</strong></div>`;
						}
					}

					// Body rows
					const rowsHtml = dataPoints
						.map(point => {
							const isTotal = point.dataset.label === "Total Assets";
							const y = (point.parsed as { y: number }).y;
							const formattedVal = `${currencySymbol}${y.toLocaleString(locale, { minimumFractionDigits: 2 })}`;
							const dotColor = (point.dataset as { borderColor?: string }).borderColor ?? t.muted;
							const dot = isTotal
								? `<span style="display:inline-block;width:8px;height:2px;border-radius:1px;background:${t.muted};margin-right:6px;flex-shrink:0;"></span>`
								: `<span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:${dotColor};margin-right:6px;flex-shrink:0;"></span>`;
							return tooltipRow(t, dot, point.dataset.label ?? "", formattedVal);
						})
						.join("");

					el.innerHTML = tooltipContainer(t, title, rowsHtml + variationHtml);

					positionTooltipEl(
						el,
						chart.canvas.getBoundingClientRect(),
						tooltip.caretX,
						tooltip.caretY
					);
				}
			}
		},
		scales: {
			x: {
				grid: { display: false },
				border: { display: false },
				ticks: { color: textColor }
			},
			y: {
				display: false,
				beginAtZero: false
			}
		}
	} as ChartConfiguration["options"];
}
