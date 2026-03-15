import { SelectItem } from "primeng/api";
import { Currency } from "../../../profile/model/currency.model";
import { AssetType } from "../../model/asset.model";

export const typeOptions: SelectItem<AssetType>[] = [
	{ label: "Asset", value: AssetType.ASSET },
	{ label: "Liability", value: AssetType.LIABILITY }
];

export const currencyOptions: SelectItem<Currency>[] = [
	{ label: "EUR", value: Currency.EUR, icon: "pi pi-euro" },
	{ label: "USD", value: Currency.USD, icon: "pi pi-dollar" }
];
