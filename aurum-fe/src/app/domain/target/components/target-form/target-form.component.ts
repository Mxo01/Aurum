import { ChangeDetectionStrategy, Component, effect, input, model, output } from "@angular/core";
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from "@angular/forms";
import { InputText } from "primeng/inputtext";
import { InputNumber } from "primeng/inputnumber";
import { DatePicker } from "primeng/datepicker";
import { Button } from "primeng/button";
import { Dialog } from "primeng/dialog";
import { SelectButton } from "primeng/selectbutton";
import { Target, TargetType } from "../../model/target.model";
import { CommonModule } from "@angular/common";
import { Currency } from "../../../profile/model/currency.model";
import { Locale } from "../../../profile/model/locale.model";

@Component({
	selector: "app-target-form",
	standalone: true,
	templateUrl: "target-form.component.html",
	imports: [
		CommonModule,
		ReactiveFormsModule,
		InputText,
		InputNumber,
		DatePicker,
		Button,
		Dialog,
		SelectButton
	],
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class TargetFormComponent {
	isVisible = model.required<boolean>();
	selectedTarget = model<Target | null>(null);
	currency = input.required<Currency>();
	locale = input<Locale>(Locale.EN_US);

	save = output<Target>();

	protected readonly TargetType = TargetType;
	protected readonly typeOptions = [
		{ label: "Net Worth", value: TargetType.NET_WORTH },
		{ label: "Manual", value: TargetType.MANUAL }
	];

	protected readonly targetForm = new FormGroup({
		name: new FormControl<string>("", {
			validators: [Validators.required],
			nonNullable: true
		}),
		type: new FormControl<TargetType>(TargetType.MANUAL, {
			validators: [Validators.required],
			nonNullable: true
		}),
		targetAmount: new FormControl<number | null>(null, [Validators.required, Validators.min(0)]),
		currentAmount: new FormControl<number>(0, {
			validators: [Validators.min(0)],
			nonNullable: true
		}),
		deadline: new FormControl<Date>(new Date(), {
			validators: [Validators.required],
			nonNullable: true
		})
	});

	constructor() {
		effect(() => {
			const target = this.selectedTarget();

			if (target) {
				this.targetForm.patchValue({
					name: target.name,
					type: target.type,
					targetAmount: target.targetAmount,
					currentAmount: target.currentAmount || 0,
					deadline: new Date(target.deadline)
				});
				this.targetForm.get("type")?.disable();
			} else {
				this.targetForm.reset({
					name: "",
					type: TargetType.MANUAL,
					targetAmount: null,
					currentAmount: 0,
					deadline: new Date()
				});
				this.targetForm.get("type")?.enable();
			}
		});
	}

	protected onHide() {
		this.selectedTarget.set(null);
		this.targetForm.reset({
			name: "",
			type: TargetType.MANUAL,
			targetAmount: null,
			currentAmount: 0,
			deadline: new Date()
		});
		this.targetForm.get("type")?.enable();
	}

	protected onSubmit() {
		if (this.targetForm.invalid) return;

		const formValue = this.targetForm.getRawValue();
		const target: Target = {
			id: this.selectedTarget()?.id,
			name: formValue.name,
			type: formValue.type,
			targetAmount: formValue.targetAmount!,
			currentAmount: formValue.type === TargetType.MANUAL ? formValue.currentAmount : undefined,
			deadline: formValue.deadline.toISOString().split("T")[0]
		};

		this.save.emit(target);
		this.isVisible.set(false);
		this.selectedTarget.set(null);
	}
}
