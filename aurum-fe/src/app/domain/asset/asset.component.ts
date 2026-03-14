import { ChangeDetectionStrategy, Component, inject, signal, OnInit } from "@angular/core";
import { Button } from "primeng/button";
import { BlockUIModule } from "primeng/blockui";
import { TableModule } from "primeng/table";
import { AssetService } from "./asset.service";
import { Asset, AssetCategory } from "./model/asset.model";
import { finalize, switchMap, tap } from "rxjs";
import { AssetFormComponent } from "./asset-form/asset-form.component";
import { ReactiveFormsModule, FormsModule } from "@angular/forms";
import { CategoryFormComponent } from "./category-form/category-form.component";
import { ConfirmationService, MenuItemCommandEvent } from "primeng/api";
import { NavigationService } from "../../shared/services/navigation/navigation.service";
import { RouterLink } from "@angular/router";
import { paths } from "../../app.routes";
import { AssetHistoryComponent } from "./asset-history/asset-history.component";
import { AssetTableComponent } from "./asset-table/asset-table.component";

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
		AssetFormComponent,
		FormsModule,
		ReactiveFormsModule,
		CategoryFormComponent,
		RouterLink,
		AssetHistoryComponent,
		AssetTableComponent
	],
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class AssetComponent implements OnInit {
	private readonly assetService = inject(AssetService);
	private readonly confirmationService = inject(ConfirmationService);
	private readonly navigationService = inject(NavigationService);

	protected readonly paths = paths;
	protected readonly previousRoute = this.navigationService.previousRoute;
	protected readonly assets = signal<Asset[]>([]);
	protected readonly areAssetsLoading = signal(false);
	protected readonly categoriesOptions = signal<AssetCategory[]>([]);
	protected readonly isHistoryDialogVisible = signal(false);
	protected readonly isDrawerVisible = signal(false);
	protected readonly isDeletePermanentlyLoading = signal(false);
	protected readonly isNewCategory = signal(false);
	protected readonly isCategoriesDialogVisible = signal(false);
	protected readonly isCategorySaveLoading = signal(false);
	protected readonly isCategoryDeleteLoading = signal(false);
	protected readonly isSaveLoading = signal(false);
	protected readonly selectedAsset = signal<Asset | null>(null);

	ngOnInit() {
		this.assetService.getUserAssetCategories().subscribe({
			next: categories => this.categoriesOptions.set(categories)
		});

		this.refreshAssets();
	}

	protected refreshAssets() {
		this.getAssets().subscribe();
	}

	private getAssets() {
		this.areAssetsLoading.set(true);

		return this.assetService.getAssets().pipe(
			finalize(() => this.areAssetsLoading.set(false)),
			tap({
				next: assets => {
					this.assets.set(assets);
					const selectedAsset = this.selectedAsset();

					if (selectedAsset) {
						const updatedAsset = assets.find(({ id }) => id === selectedAsset.id);
						if (updatedAsset) this.selectedAsset.set(updatedAsset);
					}
				}
			})
		);
	}

	protected viewHistory(asset: Asset) {
		this.selectedAsset.set(asset);
		this.isHistoryDialogVisible.set(true);
	}

	protected editAsset(asset: Asset) {
		this.selectedAsset.set(asset);
		this.isDrawerVisible.set(true);
	}

	protected toggleAssetStatus(asset: Asset) {
		const updatedAsset: Asset = {
			...asset,
			isActive: !asset.isActive
		};

		this.saveAssetAndSnapshot(updatedAsset);
	}

	protected deleteAsset({ event, asset }: { event: MenuItemCommandEvent; asset: Asset }) {
		const id = asset.id;
		const target = event?.originalEvent?.target;

		if (!id || !target) return;

		this.confirmationService.confirm({
			target,
			message:
				"Are you sure you want to delete this asset forever? All its history will be lost. This action is irreversible.",
			header: "Permanent Deletion",
			icon: "pi pi-exclamation-triangle",
			rejectLabel: "Cancel",
			rejectButtonProps: {
				label: "Cancel",
				severity: "secondary",
				outlined: true
			},
			acceptButtonProps: {
				label: "Delete Forever",
				severity: "danger",
				loading: this.isDeletePermanentlyLoading()
			},
			accept: () => {
				this.isDeletePermanentlyLoading.set(true);

				this.assetService
					.deleteAssetPermanently(id)
					.pipe(finalize(() => this.isDeletePermanentlyLoading.set(false)))
					.subscribe({
						next: assets => this.assets.set(assets)
					});
			}
		});
	}

	protected saveAssetAndSnapshot(asset: Asset) {
		this.isSaveLoading.set(true);

		this.assetService
			.saveAsset(asset)
			.pipe(finalize(() => this.isSaveLoading.set(false)))
			.subscribe({
				next: assets => this.assets.set(assets)
			});
	}

	protected saveCategory(category: AssetCategory) {
		this.isCategorySaveLoading.set(true);

		this.assetService
			.saveCategory(category)
			.pipe(finalize(() => this.isCategorySaveLoading.set(false)))
			.subscribe({
				next: categories => this.categoriesOptions.set(categories)
			});
	}

	protected deleteCategory(event: { target: EventTarget; id: string }) {
		const { id, target } = event;

		this.confirmationService.confirm({
			target,
			message: "Are you sure you want to delete this category?",
			header: "Danger Zone",
			icon: "pi pi-info-circle",
			rejectLabel: "Cancel",
			rejectButtonProps: {
				label: "Cancel",
				severity: "secondary"
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
					.pipe(
						switchMap(categories => {
							this.categoriesOptions.set(categories);
							return this.getAssets();
						}),
						finalize(() => this.isCategoryDeleteLoading.set(false))
					)
					.subscribe({
						next: assets => this.assets.set(assets)
					});
			}
		});
	}
}
