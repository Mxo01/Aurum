import { SelectItem } from "primeng/api";
import { AssetType } from "../../model/asset.model";
import { currencyOptions as allCurrencyOptions } from "../../../profile/profile.utils";

export const typeOptions: SelectItem<AssetType>[] = [
	{ label: "Asset", value: AssetType.ASSET },
	{ label: "Liability", value: AssetType.LIABILITY }
];

export const currencyOptions = allCurrencyOptions;
