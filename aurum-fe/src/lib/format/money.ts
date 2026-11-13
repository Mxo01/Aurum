export const PRIVACY_MASK = "••••••";

export interface MoneyOptions {
	currency: string;
	locale: string;
	privacy?: boolean;
	digits?: number;
	signDisplay?: "auto" | "never" | "always" | "exceptZero";
	compact?: boolean;
}

export function formatCurrency(value: number, options: MoneyOptions) {
	if (options.privacy) return PRIVACY_MASK;

	const digits = options.digits ?? 2;
	const formatter = new Intl.NumberFormat(options.locale, {
		style: "currency",
		currency: options.currency,
		minimumFractionDigits: options.compact ? 0 : digits,
		maximumFractionDigits: options.compact ? 1 : digits,
		notation: options.compact ? "compact" : "standard",
		signDisplay: options.signDisplay ?? "auto"
	});

	return formatter.format(value);
}

export function formatNumber(value: number, locale: string, digits = 2) {
	return new Intl.NumberFormat(locale, {
		minimumFractionDigits: digits,
		maximumFractionDigits: digits
	}).format(value);
}

export function formatPercent(value: number, locale: string, digits = 2) {
	const formatted = new Intl.NumberFormat(locale, {
		minimumFractionDigits: digits,
		maximumFractionDigits: digits
	}).format(Math.abs(value));
	return `${formatted}%`;
}

export function getCurrencySymbol(currency: string, locale = "en") {
	try {
		return new Intl.NumberFormat(locale, {
			style: "currency",
			currency,
			minimumFractionDigits: 0,
			maximumFractionDigits: 0
		})
			.format(0)
			.replace(/[\d,.\s]/g, "")
			.trim();
	} catch {
		return currency;
	}
}

export function toAmount(value: number | null | undefined) {
	return typeof value === "number" && Number.isFinite(value) ? value : 0;
}

export function safeDivide(value: number, by: number) {
	return by === 0 ? null : value / by;
}
