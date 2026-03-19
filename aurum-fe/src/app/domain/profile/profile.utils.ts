import { Currency } from "./model/currency.model";
import { Locale } from "./model/locale.model";

export const currencyMap: Record<Currency, "€" | "$"> = {
	EUR: "€",
	USD: "$"
};

export const localeOptions = [
	{ label: "English (US) · 1,234.56", value: Locale.EN_US },
	{ label: "Chinese · 1,234.56", value: Locale.ZH_CN },
	{ label: "English (UK) · 1,234.56", value: Locale.EN_GB },
	{ label: "French · 1 234,56", value: Locale.FR },
	{ label: "German · 1.234,56", value: Locale.DE },
	{ label: "Italian · 1.234,56", value: Locale.IT }
];
