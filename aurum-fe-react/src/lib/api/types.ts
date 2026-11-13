import type { components } from "./schema";

export type Schemas = components["schemas"];

export type AssetDTO = Schemas["AssetDTO"];
export type AssetCategoryDTO = Schemas["AssetCategoryDTO"];
export type SnapshotDTO = Schemas["SnapshotDTO"];
export type TargetDTO = Schemas["TargetDTO"];
export type AnalyticsSummaryDTO = Schemas["AnalyticsSummaryDTO"];
export type DeltaDTO = Schemas["DeltaDTO"];
export type VariationDTO = Schemas["VariationDTO"];
export type ChartDataDTO = Schemas["ChartDataDTO"];
export type CashFlowEntryDTO = Schemas["CashFlowEntryDTO"];
export type CashFlowYearDTO = Schemas["CashFlowYearDTO"];
export type UpdateCashFlowEntryDTO = Schemas["UpdateCashFlowEntryDTO"];
export type UserDTO = Schemas["User"];
export type CreateAssetDTO = Schemas["CreateAssetDTO"];
export type UpdateAssetDTO = Schemas["UpdateAssetDTO"];
export type CreateSnapshotDTO = Schemas["CreateSnapshotDTO"];
export type CreateTargetDTO = Schemas["CreateTargetDTO"];
export type UpdateTargetDTO = Schemas["UpdateTargetDTO"];
export type CreateAssetCategoryDTO = Schemas["CreateAssetCategoryDTO"];
export type UpdateAssetCategoryDTO = Schemas["UpdateAssetCategoryDTO"];

export type AssetType = AssetDTO["type"];
export type LiabilityType = NonNullable<AssetDTO["liabilityType"]>;
export type PaymentFrequency = NonNullable<AssetDTO["paymentFrequency"]>;
export type Currency = AssetDTO["originalCurrency"];
export type TargetType = TargetDTO["type"];
export type TargetStatus = NonNullable<TargetDTO["status"]>;

export const ASSET_TYPE = { ASSET: "ASSET", LIABILITY: "LIABILITY" } as const;
export const LIABILITY_TYPE = { MANUAL: "MANUAL", AUTOMATIC: "AUTOMATIC" } as const;
export const PAYMENT_FREQUENCY = {
	WEEKLY: "WEEKLY",
	MONTHLY: "MONTHLY",
	YEARLY: "YEARLY"
} as const;
export const TARGET_TYPE = { NET_WORTH: "NET_WORTH", MANUAL: "MANUAL" } as const;
export const TARGET_STATUS = {
	COMPLETED: "COMPLETED",
	IN_PROGRESS: "IN_PROGRESS",
	NOT_REACHED: "NOT_REACHED"
} as const;

export const CURRENCIES: Currency[] = [
	"AUD",
	"BGN",
	"BRL",
	"CAD",
	"CHF",
	"CNY",
	"CZK",
	"DKK",
	"EUR",
	"GBP",
	"HKD",
	"HUF",
	"IDR",
	"ILS",
	"INR",
	"ISK",
	"JPY",
	"KRW",
	"MXN",
	"MYR",
	"NOK",
	"NZD",
	"PHP",
	"PLN",
	"RON",
	"SEK",
	"SGD",
	"THB",
	"TRY",
	"USD",
	"ZAR"
];

export type Locale = "en-US" | "fr-FR" | "it-IT";
export const LOCALES: { label: string; value: Locale }[] = [
	{ label: "English · 1,234.56", value: "en-US" },
	{ label: "French · 1 234,56", value: "fr-FR" },
	{ label: "Italian · 1.234,56", value: "it-IT" }
];
