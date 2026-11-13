import type { AssetDTO } from "../../../shared/api/contract";

type ContractCurrency = AssetDTO["originalCurrency"];

/**
 * Compile-time guard: a currency added to the backend contract breaks the build
 * here until the enum below is updated, instead of failing at runtime.
 */
export type ContractCurrencyCoverage = ContractCurrency extends Currency ? true : never;

export enum Currency {
	AUD = "AUD",
	BGN = "BGN",
	BRL = "BRL",
	CAD = "CAD",
	CHF = "CHF",
	CNY = "CNY",
	CZK = "CZK",
	DKK = "DKK",
	EUR = "EUR",
	GBP = "GBP",
	HKD = "HKD",
	HUF = "HUF",
	IDR = "IDR",
	ILS = "ILS",
	INR = "INR",
	ISK = "ISK",
	JPY = "JPY",
	KRW = "KRW",
	MXN = "MXN",
	MYR = "MYR",
	NOK = "NOK",
	NZD = "NZD",
	PHP = "PHP",
	PLN = "PLN",
	RON = "RON",
	SEK = "SEK",
	SGD = "SGD",
	THB = "THB",
	TRY = "TRY",
	USD = "USD",
	ZAR = "ZAR"
}
