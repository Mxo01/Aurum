import {
	ChangeDetectionStrategy,
	Component,
	DestroyRef,
	inject,
	signal,
	OnInit,
	computed
} from "@angular/core";
import { Button } from "primeng/button";
import { TableModule } from "primeng/table";
import { AssetService } from "./asset.service";
import { Asset, AssetCategory } from "./model/asset.model";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { finalize, switchMap, tap } from "rxjs";
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
import { formatDateToISO } from "../../shared/utils";
import { SnapshotService } from "../snapshot/snapshot.service";

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
		AssetTableComponent
	],
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class AssetComponent implements OnInit {
	private readonly assetService = inject(AssetService);
	private readonly snapshotService = inject(SnapshotService);
	private readonly confirmationService = inject(ConfirmationService);
	private readonly navigationService = inject(NavigationService);
	private readonly profileService = inject(ProfileService);
	private readonly themeService = inject(ThemeService);
	private readonly destroyRef = inject(DestroyRef);

	readonly paths = paths;
	readonly previousRoute = computed(() => this.navigationService.previousRoute());
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

	ngOnInit() {
		this.profileService
			.getProfile()
			.pipe(takeUntilDestroyed(this.destroyRef))
			.subscribe({
				next: profile => {
					this.userCurrency.set(profile.currency);
					this.userLocale.set(profile.locale);
					this.themeService.applyLocale(profile.locale);
				}
			});

		this.assetService
			.getAssetCategories()
			.pipe(takeUntilDestroyed(this.destroyRef))
			.subscribe({
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
			takeUntilDestroyed(this.destroyRef),
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

	currentValueUpdate({ assetId, value }: { assetId: string; value: number }) {
		this.snapshotService
			.saveSnapshot({
				assetId,
				amountOriginalCurrency: value,
				referenceDate: formatDateToISO(new Date())
			})
			.pipe(switchMap(() => this.getAssets()))
			.subscribe({ next: assets => this.assets.set(assets) });
	}
}
