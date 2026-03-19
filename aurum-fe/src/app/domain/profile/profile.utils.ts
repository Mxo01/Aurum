import { Currency } from "./model/currency.model";
import { Locale } from "./model/locale.model";

export function getCurrencySymbol(currency: string): string {
	try {
		const formatted = new Intl.NumberFormat("en", {
			style: "currency",
			currency,
			minimumFractionDigits: 0,
			maximumFractionDigits: 0
		}).format(0);
		return formatted.replace(/[\d,.\s]/g, "").trim();
	} catch {
		return currency;
	}
}

export const currencyOptions = Object.values(Currency).map(code => ({
	label: `${code} (${getCurrencySymbol(code)})`,
	value: code
}));

export const localeOptions = [
	{ label: "English · 1,234.56", value: Locale.EN_US },
	{ label: "French · 1 234,56", value: Locale.FR },
	{ label: "Italian · 1.234,56", value: Locale.IT }
];
