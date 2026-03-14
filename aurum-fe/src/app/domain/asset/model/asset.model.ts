import { Currency } from "../../profile/model/currency.model";

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
}
