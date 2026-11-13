import type { AssetCategoryDTO, AssetDTO } from "../../../shared/api/contract";
import { Currency } from "../../profile/model/currency.model";

export type AssetType = AssetDTO["type"];
export const AssetType = {
	LIABILITY: "LIABILITY",
	ASSET: "ASSET"
} as const satisfies Record<AssetType, AssetType>;

export type LiabilityType = NonNullable<AssetDTO["liabilityType"]>;
export const LiabilityType = {
	MANUAL: "MANUAL",
	AUTOMATIC: "AUTOMATIC"
} as const satisfies Record<LiabilityType, LiabilityType>;

export type PaymentFrequency = NonNullable<AssetDTO["paymentFrequency"]>;
export const PaymentFrequency = {
	WEEKLY: "WEEKLY",
	MONTHLY: "MONTHLY",
	YEARLY: "YEARLY"
} as const satisfies Record<PaymentFrequency, PaymentFrequency>;

export type AssetCategory = AssetCategoryDTO;

/**
 * The client's view of an asset: the contract minus the fields the server
 * computes from snapshots (latest/previous value, snapshot list), plus the
 * `Date` the form works with instead of an ISO string.
 */
export type Asset = Omit<
	AssetDTO,
	| "snapshots"
	| "latestValue"
	| "latestValueBase"
	| "previousValue"
	| "referenceDate"
	| "originalCurrency"
> & {
	originalCurrency: Currency;
	latestValue?: number | null;
	latestValueBase?: number | null;
	previousValue?: number | null;
	referenceDate?: string | Date | null;
};
