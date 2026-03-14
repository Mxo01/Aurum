import { Currency } from "./../profile/model/currency.model";
import {
	ChangeDetectionStrategy,
	Component,
	inject,
	signal,
	OnInit,
	viewChild,
	computed
} from "@angular/core";
import { Button } from "primeng/button";
import { BlockUIModule } from "primeng/blockui";
import { TableModule } from "primeng/table";
import { AssetService } from "./asset.service";
import { Asset, AssetCategory, AssetType } from "./model/asset.model";
import { finalize } from "rxjs";
import { Drawer } from "primeng/drawer";
import { AssetFormComponent } from "./asset-form/asset-form.component";
import { Dialog } from "primeng/dialog";
import { ReactiveFormsModule, FormsModule } from "@angular/forms";
import { CategoryFormComponent } from "./category-form/category-form.component";
import { Menu } from "primeng/menu";
import { ConfirmationService, MenuItem, MenuItemCommandEvent } from "primeng/api";
import { Tag } from "primeng/tag";
import { NavigationService } from "../../shared/services/navigation/navigation.service";
import { RouterLink } from "@angular/router";
import { paths } from "../../app.routes";

@Component({
	selector: "app-asset",
	standalone: true,
	templateUrl: "./asset.component.html",
	styles: `
		::ng-deep {
			.p-listbox-empty-message {
				height: 100%;
			}
		}
	`,
	imports: [
		Button,
		BlockUIModule,
		TableModule,
		Drawer,
		Menu,
		AssetFormComponent,
		Dialog,
		Tag,
		FormsModule,
		ReactiveFormsModule,
		CategoryFormComponent,
		RouterLink
	],
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class AssetComponent implements OnInit {
	private readonly assetService = inject(AssetService);
	private readonly confirmationService = inject(ConfirmationService);
	private readonly navigationService = inject(NavigationService);

	protected readonly assetFormComponent = viewChild<AssetFormComponent>("assetForm");
	protected readonly categoryFormComponent = viewChild<CategoryFormComponent>("categoryForm");

	protected readonly Currency = Currency;
	protected readonly AssetType = AssetType;

	protected readonly paths = paths;
	protected readonly previousRoute = this.navigationService.previousRoute;
	protected readonly categoriesOptions = signal<AssetCategory[]>([]);
	protected readonly assets = signal<Asset[]>([]);
	protected readonly areAssetsLoading = signal(false);
	protected readonly isDrawerVisible = signal(false);
	protected readonly isNewCategory = signal(false);
	protected readonly isCategoriesDialogVisible = signal(false);
	protected readonly isCategorySaveLoading = signal(false);
	protected readonly isCategoryDeleteLoading = signal(false);
	protected readonly isSaveLoading = signal(false);
	protected readonly selectedAsset = signal<Asset | null>(null);
	protected readonly rowMenuItems = computed<MenuItem[]>(() => [
		{
			label: "Edit",
			icon: "pi pi-pencil",
			command: () => this.editAsset(this.selectedAsset())
		},
		{
			label: this.selectedAsset()?.isActive ? "Archive" : "Activate",
			labelClass: this.selectedAsset()?.isActive ? "text-red-500" : "",
			icon: this.selectedAsset()?.isActive ? "pi pi-inbox" : "pi pi-check-circle",
			iconClass: this.selectedAsset()?.isActive ? "text-red-500!" : "",
			command: event => this.toggleAssetStatus(event)
		}
	]);

	ngOnInit() {
		this.assetService.getUserAssetCategories().subscribe({
			next: categories => this.categoriesOptions.set(categories)
		});

		this.areAssetsLoading.set(true);

		this.assetService
			.getAssets()
			.pipe(finalize(() => this.areAssetsLoading.set(false)))
			.subscribe({
				next: assets => this.assets.set(assets)
			});
	}

	private editAsset(asset: Asset | null) {
		if (!asset) return;
		this.isDrawerVisible.set(true);
	}

	private toggleAssetStatus(event: MenuItemCommandEvent) {
		if (this.selectedAsset()?.isActive) {
			this.deleteAsset({
				target: event?.originalEvent?.target,
				id: this.selectedAsset()?.id ?? ""
			});
		} else {
			const asset = this.selectedAsset();
			if (!asset) return;

			const updatedAsset: Asset = {
				...asset,
				isActive: !asset.isActive
			};

			this.saveAsset(updatedAsset);
		}
	}

	private deleteAsset(event: { target?: EventTarget | null; id: string }) {
		const { id, target } = event;

		if (!id || !target) return;

		this.confirmationService.confirm({
			target,
			message: "Are you sure you want to archive this asset?",
			header: "Danger Zone",
			icon: "pi pi-info-circle",
			rejectLabel: "Cancel",
			rejectButtonProps: {
				label: "Cancel",
				severity: "secondary",
				outlined: true
			},
			acceptButtonProps: {
				label: "Archive",
				severity: "danger",
				loading: this.isCategoryDeleteLoading()
			},

			accept: () => {
				this.assetService.deleteAsset(id).subscribe({
					next: assets => this.assets.set(assets)
				});
			}
		});
	}

	saveCategory(category: AssetCategory) {
		this.isCategorySaveLoading.set(true);

		this.assetService
			.saveCategory(category)
			.pipe(finalize(() => this.isCategorySaveLoading.set(false)))
			.subscribe({
				next: categories => this.categoriesOptions.set(categories)
			});
	}

	saveAssetFromForm() {
		const form = this.assetFormComponent()?.assetForm.getRawValue();

		if (!form || !form.category || !form.type || !form.currency || !form.name) return;

		const asset: Asset = {
			id: this.selectedAsset()?.id ?? "",
			name: form.name,
			categoryId: form.category.id,
			categoryName: form.category.name,
			type: form.type,
			originalCurrency: form.currency,
			isActive: !!form.isActive,
			isFavorite: !!form.isFavorite
		};

		this.saveAsset(asset);
	}

	private saveAsset(asset: Asset) {
		this.isSaveLoading.set(true);

		this.assetService
			.saveAsset(asset)
			.pipe(finalize(() => this.isSaveLoading.set(false)))
			.subscribe({
				next: assets => {
					this.assets.set(assets);
					this.hideDrawer();
				}
			});
	}

	hideDrawer() {
		this.isDrawerVisible.set(false);
		this.selectedAsset.set(null);
	}

	deleteCategory(event: { target: EventTarget; id: string }) {
		const { id, target } = event;

		this.confirmationService.confirm({
			target,
			message: "Are you sure you want to delete this category?",
			header: "Danger Zone",
			icon: "pi pi-info-circle",
			rejectLabel: "Cancel",
			rejectButtonProps: {
				label: "Cancel",
				severity: "secondary",
				outlined: true
			},
			acceptButtonProps: {
				label: "Delete",
				severity: "danger",
				loading: this.isCategoryDeleteLoading()
			},

			accept: () => {
				if (!id) return;

				this.isCategoryDeleteLoading.set(true);

				this.assetService
					.deleteCategory(id)
					.pipe(finalize(() => this.isCategoryDeleteLoading.set(false)))
					.subscribe({
						next: categories => this.categoriesOptions.set(categories)
					});
			}
		});
	}
}
