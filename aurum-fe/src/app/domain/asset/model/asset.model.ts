import { Currency } from "../../profile/model/currency.model";
import { Snapshot } from "../../snapshot/model/snapshot.model";

export enum AssetType {
	LIABILITY = "LIABILITY",
	ASSET = "ASSET"
}

export interface AssetCategory {
	id: string;
	name: string;
	type: AssetType;
}

export interface Asset {
	id: string;
	name: string;
	categoryId: string;
	categoryName: string;
	type: AssetType;
	originalCurrency: Currency;
	isActive: boolean;
	isFavorite: boolean;
	snapshots?: Snapshot[];
	initialValue?: number | null;
	referenceDate?: string | Date | null;
}
