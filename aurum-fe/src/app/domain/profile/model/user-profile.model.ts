import type { Schemas } from "../../../shared/api/contract";
import { Currency } from "./currency.model";
import { Locale } from "./locale.model";

/**
 * What `GET /users` returns: the account as the backend stores it. `email` is
 * deliberately absent — it is a claim in the Auth0 token, never a column.
 */
export type UserAccount = Required<Pick<Schemas["User"], "id">> &
	Pick<Schemas["User"], "picture"> & {
		currency: Currency;
		locale: Locale;
	};

/** Frontend view model: the account plus the email read from the token. */
export type UserProfile = UserAccount & { email: string };
