import { ToggleButton } from "primeng/togglebutton";
import {
	ChangeDetectionStrategy,
	Component,
	DestroyRef,
	effect,
	inject,
	input,
	model,
	OnInit,
	output,
	signal
} from "@angular/core";
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from "@angular/forms";
import { InputText } from "primeng/inputtext";
import { Select } from "primeng/select";
import { InputNumber } from "primeng/inputnumber";
import { DatePicker } from "primeng/datepicker";
import {
	currencyOptions,
	typeOptions,
	liabilityTypeOptions,
	paymentFrequencyOptions
} from "./asset-form.utils";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { Button } from "primeng/button";
import { Drawer } from "primeng/drawer";
import { formatDateToISO } from "../../../../shared/utils";
import { Currency } from "../../../profile/model/currency.model";
import { Locale } from "../../../profile/model/locale.model";
import {
	Asset,
	AssetCategory,
	AssetType,
	LiabilityType,
	PaymentFrequency
} from "../../model/asset.model";

@Component({
	selector: "app-asset-form",
	standalone: true,
	templateUrl: "./asset-form.component.html",
	imports: [
		ReactiveFormsModule,
		InputText,
		ToggleButton,
		Select,
		InputNumber,
		DatePicker,
		Button,
		Drawer
	],
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class AssetFormComponent implements OnInit {
	isVisible = model.required<boolean>();
	selectedAsset = model<Asset | null>(null);

	categoriesOptions = input.required<AssetCategory[]>();
	locale = input<Locale>(Locale.EN_US);

	save = output<Asset>();

	readonly assetForm = new FormGroup({
		name: new FormControl<string>("", [Validators.required, Validators.maxLength(100)]),
		category: new FormControl<AssetCategory | null>(null, Validators.required),
		type: new FormControl<AssetType | null>({ value: null, disabled: true }, Validators.required),
		currency: new FormControl<Currency>(Currency.EUR, Validators.required),
		isFavorite: new FormControl<boolean>(false),
		initialValue: new FormControl<number | null>(null),
		referenceDate: new FormControl<Date>(new Date()),
		liabilityType: new FormControl<LiabilityType | null>(null),
		paymentFrequency: new FormControl<PaymentFrequency | null>(null),
		paymentAmount: new FormControl<number | null>(null)
	});
	readonly currencyOptions = signal(currencyOptions);
	readonly typeOptions = signal(typeOptions);
	readonly liabilityTypeOptions = signal(liabilityTypeOptions);
	readonly paymentFrequencyOptions = signal(paymentFrequencyOptions);
	readonly AssetType = AssetType;
	readonly LiabilityType = LiabilityType;

	private readonly destroyRef = inject(DestroyRef);

	constructor() {
		effect(() => {
			const selectedAsset = this.selectedAsset();

			if (selectedAsset) {
				this.assetForm.patchValue({
					name: selectedAsset.name,
					category: this.categoriesOptions().find(c => c.id === selectedAsset.categoryId) ?? {
						id: selectedAsset.categoryId,
						name: selectedAsset.categoryName,
						type: selectedAsset.type,
						icon: null,
						isDefault: false
					},
					type: selectedAsset.type,
					currency: selectedAsset.originalCurrency,
					isFavorite: selectedAsset.isFavorite,
					liabilityType: selectedAsset.liabilityType ?? null,
					paymentFrequency: selectedAsset.paymentFrequency ?? null,
					paymentAmount: selectedAsset.paymentAmount ?? null
				});
				this.assetForm.controls.currency.disable();
				this.assetForm.controls.isFavorite.enable();
				this.updateLiabilityValidators(selectedAsset.liabilityType ?? null);
			} else {
				this.assetForm.controls.currency.enable();
				this.assetForm.controls.isFavorite.enable();
				this.resetForm();
			}
		});
	}

	ngOnInit() {
		this.assetForm.controls.category.valueChanges
			.pipe(takeUntilDestroyed(this.destroyRef))
			.subscribe({
				next: category => {
					if (category) this.assetForm.controls.type.setValue(category.type);
				}
			});

		this.assetForm.controls.liabilityType.valueChanges
			.pipe(takeUntilDestroyed(this.destroyRef))
			.subscribe({
				next: liabilityType => {
					this.updateLiabilityValidators(liabilityType);
				}
			});
	}

	private updateLiabilityValidators(liabilityType: LiabilityType | null) {
		const frequencyCtrl = this.assetForm.controls.paymentFrequency;
		const amountCtrl = this.assetForm.controls.paymentAmount;

		if (liabilityType === LiabilityType.AUTOMATIC) {
			frequencyCtrl.setValidators(Validators.required);
			amountCtrl.setValidators([Validators.required, Validators.min(0.01)]);
		} else {
			frequencyCtrl.clearValidators();
			amountCtrl.clearValidators();
			frequencyCtrl.setValue(null);
			amountCtrl.setValue(null);
		}

		frequencyCtrl.updateValueAndValidity();
		amountCtrl.updateValueAndValidity();
	}

	resetForm() {
		this.assetForm.reset({
			currency: Currency.EUR,
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
			categoryIcon: form.category.icon ?? null,
			type: form.type,
			originalCurrency: form.currency,
			isFavorite: !!form.isFavorite,
			initialValue: isNewAsset ? form.initialValue : null,
			referenceDate: isNewAsset && form.referenceDate ? formatDateToISO(form.referenceDate) : null,
			liabilityType: form.type === AssetType.LIABILITY ? (form.liabilityType ?? null) : null,
			paymentFrequency:
				form.type === AssetType.LIABILITY && form.liabilityType === LiabilityType.AUTOMATIC
					? (form.paymentFrequency ?? null)
					: null,
			paymentAmount:
				form.type === AssetType.LIABILITY && form.liabilityType === LiabilityType.AUTOMATIC
					? (form.paymentAmount ?? null)
					: null
		};

		this.hideDrawer();
		this.isVisible.set(false);
		this.save.emit(asset);
	}
}
