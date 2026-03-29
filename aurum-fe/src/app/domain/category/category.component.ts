import {
	ChangeDetectionStrategy,
	Component,
	computed,
	DestroyRef,
	inject,
	OnInit,
	signal
} from "@angular/core";
import { Button } from "primeng/button";
import { RouterLink } from "@angular/router";
import { ConfirmationService } from "primeng/api";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { finalize } from "rxjs";
import { CategoryFormComponent } from "./category-form/category-form.component";
import { CategoryItemComponent } from "./category-item/category-item.component";
import { paths } from "../../app.routes";
import { NavigationService } from "../../shared/services/navigation/navigation.service";
import { AssetService } from "../asset/asset.service";
import { AssetCategory } from "../asset/model/asset.model";

@Component({
	selector: "app-category",
	standalone: true,
	templateUrl: "./category.component.html",
	imports: [Button, RouterLink, CategoryFormComponent, CategoryItemComponent],
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class CategoryComponent implements OnInit {
	private readonly assetService = inject(AssetService);
	private readonly confirmationService = inject(ConfirmationService);
	private readonly navigationService = inject(NavigationService);
	private readonly destroyRef = inject(DestroyRef);

	readonly paths = paths;
	readonly previousRoute = computed(() => this.navigationService.previousRoute());
	readonly categories = signal<AssetCategory[]>([]);
	readonly defaultCategories = computed(() => this.categories().filter(c => c.isDefault));
	readonly customCategories = computed(() => this.categories().filter(c => !c.isDefault));
	readonly isDialogVisible = signal(false);
	readonly selectedCategory = signal<AssetCategory | null>(null);
	readonly isSaveLoading = signal(false);
	readonly isDeleteLoading = signal(false);

	ngOnInit() {
		this.assetService
			.getAssetCategories()
			.pipe(takeUntilDestroyed(this.destroyRef))
			.subscribe({
				next: categories => this.categories.set(categories)
			});
	}

	openAdd() {
		this.selectedCategory.set(null);
		this.isDialogVisible.set(true);
	}

	openEdit(category: AssetCategory) {
		this.selectedCategory.set({ ...category });
		this.isDialogVisible.set(true);
	}

	saveCategory(category: AssetCategory) {
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

	deleteCategory(event: { target: EventTarget; id: string }) {
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
