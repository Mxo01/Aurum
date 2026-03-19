import { Currency } from "./model/currency.model";

export const currencyMap: Record<Currency, "€" | "$"> = {
	EUR: "€",
	USD: "$"
};

export const localeMap: Record<Currency, string> = {
	EUR: "de-DE",
	USD: "en-US"
};
