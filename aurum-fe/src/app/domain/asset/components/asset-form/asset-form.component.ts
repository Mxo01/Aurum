import { ToggleButton } from "primeng/togglebutton";
import {
	ChangeDetectionStrategy,
	Component,
	effect,
	input,
	model,
	OnInit,
	output,
	signal,
	OnDestroy
} from "@angular/core";
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from "@angular/forms";
import { InputText } from "primeng/inputtext";
import { Select } from "primeng/select";
import { SelectButton } from "primeng/selectbutton";
import { InputNumber } from "primeng/inputnumber";
import { DatePicker } from "primeng/datepicker";
import { currencyOptions, typeOptions } from "./asset-form.utils";
import { Button } from "primeng/button";
import { Drawer } from "primeng/drawer";
import { Subscription } from "rxjs";
import { Currency } from "../../../profile/model/currency.model";
import { Asset, AssetCategory, AssetType } from "../../model/asset.model";

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
		DatePicker,
		Button,
		Drawer
	],
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class AssetFormComponent implements OnInit, OnDestroy {
	isVisible = model.required<boolean>();
	selectedAsset = model<Asset | null>(null);

	categoriesOptions = input.required<AssetCategory[]>();

	save = output<Asset>();

	protected readonly assetForm = new FormGroup({
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
	protected readonly AssetType = AssetType;

	private categoryCtrlSub?: Subscription;
	private isActiveCtrlSub?: Subscription;

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
				this.assetForm.controls.currency.disable();
			} else {
				this.assetForm.controls.currency.enable();
				this.resetForm();
			}
		});
	}

	ngOnInit() {
		this.categoryCtrlSub = this.assetForm.controls.category.valueChanges.subscribe({
			next: category => {
				if (category) this.assetForm.controls.type.setValue(category.type);
			}
		});
		this.isActiveCtrlSub = this.assetForm.controls.isActive.valueChanges.subscribe({
			next: isActive => {
				if (!isActive) {
					this.assetForm.controls.isFavorite.setValue(false);
				}
			}
		});
	}

	ngOnDestroy() {
		this.categoryCtrlSub?.unsubscribe();
		this.isActiveCtrlSub?.unsubscribe();
	}

	resetForm() {
		this.assetForm.reset({
			currency: Currency.EUR,
			isActive: true,
			referenceDate: new Date()
		});
	}

	hideDrawer() {
		this.selectedAsset.set(null);
		this.resetForm();
	}

	saveAssetFromForm() {
		const form = this.assetForm.getRawValue();

		if (!form || !form.category || !form.type || !form.currency || !form.name) return;

		const isNewAsset = !this.selectedAsset();

		const asset: Asset = {
			id: this.selectedAsset()?.id ?? "",
			name: form.name,
			categoryId: form.category.id,
			categoryName: form.category.name,
			type: form.type,
			originalCurrency: form.currency,
			isActive: !!form.isActive,
			isFavorite: !!form.isFavorite,
			initialValue: isNewAsset ? form.initialValue : null,
			referenceDate:
				isNewAsset && form.referenceDate
					? form.referenceDate.getFullYear() +
						"-" +
						String(form.referenceDate.getMonth() + 1).padStart(2, "0") +
						"-" +
						String(form.referenceDate.getDate()).padStart(2, "0")
					: null
		};

		this.hideDrawer();
		this.isVisible.set(false);
		this.save.emit(asset);
	}
}
