import { Currency } from "../../profile/model/currency.model";
import { Snapshot } from "../../snapshot/model/snapshot.model";

export enum AssetType {
	LIABILITY = "LIABILITY",
	ASSET = "ASSET"
}

export enum LiabilityType {
	MANUAL = "MANUAL",
	AUTOMATIC = "AUTOMATIC"
}

export enum PaymentFrequency {
	WEEKLY = "WEEKLY",
	MONTHLY = "MONTHLY",
	YEARLY = "YEARLY"
}

export interface AssetCategory {
	id: string;
	name: string;
	type: AssetType;
	icon: string | null;
	isDefault: boolean;
}

export interface Asset {
	id: string;
	name: string;
	categoryId: string;
	categoryName: string;
	categoryIcon: string | null;
	type: AssetType;
	originalCurrency: Currency;
	isActive: boolean;
	isFavorite: boolean;
	snapshots?: Snapshot[];
	initialValue?: number | null;
	referenceDate?: string | Date | null;
	liabilityType?: LiabilityType | null;
	paymentFrequency?: PaymentFrequency | null;
	paymentAmount?: number | null;
}
