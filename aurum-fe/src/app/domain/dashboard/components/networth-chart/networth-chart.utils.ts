import { chartColors } from "../../../../app.utils";
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
			backgroundColor: isDarkMode ? "rgba(255,255,255,0.1)" : "rgba(0,0,0,0.06)",
			borderRadius: 4,
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

function getOrCreateTooltipEl(chartId: string): HTMLDivElement {
	const id = `chartjs-tooltip-${chartId}`;
	let el = document.getElementById(id) as HTMLDivElement | null;
	if (!el) {
		el = document.createElement("div");
		el.id = id;
		el.style.position = "fixed";
		el.style.pointerEvents = "none";
		el.style.zIndex = "9999";
		el.style.transition = "opacity 0.15s ease";
		document.body.appendChild(el);
	}
	return el;
}

export function getNetworthChartOptions(
	currencySymbol: string,
	isDarkMode: boolean,
	locale: string
): ChartConfiguration["options"] {
	const textColor = isDarkMode ? "#ffffff" : "#000000";
	const bodyColor = isDarkMode ? "#d1d5db" : "#4b5563";
	const bgColor = isDarkMode ? "#1f2937" : "#ffffff";
	const borderColor = isDarkMode ? "rgba(255,255,255,0.1)" : "rgba(0,0,0,0.1)";
	const mutedColor = isDarkMode ? "#9ca3af" : "#6b7280";

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

					// Variation
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
							variationHtml = `
								<div style="margin-top:8px;padding-top:8px;border-top:1px solid ${borderColor};font-size:12px;color:${bodyColor};">
									Variation:&nbsp;<span style="color:${color};font-weight:600;">${sign}${currencySymbol}\u00A0${(diff >= 0 ? "" : "-") + formattedDiff}&nbsp;(${sign}${formattedPercent}%)</span>
								</div>`;
						}
					}

					// Body rows
					const rowsHtml = dataPoints
						.map(point => {
							const isTotal = point.dataset.label === "Total Assets";
							const y = (point.parsed as { y: number }).y;
							const formattedVal = `${currencySymbol}\u00A0${y.toLocaleString(locale, { minimumFractionDigits: 2 })}`;
							const dotColor =
								(point.dataset as { borderColor?: string }).borderColor ?? mutedColor;
							const dotHtml = isTotal
								? `<span style="display:inline-block;width:8px;height:2px;border-radius:1px;background:${mutedColor};margin-right:6px;flex-shrink:0;"></span>`
								: `<span style="display:inline-block;width:8px;height:8px;border-radius:50%;background:${dotColor};margin-right:6px;flex-shrink:0;"></span>`;
							return `<div style="display:flex;align-items:center;margin-bottom:4px;font-size:13px;color:${bodyColor};">${dotHtml}<span>${point.dataset.label}:&nbsp;<strong style="color:${textColor};">${formattedVal}</strong></span></div>`;
						})
						.join("");

					el.innerHTML = `
						<div style="background:${bgColor};border:1px solid ${borderColor};border-radius:8px;padding:12px 14px;min-width:200px;box-shadow:0 4px 16px rgba(0,0,0,0.15);">
							<div style="font-weight:700;font-size:13px;color:${textColor};margin-bottom:8px;">${title}</div>
							${rowsHtml}
							${variationHtml}
						</div>`;

					// Position
					const rect = chart.canvas.getBoundingClientRect();
					const tipWidth = el.offsetWidth || 220;
					const tipHeight = el.offsetHeight || 120;
					let left = rect.left + tooltip.caretX;
					let top = rect.top + tooltip.caretY - tipHeight - 12;

					if (left + tipWidth / 2 > window.innerWidth - 8) {
						left = window.innerWidth - tipWidth - 8;
					} else if (left - tipWidth / 2 < 8) {
						left = 8;
					} else {
						left -= tipWidth / 2;
					}
					if (top < 8) top = rect.top + tooltip.caretY + 12;

					el.style.left = `${left}px`;
					el.style.top = `${top}px`;
					el.style.opacity = "1";
				}
			}
		},
		scales: {
			x: {
				grid: { display: false },
				ticks: { color: textColor }
			},
			y: {
				beginAtZero: false,
				grid: { display: false },
				ticks: { display: false }
			}
		}
	} as ChartConfiguration["options"];
}
