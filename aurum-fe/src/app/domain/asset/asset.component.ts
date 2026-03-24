import { ChangeDetectionStrategy, Component, inject, signal, OnInit } from "@angular/core";
import { Button } from "primeng/button";
import { TableModule } from "primeng/table";
import { AssetService } from "./asset.service";
import { Asset, AssetCategory } from "./model/asset.model";
import { finalize, tap } from "rxjs";
import { ReactiveFormsModule, FormsModule } from "@angular/forms";
import { ConfirmationService, MenuItemCommandEvent } from "primeng/api";
import { NavigationService } from "../../shared/services/navigation/navigation.service";
import { ThemeService } from "../../shared/services/theme/theme.service";
import { RouterLink } from "@angular/router";
import { ProfileService } from "../profile/profile.service";
import { Currency } from "../profile/model/currency.model";
import { Locale } from "../profile/model/locale.model";
import { paths } from "../../app.routes";
import { AssetFormComponent } from "./components/asset-form/asset-form.component";
import { AssetHistoryComponent } from "./components/asset-history/asset-history.component";
import { AssetTableComponent } from "./components/asset-table/asset-table.component";
import { Dialog } from "primeng/dialog";
import { DatePicker } from "primeng/datepicker";

@Component({
	selector: "app-asset",
	standalone: true,
	templateUrl: "./asset.component.html",
	imports: [
		Button,
		TableModule,
		AssetFormComponent,
		FormsModule,
		ReactiveFormsModule,
		RouterLink,
		AssetHistoryComponent,
		AssetTableComponent,
		Dialog,
		DatePicker
	],
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class AssetComponent implements OnInit {
	private readonly assetService = inject(AssetService);
	private readonly confirmationService = inject(ConfirmationService);
	private readonly navigationService = inject(NavigationService);
	private readonly profileService = inject(ProfileService);
	private readonly themeService = inject(ThemeService);

	readonly paths = paths;
	readonly previousRoute = this.navigationService.previousRoute;
	readonly assets = signal<Asset[]>([]);
	readonly userCurrency = signal<Currency>(Currency.EUR);
	readonly userLocale = signal<Locale>(Locale.EN_US);
	readonly areAssetsLoading = signal(false);
	readonly categoriesOptions = signal<AssetCategory[]>([]);
	readonly isHistoryDialogVisible = signal(false);
	readonly isDrawerVisible = signal(false);
	readonly isDeletePermanentlyLoading = signal(false);
	readonly isSaveLoading = signal(false);
	readonly selectedAsset = signal<Asset | null>(null);

	readonly isStatusDialogVisible = signal(false);
	readonly pendingStatusToggle = signal<{ asset: Asset; newStatus: boolean } | null>(null);
	statusChangeDate: Date = new Date();

	ngOnInit() {
		this.profileService.getProfile().subscribe({
			next: profile => {
				this.userCurrency.set(profile.currency);
				this.userLocale.set(profile.locale);
				this.themeService.applyLocale(profile.locale);
			}
		});

		this.assetService.getAssetCategories().subscribe({
			next: categories => this.categoriesOptions.set(categories)
		});

		this.refreshAssets();
	}

	refreshAssets() {
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

	viewHistory(asset: Asset) {
		this.selectedAsset.set(asset);
		this.isHistoryDialogVisible.set(true);
	}

	editAsset(asset: Asset) {
		this.selectedAsset.set(asset);
		this.isDrawerVisible.set(true);
	}

	toggleAssetStatus(asset: Asset) {
		this.pendingStatusToggle.set({ asset, newStatus: !asset.isActive });
		this.statusChangeDate = new Date();
		this.isStatusDialogVisible.set(true);
	}

	cancelStatusToggle() {
		this.isStatusDialogVisible.set(false);
		this.pendingStatusToggle.set(null);
	}

	confirmStatusToggle() {
		const pending = this.pendingStatusToggle();
		if (!pending) return;

		const { asset, newStatus } = pending;
		const d = this.statusChangeDate;
		const changedAt =
			d.getFullYear() +
			"-" +
			String(d.getMonth() + 1).padStart(2, "0") +
			"-" +
			String(d.getDate()).padStart(2, "0");

		this.isStatusDialogVisible.set(false);
		this.pendingStatusToggle.set(null);
		this.isSaveLoading.set(true);

		this.assetService
			.patchAssetStatus(asset.id, newStatus, changedAt)
			.pipe(finalize(() => this.isSaveLoading.set(false)))
			.subscribe({
				next: assets => this.assets.set(assets)
			});
	}

	deleteAsset({ event, asset }: { event: MenuItemCommandEvent; asset: Asset }) {
		const id = asset.id;
		const target = event?.originalEvent?.target;

		if (!id || !target) return;

		this.confirmationService.confirm({
			target,
			message:
				"Are you sure you want to delete this asset forever? All its history will be lost. This action is irreversible.",
			header: "Permanent Deletion",
			icon: "pi pi-exclamation-triangle",
			rejectVisible: false,
			acceptButtonProps: {
				size: "small",
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

	saveAssetAndSnapshot(asset: Asset) {
		this.isSaveLoading.set(true);

		this.assetService
			.saveAsset(asset)
			.pipe(finalize(() => this.isSaveLoading.set(false)))
			.subscribe({
				next: assets => this.assets.set(assets)
			});
	}
}
