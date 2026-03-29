import { FormsModule } from "@angular/forms";
import { Component, computed, inject, input, model, output, signal } from "@angular/core";
import { Dialog } from "primeng/dialog";
import { TableModule } from "primeng/table";
import { Button } from "primeng/button";
import { InputNumber } from "primeng/inputnumber";
import { DatePicker } from "primeng/datepicker";
import { DatePipe } from "@angular/common";
import { PrivacyCurrencyPipe } from "../../../../shared/pipes/privacy-currency.pipe";
import { SelectButton } from "primeng/selectbutton";
import { ViewMode } from "./model/asset-history.model";
import { SelectItem } from "primeng/api";
import { UIChart } from "primeng/chart";
import { takeUntilDestroyed, toObservable } from "@angular/core/rxjs-interop";
import { of, switchMap } from "rxjs";
import { formatDateToISO, isTruthy } from "../../../../shared/utils";
import { Currency } from "../../../profile/model/currency.model";
import { Locale } from "../../../profile/model/locale.model";
import { getCurrencySymbol } from "../../../profile/profile.utils";
import { Snapshot } from "../../../snapshot/model/snapshot.model";
import { SnapshotService } from "../../../snapshot/snapshot.service";
import { Asset, LiabilityType } from "../../model/asset.model";
import { mapSnapshotsToChartData, getChartOptions } from "./asset-history.utils";
import { ThemeService } from "../../../../shared/services/theme/theme.service";
import { Message } from "primeng/message";

@Component({
	selector: "app-asset-history",
	standalone: true,
	templateUrl: "./asset-history.component.html",
	imports: [
		Dialog,
		TableModule,
		Button,
		FormsModule,
		InputNumber,
		DatePicker,
		DatePipe,
		PrivacyCurrencyPipe,
		FormsModule,
		Message,
		SelectButton,
		UIChart
	]
})
export class AssetHistoryComponent {
	readonly themeService = inject(ThemeService);
	private readonly snapshotService = inject(SnapshotService);

	isVisible = model.required<boolean>();
	selectedAsset = model<Asset | null>();
	locale = input<Locale>(Locale.EN_US);
	snapshotsChanged = output<void>();

	readonly ViewMode = ViewMode;
	readonly LiabilityType = LiabilityType;

	viewMode = signal<ViewMode>(ViewMode.Table);
	viewModeOptions = signal<SelectItem<ViewMode>[]>([
		{ label: "Table", value: ViewMode.Table, icon: "pi pi-table" },
		{ label: "Chart", value: ViewMode.Chart, icon: "pi pi-chart-line" }
	]);
	selectedSnapshotsHistory = signal<Snapshot[]>([]);
	newSnapshotValue = signal<number | null>(null);
	newSnapshotDate = signal<Date>(new Date());

	private readonly snapshots = signal<Snapshot[]>([]);

	constructor() {
		toObservable(this.selectedAsset)
			.pipe(
				switchMap(asset =>
					asset?.id ? this.snapshotService.getSnapshotsByAssetId(asset.id) : of([])
				),
				takeUntilDestroyed()
			)
			.subscribe({ next: snapshots => this.snapshots.set(snapshots) });
	}

	readonly isAutomaticLiability = computed(() => {
		const asset = this.selectedAsset();
		return asset?.liabilityType === LiabilityType.AUTOMATIC;
	});

	readonly snapshotsForSelectedAsset = computed(() =>
		this.snapshots().toSorted(
			(a, b) => new Date(b.referenceDate).getTime() - new Date(a.referenceDate).getTime()
		)
	);

	readonly chartData = computed(() =>
		mapSnapshotsToChartData(
			this.snapshotsForSelectedAsset(),
			this.themeService.isDarkMode(),
			this.locale()
		)
	);
	readonly chartOptions = computed(() =>
		getChartOptions(
			getCurrencySymbol(this.selectedAsset()?.originalCurrency || Currency.EUR),
			this.themeService.isDarkMode(),
			this.locale()
		)
	);

	onHideDrawer() {
		this.selectedSnapshotsHistory.set([]);
		this.newSnapshotValue.set(null);
		this.newSnapshotDate.set(new Date());
		this.selectedAsset.set(null);
		this.viewMode.set(ViewMode.Table);
	}

	addSnapshotFromHistory() {
		const assetId = this.selectedAsset()?.id;
		const amount = this.newSnapshotValue();
		const date = this.newSnapshotDate();

		if (!assetId || amount === null || !date) return;

		const formattedDate = formatDateToISO(date);

		const snapshot: Snapshot = {
			assetId,
			amountOriginalCurrency: amount,
			referenceDate: formattedDate
		};

		this.snapshotService.saveSnapshot(snapshot).subscribe({
			next: snapshots => {
				this.snapshots.set(snapshots);
				this.snapshotsChanged.emit();
				this.newSnapshotValue.set(null);
				this.newSnapshotDate.set(new Date());
			}
		});
	}

	deleteSelectedSnapshotsHistory() {
		const assetId = this.selectedAsset()?.id;
		const ids = this.selectedSnapshotsHistory()
			.map(s => s.id)
			.filter(isTruthy);
		if (!assetId || !ids.length) return;

		this.snapshotService.deleteSnapshotsBulk(assetId, ids).subscribe({
			next: snapshots => {
				this.snapshots.set(snapshots);
				this.snapshotsChanged.emit();
				this.selectedSnapshotsHistory.set([]);
			}
		});
	}
}
