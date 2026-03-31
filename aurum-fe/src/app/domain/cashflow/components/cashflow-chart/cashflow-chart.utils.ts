import {
	chartColors,
	getChartTooltipStyle,
	getOrCreateTooltipEl,
	positionTooltipEl,
	tooltipContainer,
	tooltipRow
} from "../../../../shared/utils";
import { CashFlowEntry, MONTH_LABELS } from "../../model/cashflow.model";
import { Chart, ChartConfiguration, ChartDataset, Plugin } from "chart.js";

export function mapCashFlowToChartData(entries: CashFlowEntry[]): {
	labels: string[];
	datasets: ChartDataset<"bar">[];
} {
	const earnedColor = chartColors[2];
	const spentColor = chartColors[0];

	return {
		labels: entries.map(e => MONTH_LABELS[e.month - 1]),
		datasets: [
			{
				label: "Earned",
				data: entries.map(e => e.earned),
				backgroundColor: earnedColor + "CC",
				borderColor: earnedColor,
				borderWidth: 1,
				borderRadius: 6,
				borderSkipped: false,
				maxBarThickness: 32
			} as ChartDataset<"bar">,
			{
				label: "Spent",
				data: entries.map(e => e.spent),
				backgroundColor: spentColor + "CC",
				borderColor: spentColor,
				borderWidth: 1,
				borderRadius: 6,
				borderSkipped: false,
				maxBarThickness: 32
			} as ChartDataset<"bar">
		]
	};
}

export function buildBarLabelsPlugin(
	currencySymbol: string,
	isDarkMode: boolean,
	isPrivacyMode: boolean
): Plugin<"bar"> {
	const labelColor = isDarkMode ? "rgba(255,255,255,0.55)" : "rgba(0,0,0,0.45)";
	const font = "11px ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace";

	return {
		id: "cashflowBarLabels",
		afterDatasetsDraw(chart: Chart<"bar">) {
			const { ctx } = chart;
			ctx.save();
			ctx.font = font;
			ctx.fillStyle = labelColor;
			ctx.textAlign = "center";
			ctx.textBaseline = "bottom";

			chart.data.datasets.forEach((_dataset, datasetIndex) => {
				const meta = chart.getDatasetMeta(datasetIndex);
				if (meta.hidden) return;

				meta.data.forEach((bar, index) => {
					const raw = chart.data.datasets[datasetIndex]?.data[index];
					const value = typeof raw === "number" ? raw : 0;
					if (value === 0) return;

					const label = isPrivacyMode ? "••••" : `${currencySymbol}${compactNumber(value)}`;

					ctx.fillText(label, bar.x, bar.y - 3);
				});
			});

			ctx.restore();
		}
	};
}

function compactNumber(value: number): string {
	if (value >= 1_000_000) return `${(value / 1_000_000).toFixed(1)}M`;
	if (value >= 1_000) return `${(value / 1_000).toFixed(1)}K`;
	return value.toFixed(0);
}

export function getCashFlowChartOptions(
	currencySymbol: string,
	isDarkMode: boolean,
	locale: string,
	isPrivacyMode: boolean = false,
	entries: CashFlowEntry[] = []
): ChartConfiguration["options"] {
	const textColor = isDarkMode ? "rgba(255,255,255,0.4)" : "rgba(0,0,0,0.35)";
	const t = getChartTooltipStyle(isDarkMode);
	const earnedColor = chartColors[2];
	const spentColor = chartColors[0];

	return {
		responsive: true,
		maintainAspectRatio: false,
		interaction: { mode: "index", intersect: false },
		layout: { padding: { top: 20 } },
		plugins: {
			legend: { display: false },
			tooltip: {
				enabled: false,
				external: ({ chart, tooltip }) => {
					const el = getOrCreateTooltipEl("cashflow-" + chart.id.toString());

					if (tooltip.opacity === 0) {
						el.style.opacity = "0";
						return;
					}

					const dataPoints = tooltip.dataPoints ?? [];
					const index = dataPoints[0]?.dataIndex ?? 0;
					const title = tooltip.title?.[0] ?? "";

					const earnedPoint = dataPoints.find(p => p.dataset.label === "Earned");
					const spentPoint = dataPoints.find(p => p.dataset.label === "Spent");

					const earnedVal = (earnedPoint?.parsed as { y: number } | undefined)?.y ?? 0;
					const spentVal = (spentPoint?.parsed as { y: number } | undefined)?.y ?? 0;
					const balance = earnedVal - spentVal;
					const balanceSign = balance >= 0 ? "+" : "";
					const balanceColor = balance >= 0 ? "#22c55e" : "#ef4444";

					const fmt = (v: number) =>
						isPrivacyMode
							? "••••••"
							: `${currencySymbol}${Math.abs(v).toLocaleString(locale, { minimumFractionDigits: 2 })}`;

					const earnedDot = `<span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:${earnedColor};margin-right:6px;flex-shrink:0;"></span>`;
					const spentDot = `<span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:${spentColor};margin-right:6px;flex-shrink:0;"></span>`;

					let variationHtml = "";
					if (index > 0 && entries.length > 0) {
						const prev = entries[index - 1];
						const curr = entries[index];
						if (prev && curr) {
							const prevBalance = prev.earned - prev.spent;
							const currBalance = curr.earned - curr.spent;
							if (prevBalance !== 0) {
								const diff = currBalance - prevBalance;
								const pct = (diff / Math.abs(prevBalance)) * 100;
								const sign = diff >= 0 ? "+" : "";
								const color = diff >= 0 ? "#22c55e" : "#ef4444";
								const formattedDiff = isPrivacyMode
									? "••••••"
									: `${sign}${currencySymbol}${(diff >= 0 ? "" : "-") + Math.abs(diff).toLocaleString(locale, { minimumFractionDigits: 2 })}`;
								const formattedPct = isPrivacyMode
									? "••••••"
									: `${sign}${(isNaN(pct) || !isFinite(pct) ? 0 : pct).toFixed(2)}%`;
								variationHtml = `<div style="margin-top:8px;padding-top:8px;border-top:1px solid ${t.border};font-size:12px;font-family:${t.font};color:${t.body};">vs prev:&nbsp;<strong style="color:${color};font-family:${t.monoFont};">${formattedDiff}&nbsp;(${formattedPct})</strong></div>`;
							}
						}
					}

					const balanceFormatted = isPrivacyMode
						? "••••••"
						: `${balanceSign}${currencySymbol}${(balance >= 0 ? "" : "-") + Math.abs(balance).toLocaleString(locale, { minimumFractionDigits: 2 })}`;

					const balanceDot = `<span style="display:inline-block;width:8px;height:2px;border-radius:1px;background:${t.muted};margin-right:6px;flex-shrink:0;"></span>`;
					const balanceRow = tooltipRow(
						t,
						balanceDot,
						"Balance",
						`<span style="color:${balanceColor};font-family:${t.monoFont};">${balanceFormatted}</span>`
					);

					const rowsHtml =
						tooltipRow(t, earnedDot, "Earned", fmt(earnedVal)) +
						tooltipRow(t, spentDot, "Spent", fmt(spentVal)) +
						`<div style="margin-top:6px;padding-top:6px;border-top:1px solid ${t.border};">` +
						balanceRow +
						"</div>" +
						variationHtml;

					el.innerHTML = tooltipContainer(t, title, rowsHtml);
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
				ticks: { color: textColor, font: { size: 11 } }
			},
			y: {
				display: false,
				beginAtZero: true
			}
		}
	} as ChartConfiguration["options"];
}
