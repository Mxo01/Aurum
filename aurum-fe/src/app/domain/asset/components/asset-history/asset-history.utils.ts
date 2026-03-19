import { Locale } from "../../../profile/model/locale.model";
import { Snapshot } from "../../../snapshot/model/snapshot.model";
import {
	chartTooltipStyle,
	getOrCreateTooltipEl,
	positionTooltipEl,
	tooltipContainer,
	tooltipRow
} from "../../../../app.utils";

export function mapSnapshotsToChartData(
	snapshots: Snapshot[],
	isDarkMode: boolean,
	locale: Locale
) {
	const data = snapshots.toSorted(
		(a, b) => new Date(a.referenceDate).getTime() - new Date(b.referenceDate).getTime()
	);

	return {
		labels: data.map(({ referenceDate }) =>
			new Date(referenceDate).toLocaleDateString(locale, {
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

export function getChartOptions(currencySymbol: string, isDarkMode: boolean, locale: Locale) {
	const textColor = isDarkMode ? "#ffffff" : "#000000";
	const gridColor = isDarkMode ? "rgba(255, 255, 255, 0.1)" : "rgba(0, 0, 0, 0.1)";
	const t = chartTooltipStyle;

	return {
		responsive: true,
		maintainAspectRatio: false,
		plugins: {
			legend: { display: false },
			tooltip: {
				enabled: false,
				external: ({
					chart,
					tooltip
				}: {
					chart: { id: number; canvas: HTMLCanvasElement };
					tooltip: {
						opacity: number;
						title?: string[];
						dataPoints?: {
							dataset: { label?: string; borderColor?: string };
							parsed: { y: number };
						}[];
						caretX: number;
						caretY: number;
					};
				}) => {
					const el = getOrCreateTooltipEl(chart.id.toString());

					if (tooltip.opacity === 0) {
						el.style.opacity = "0";
						return;
					}

					const title = tooltip.title?.[0] ?? "";
					const rowsHtml = (tooltip.dataPoints ?? [])
						.map(point => {
							const formattedVal = `${currencySymbol}${point.parsed.y.toLocaleString(locale, { minimumFractionDigits: 2 })}`;
							const dotColor = (point.dataset.borderColor as string | undefined) ?? t.muted;
							const dot = `<span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:${dotColor};margin-right:6px;flex-shrink:0;"></span>`;
							return tooltipRow(dot, point.dataset.label ?? "", formattedVal);
						})
						.join("");

					el.innerHTML = tooltipContainer(title, rowsHtml);

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
			x: { ticks: { color: textColor } },
			y: {
				beginAtZero: false,
				grid: { color: gridColor },
				ticks: {
					color: textColor,
					callback: (value: number) => `${currencySymbol} ${value.toLocaleString(locale)}`
				}
			}
		}
	};
}
