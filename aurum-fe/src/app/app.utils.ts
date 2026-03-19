import Aura from "@primeuix/themes/aura";
import { definePreset } from "@primeuix/themes";
import { AuthConfig } from "@auth0/auth0-angular";

export const primengPreset = definePreset(Aura, {
	semantic: {
		primary: {
			50: "{zinc.50}",
			100: "{zinc.100}",
			200: "{zinc.200}",
			300: "{zinc.300}",
			400: "{zinc.400}",
			500: "{zinc.500}",
			600: "{zinc.600}",
			700: "{zinc.700}",
			800: "{zinc.800}",
			900: "{zinc.900}",
			950: "{zinc.950}"
		},
		colorScheme: {
			light: {
				primary: {
					color: "{zinc.950}",
					contrastColor: "#ffffff",
					hoverColor: "{zinc.800}",
					activeColor: "{zinc.700}"
				},
				highlight: {
					background: "{zinc.950}",
					focusBackground: "{zinc.800}",
					color: "#ffffff",
					focusColor: "#ffffff"
				}
			},
			dark: {
				primary: {
					color: "{zinc.50}",
					contrastColor: "{zinc.950}",
					hoverColor: "{zinc.200}",
					activeColor: "{zinc.300}"
				},
				highlight: {
					background: "{zinc.50}",
					focusBackground: "{zinc.200}",
					color: "{zinc.950}",
					focusColor: "{zinc.950}"
				}
			}
		}
	}
});

export const auth0Config: AuthConfig = {
	domain: "aurum-api.eu.auth0.com",
	clientId: "ZHikUaUM2UOqcOHtTzdPephPVMWifRPj",
	authorizationParams: {
		redirect_uri: window.location.origin,
		audience: "https://api.aurum.com"
	},
	httpInterceptor: {
		allowedList: [
			{
				uri: "http://localhost:8080/api/*",
				tokenOptions: {
					authorizationParams: {
						audience: "https://api.aurum.com"
					}
				}
			}
		]
	}
};

export const darkModeSelector = "p-dark";

export const chartColors = ["#1D2D44", "#597491", "#748CAB", "#99aec9", "#c0d5f0"];

export const chartTooltipStyle = {
	bg: "#18181B",
	title: "#ffffff",
	body: "#d1d5db",
	border: "rgba(255,255,255,0.1)",
	muted: "#9ca3af",
	font: "inherit",
	monoFont: "ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace"
} as const;

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

export function tooltipRow(dot: string, label: string, value: string): string {
	const t = chartTooltipStyle;
	return `<div style="display:flex;align-items:center;margin-bottom:4px;font-size:13px;font-family:${t.font};color:${t.body};">${dot}<span>${label}:&nbsp;<strong style="color:${t.title};font-family:${t.monoFont};">${value}</strong></span></div>`;
}

export function tooltipContainer(title: string, body: string): string {
	const t = chartTooltipStyle;
	return `<div style="background:${t.bg};border:1px solid ${t.border};border-radius:8px;padding:12px 14px;min-width:180px;box-shadow:0 4px 20px rgba(0,0,0,0.3);font-family:${t.font};"><div style="font-weight:700;font-size:13px;font-family:${t.font};color:${t.title};margin-bottom:8px;">${title}</div>${body}</div>`;
}
