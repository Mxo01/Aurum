/* eslint-disable @typescript-eslint/no-explicit-any */

type Truthy<T> = Exclude<T, false | "" | null | undefined>;

export function isTruthy<T>(value: T): value is Truthy<T> {
	return value !== false && value !== "" && value !== null && value !== undefined;
}

export function formatDateToISO(date: Date): string {
	return (
		date.getFullYear() +
		"-" +
		String(date.getMonth() + 1).padStart(2, "0") +
		"-" +
		String(date.getDate()).padStart(2, "0")
	);
}

// ─── Chart utils ─────────────────────────────────────────────────────────────

/**
 * Returns a Chart.js scriptable backgroundColor function that renders a vertical
 * canvas gradient: 15% opacity near the line fading to fully transparent at the bottom.
 * Pass the result directly as a dataset's `backgroundColor`.
 */
/**
 * Returns a Chart.js scriptable backgroundColor function that renders a vertical
 * canvas gradient across the full chart area, suitable for bar datasets.
 */
export function createBarGradient(
	topColor: string,
	bottomColor: string
): (context: any) => CanvasGradient | string {
	return context => {
		const chart = context.chart;
		const { ctx, chartArea } = chart;
		if (!chartArea) return topColor;
		const gradient = ctx.createLinearGradient(0, chartArea.top, 0, chartArea.bottom);
		gradient.addColorStop(0, topColor);
		gradient.addColorStop(1, bottomColor);
		return gradient;
	};
}

export function createGradientFill(
	borderColorHex: string
): (context: any) => CanvasGradient | string {
	return context => {
		const chart = context.chart;
		const { ctx, chartArea } = chart;
		if (!chartArea) return "transparent";
		const gradient = ctx.createLinearGradient(0, chartArea.top, 0, chartArea.bottom);
		gradient.addColorStop(0, borderColorHex + "26"); // 15% opacity near the line
		gradient.addColorStop(1, borderColorHex + "00"); // transparent at the bottom
		return gradient;
	};
}

export const chartColors = ["#1D2D44", "#597491", "#748CAB", "#99aec9", "#c0d5f0"];

const monoFont = "ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace";

export function getChartTooltipStyle(isDarkMode: boolean) {
	return isDarkMode
		? {
				bg: "#18181B",
				title: "#ffffff",
				body: "#d1d5db",
				border: "rgba(255,255,255,0.1)",
				muted: "#9ca3af",
				font: "inherit",
				monoFont
			}
		: {
				bg: "#f4f4f5",
				title: "#09090b",
				body: "#3f3f46",
				border: "rgba(0,0,0,0.08)",
				muted: "#71717a",
				font: "inherit",
				monoFont
			};
}

export type ChartTooltipStyle = ReturnType<typeof getChartTooltipStyle>;

export function getOrCreateTooltipEl(chartId: string): HTMLDivElement {
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

export function positionTooltipEl(
	el: HTMLDivElement,
	canvasRect: DOMRect,
	caretX: number,
	caretY: number
): void {
	const tipWidth = el.offsetWidth || 220;
	const tipHeight = el.offsetHeight || 120;
	let left = canvasRect.left + caretX;
	let top = canvasRect.top + caretY - tipHeight - 12;

	if (left + tipWidth / 2 > window.innerWidth - 8) {
		left = window.innerWidth - tipWidth - 8;
	} else if (left - tipWidth / 2 < 8) {
		left = 8;
	} else {
		left -= tipWidth / 2;
	}
	if (top < 8) top = canvasRect.top + caretY + 12;

	el.style.left = `${left}px`;
	el.style.top = `${top}px`;
	el.style.opacity = "1";
}

export function tooltipRow(
	t: ChartTooltipStyle,
	dot: string,
	label: string,
	value: string
): string {
	return `<div style="display:flex;align-items:center;margin-bottom:4px;font-size:13px;font-family:${t.font};color:${t.body};">${dot}<span>${label}:&nbsp;<strong style="color:${t.title};font-family:${t.monoFont};">${value}</strong></span></div>`;
}

export function tooltipContainer(t: ChartTooltipStyle, title: string, body: string): string {
	return `<div style="background:${t.bg};border:1px solid ${t.border};border-radius:8px;padding:12px 14px;min-width:180px;box-shadow:0 4px 20px rgba(0,0,0,0.3);font-family:${t.font};"><div style="font-weight:700;font-size:13px;font-family:${t.font};color:${t.title};margin-bottom:8px;">${title}</div>${body}</div>`;
}
