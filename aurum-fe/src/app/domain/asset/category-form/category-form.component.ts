import { ChangeDetectionStrategy, Component, input, model, output, signal } from "@angular/core";
import {
	FormControl,
	FormGroup,
	FormsModule,
	ReactiveFormsModule,
	Validators
} from "@angular/forms";
import { BlockUIModule } from "primeng/blockui";
import { Button } from "primeng/button";
import { InputText } from "primeng/inputtext";
import { Listbox, ListboxChangeEvent } from "primeng/listbox";
import { Panel } from "primeng/panel";
import { Select } from "primeng/select";
import { TableModule } from "primeng/table";
import { ToggleSwitch } from "primeng/toggleswitch";
import { AssetCategory, AssetType } from "../model/asset.model";
import { typeOptions } from "../asset-form/asset-form.utils";

@Component({
	selector: "app-category-form",
	standalone: true,
	templateUrl: "./category-form.component.html",
	imports: [
		Button,
		BlockUIModule,
		TableModule,
		ToggleSwitch,
		FormsModule,
		Listbox,
		InputText,
		Select,
		Panel,
		ReactiveFormsModule
	],
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class CategoryFormComponent {
	isNewCategory = model.required<boolean>();

	categoriesOptions = input.required<AssetCategory[]>();
	isSaveLoading = input.required<boolean>();
	isDeleteLoading = input.required<boolean>();

	save = output<AssetCategory>();
	delete = output<{ target: EventTarget; id: string }>();

	protected readonly typeOptions = signal(typeOptions);
	protected readonly selectedCategory = signal<AssetCategory | null>(null);
	protected readonly categoryForm = new FormGroup({
		name: new FormControl<string>("", Validators.required),
		type: new FormControl<AssetType | null>(null, Validators.required)
	});

	selectCategory(event: ListboxChangeEvent) {
		this.isNewCategory.set(false);
		this.selectedCategory.set(event.value);
		this.categoryForm.patchValue({
			name: this.selectedCategory()?.name,
			type: this.selectedCategory()?.type
		});
	}

	addNewCategory() {
		this.selectedCategory.set(null);
		this.categoryForm.reset();
	}

	onCategoriesDialogHide() {
		this.selectedCategory.set(null);
		this.categoryForm.reset();
	}

	onNewCategoryToggleChange(isNewCategory: boolean) {
		if (isNewCategory) this.selectedCategory.set(null);
		else this.categoryForm.reset();
	}

	saveCategory() {
		const form = this.categoryForm.value;

		if (!form || !form.name || !form.type) return;

		const category: AssetCategory = {
			id: this.selectedCategory()?.id ?? "",
			name: form.name,
			type: form.type
		};

		this.save.emit(category);
	}

	deleteCategory(event: Event) {
		this.delete.emit({
			target: event.target as EventTarget,
			id: this.selectedCategory()?.id ?? ""
		});
		this.selectedCategory.set(null);
		this.categoryForm.reset();
	}
}
