import { Currency } from "./currency.model";

export interface UserProfile {
	id: string;
	email: string;
	currency: Currency;
}
