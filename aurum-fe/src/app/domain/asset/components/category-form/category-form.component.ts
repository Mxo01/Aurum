import {
	ChangeDetectionStrategy,
	Component,
	effect,
	input,
	model,
	output,
	signal
} from "@angular/core";
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from "@angular/forms";
import { Button } from "primeng/button";
import { InputText } from "primeng/inputtext";
import { Select } from "primeng/select";
import { Dialog } from "primeng/dialog";
import { AssetCategory, AssetType } from "../../model/asset.model";
import { typeOptions } from "../asset-form/asset-form.utils";

@Component({
	selector: "app-category-form",
	standalone: true,
	templateUrl: "./category-form.component.html",
	imports: [Button, InputText, Select, ReactiveFormsModule, Dialog],
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class CategoryFormComponent {
	isVisible = model.required<boolean>();
	selectedCategory = input<AssetCategory | null>(null);
	isSaveLoading = input<boolean>(false);
	isDeleteLoading = input<boolean>(false);

	save = output<AssetCategory>();
	delete = output<{ target: EventTarget; id: string }>();

	protected readonly typeOptions = signal(typeOptions);
	protected readonly categoryForm = new FormGroup({
		name: new FormControl<string>("", Validators.required),
		type: new FormControl<AssetType | null>(null, Validators.required)
	});

	constructor() {
		effect(() => {
			const selectedCategory = this.selectedCategory();

			if (selectedCategory) {
				this.categoryForm.patchValue({ name: selectedCategory.name, type: selectedCategory.type });
			} else {
				this.categoryForm.reset();
			}
		});
	}

	onDialogHide() {
		this.categoryForm.reset();
	}

	saveCategory() {
		const { name, type } = this.categoryForm.value;
		if (!name || !type) return;
		this.save.emit({ id: this.selectedCategory()?.id ?? "", name, type });
	}

	deleteCategory(event: Event) {
		this.delete.emit({
			target: event.target as EventTarget,
			id: this.selectedCategory()?.id ?? ""
		});
	}
}
