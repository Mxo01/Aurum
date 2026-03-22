import {
	ChangeDetectionStrategy,
	Component,
	effect,
	input,
	model,
	output,
	signal
} from "@angular/core";
import {
	FormControl,
	FormGroup,
	FormsModule,
	ReactiveFormsModule,
	Validators
} from "@angular/forms";
import { Button } from "primeng/button";
import { InputText } from "primeng/inputtext";
import { Select } from "primeng/select";
import { Dialog } from "primeng/dialog";
import { ToggleButton } from "primeng/togglebutton";
import { AssetCategory, AssetType } from "../../model/asset.model";
import { typeOptions } from "../asset-form/asset-form.utils";
import { categoryIcons } from "./category-form.utils";
@Component({
	selector: "app-category-form",
	standalone: true,
	templateUrl: "./category-form.component.html",
	imports: [Button, InputText, Select, ReactiveFormsModule, FormsModule, Dialog, ToggleButton],
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
	protected readonly icons = signal(categoryIcons);
	protected readonly selectedIcon = signal<string | null>(null);
	protected readonly categoryForm = new FormGroup({
		name: new FormControl<string>("", Validators.required),
		type: new FormControl<AssetType | null>(null, Validators.required),
		icon: new FormControl<string | null>(null)
	});

	constructor() {
		effect(() => {
			const selectedCategory = this.selectedCategory();

			if (selectedCategory) {
				this.categoryForm.patchValue({
					name: selectedCategory.name,
					type: selectedCategory.type,
					icon: selectedCategory.icon
				});
				this.selectedIcon.set(selectedCategory.icon ?? null);
			} else {
				this.categoryForm.reset();
				this.selectedIcon.set(null);
			}
		});
	}

	onDialogHide() {
		this.categoryForm.reset();
		this.selectedIcon.set(null);
	}

	selectIcon(icon: string) {
		const next = this.selectedIcon() === icon ? null : icon;
		this.selectedIcon.set(next);
		this.categoryForm.controls.icon.setValue(next);
	}

	saveCategory() {
		const { name, type, icon } = this.categoryForm.value;
		if (!name || !type) return;
		this.save.emit({
			id: this.selectedCategory()?.id ?? "",
			name,
			type,
			icon: icon ?? null,
			isDefault: false
		});
	}

	deleteCategory(event: Event) {
		this.delete.emit({
			target: event.target as EventTarget,
			id: this.selectedCategory()?.id ?? ""
		});
	}
}
