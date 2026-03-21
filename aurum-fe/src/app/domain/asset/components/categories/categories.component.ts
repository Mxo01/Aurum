import {
	ChangeDetectionStrategy,
	Component,
	computed,
	inject,
	OnInit,
	signal
} from "@angular/core";
import { Button } from "primeng/button";
import { RouterLink } from "@angular/router";
import { ConfirmationService } from "primeng/api";
import { finalize } from "rxjs";
import { AssetService } from "../../asset.service";
import { AssetCategory } from "../../model/asset.model";
import { CategoryFormComponent } from "../category-form/category-form.component";
import { NavigationService } from "../../../../shared/services/navigation/navigation.service";
import { paths } from "../../../../app.routes";
import { CategoryItemComponent } from "./category-item/category-item.component";

@Component({
	selector: "app-categories",
	standalone: true,
	templateUrl: "./categories.component.html",
	imports: [Button, RouterLink, CategoryFormComponent, CategoryItemComponent],
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class CategoriesComponent implements OnInit {
	private readonly assetService = inject(AssetService);
	private readonly confirmationService = inject(ConfirmationService);
	private readonly navigationService = inject(NavigationService);

	protected readonly paths = paths;
	protected readonly previousRoute = this.navigationService.previousRoute;
	protected readonly categories = signal<AssetCategory[]>([]);
	protected readonly defaultCategories = computed(() => this.categories().filter(c => c.isDefault));
	protected readonly customCategories = computed(() => this.categories().filter(c => !c.isDefault));
	protected readonly isDialogVisible = signal(false);
	protected readonly selectedCategory = signal<AssetCategory | null>(null);
	protected readonly isSaveLoading = signal(false);
	protected readonly isDeleteLoading = signal(false);

	ngOnInit() {
		this.assetService.getAssetCategories().subscribe({
			next: categories => this.categories.set(categories)
		});
	}

	protected openAdd() {
		this.selectedCategory.set(null);
		this.isDialogVisible.set(true);
	}

	protected openEdit(category: AssetCategory) {
		this.selectedCategory.set({ ...category });
		this.isDialogVisible.set(true);
	}

	protected saveCategory(category: AssetCategory) {
		this.isSaveLoading.set(true);

		this.assetService
			.saveCategory(category)
			.pipe(finalize(() => this.isSaveLoading.set(false)))
			.subscribe({
				next: ({ categories }) => {
					this.categories.set(categories);
					this.isDialogVisible.set(false);
				}
			});
	}

	protected deleteCategory(event: { target: EventTarget; id: string }) {
		const { id, target } = event;
		this.confirmationService.confirm({
			target,
			message:
				"Are you sure you want to delete this category? All assets in this category will be deleted. This action is irreversible.",
			header: "Danger Zone",
			icon: "pi pi-info-circle",
			rejectVisible: false,
			acceptButtonProps: {
				size: "small",
				label: "Delete",
				severity: "danger"
			},
			accept: () => {
				if (!id) return;

				this.isDeleteLoading.set(true);

				this.assetService
					.deleteCategory(id)
					.pipe(finalize(() => this.isDeleteLoading.set(false)))
					.subscribe({
						next: ({ categories }) => {
							this.categories.set(categories);
							this.isDialogVisible.set(false);
						}
					});
			}
		});
	}
}
