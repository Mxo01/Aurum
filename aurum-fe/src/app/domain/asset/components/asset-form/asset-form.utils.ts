import { SelectItem } from "primeng/api";
import { AssetType, LiabilityType, PaymentFrequency } from "../../model/asset.model";
import { currencyOptions as allCurrencyOptions } from "../../../profile/profile.utils";

export const typeOptions: SelectItem<AssetType>[] = [
	{ label: "Asset", value: AssetType.ASSET },
	{ label: "Liability", value: AssetType.LIABILITY }
];

export const liabilityTypeOptions: SelectItem<LiabilityType>[] = [
	{ label: "Manual", value: LiabilityType.MANUAL },
	{ label: "Automatic", value: LiabilityType.AUTOMATIC }
];

export const paymentFrequencyOptions: SelectItem<PaymentFrequency>[] = [
	{ label: "Weekly", value: PaymentFrequency.WEEKLY },
	{ label: "Monthly", value: PaymentFrequency.MONTHLY },
	{ label: "Yearly", value: PaymentFrequency.YEARLY }
];

export const currencyOptions = allCurrencyOptions;
