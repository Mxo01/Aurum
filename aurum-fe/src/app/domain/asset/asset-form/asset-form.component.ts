import { ToggleButton } from "primeng/togglebutton";
import { ChangeDetectionStrategy, Component, effect, input, OnInit, signal } from "@angular/core";
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from "@angular/forms";
import { Asset, AssetCategory, AssetType } from "../model/asset.model";
import { InputText } from "primeng/inputtext";
import { Select } from "primeng/select";
import { SelectButton } from "primeng/selectbutton";
import { InputNumber } from "primeng/inputnumber";
import { DatePicker } from "primeng/datepicker";
import { currencyOptions, typeOptions } from "./asset-form.utils";
import { Currency } from "../../profile/model/currency.model";

@Component({
	selector: "app-asset-form",
	standalone: true,
	templateUrl: "./asset-form.component.html",
	imports: [
		ReactiveFormsModule,
		InputText,
		ToggleButton,
		Select,
		SelectButton,
		InputNumber,
		DatePicker
	],
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class AssetFormComponent implements OnInit {
	selectedAsset = input<Asset | null>(null);
	categoriesOptions = input.required<AssetCategory[]>();

	readonly assetForm = new FormGroup({
		name: new FormControl<string>("", Validators.required),
		category: new FormControl<AssetCategory | null>(null, Validators.required),
		type: new FormControl<AssetType | null>({ value: null, disabled: true }, Validators.required),
		currency: new FormControl<Currency>(Currency.EUR, Validators.required),
		isActive: new FormControl<boolean>(true),
		isFavorite: new FormControl<boolean>(false),
		initialValue: new FormControl<number | null>(null),
		referenceDate: new FormControl<Date>(new Date())
	});
	protected readonly currencyOptions = signal(currencyOptions);
	protected readonly typeOptions = signal(typeOptions);

	constructor() {
		effect(() => {
			const selectedAsset = this.selectedAsset();

			if (selectedAsset) {
				this.assetForm.patchValue({
					name: selectedAsset.name,
					category: {
						id: selectedAsset.categoryId,
						name: selectedAsset.categoryName,
						type: selectedAsset.type
					},
					type: selectedAsset.type,
					currency: selectedAsset.originalCurrency,
					isActive: selectedAsset.isActive,
					isFavorite: selectedAsset.isFavorite
				});
			}
		});
	}

	ngOnInit() {
		this.assetForm.controls.category.valueChanges.subscribe({
			next: category => {
				if (category) this.assetForm.controls.type.setValue(category.type);
			}
		});
	}
}
