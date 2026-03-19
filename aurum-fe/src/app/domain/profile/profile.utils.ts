import { Currency } from "./model/currency.model";
import { Locale } from "./model/locale.model";

export const currencyMap: Record<Currency, "€" | "$"> = {
	EUR: "€",
	USD: "$"
};

export const localeOptions = [
	{ label: "English · 1,234.56", value: Locale.EN_US },
	{ label: "French · 1 234,56", value: Locale.FR },
	{ label: "Italian · 1.234,56", value: Locale.IT }
];
