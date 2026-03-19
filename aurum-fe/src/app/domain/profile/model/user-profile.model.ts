import { Currency } from "./currency.model";
import { Locale } from "./locale.model";

export interface UserProfile {
	id: string;
	email: string;
	currency: Currency;
	locale: Locale;
}
