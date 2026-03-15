import { FormsModule } from "@angular/forms";
import { Component, computed, inject, model, output, signal } from "@angular/core";
import { Dialog } from "primeng/dialog";
import { TableModule } from "primeng/table";
import { Button } from "primeng/button";
import { InputNumber } from "primeng/inputnumber";
import { DatePicker } from "primeng/datepicker";
import { DatePipe, CurrencyPipe } from "@angular/common";
import { SelectButton } from "primeng/selectbutton";
import { ViewMode } from "./model/asset-history.model";
import { SelectItem } from "primeng/api";
import { UIChart } from "primeng/chart";
import { isTruthy } from "../../../../shared/utils";
import { Currency } from "../../../profile/model/currency.model";
import { currencyMap } from "../../../profile/profile.utils";
import { Snapshot } from "../../../snapshot/model/snapshot.model";
import { SnapshotService } from "../../../snapshot/snapshot.service";
import { Asset } from "../../model/asset.model";
import { mapSnapshotsToChartData, getChartOptions } from "./asset-history.utils";

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
		CurrencyPipe,
		FormsModule,
		SelectButton,
		UIChart
	]
})
export class AssetHistoryComponent {
	private readonly snapshotService = inject(SnapshotService);

	isVisible = model.required<boolean>();
	selectedAsset = model<Asset | null>();
	snapshotsChanged = output<void>();

	protected readonly ViewMode = ViewMode;

	protected viewMode = signal<ViewMode>(ViewMode.Table);
	protected viewModeOptions = signal<SelectItem<ViewMode>[]>([
		{ label: "Table", value: ViewMode.Table, icon: "pi pi-table" },
		{ label: "Chart", value: ViewMode.Chart, icon: "pi pi-chart-line" }
	]);
	protected selectedSnapshotsHistory = signal<Snapshot[]>([]);
	protected newSnapshotValue = signal<number | null>(null);
	protected newSnapshotDate = signal<Date>(new Date());
	protected readonly snapshotsForSelectedAsset = computed(() => {
		const asset = this.selectedAsset();
		if (!asset || !asset.snapshots) return [];

		return asset.snapshots.toSorted(
			(a, b) => new Date(b.referenceDate).getTime() - new Date(a.referenceDate).getTime()
		);
	});
	protected readonly chartData = computed(() =>
		mapSnapshotsToChartData(this.snapshotsForSelectedAsset())
	);
	protected readonly chartOptions = computed(() =>
		getChartOptions(currencyMap[this.selectedAsset()?.originalCurrency || Currency.EUR])
	);

	protected onHideDrawer() {
		this.selectedSnapshotsHistory.set([]);
		this.newSnapshotValue.set(null);
		this.newSnapshotDate.set(new Date());
		this.selectedAsset.set(null);
	}

	protected addSnapshotFromHistory() {
		const assetId = this.selectedAsset()?.id;
		const amount = this.newSnapshotValue();
		const date = this.newSnapshotDate();

		if (!assetId || amount === null || !date) return;

		const formattedDate =
			date.getFullYear() +
			"-" +
			String(date.getMonth() + 1).padStart(2, "0") +
			"-" +
			String(date.getDate()).padStart(2, "0");

		const snapshot: Snapshot = {
			assetId,
			amountOriginalCurrency: amount,
			referenceDate: formattedDate
		};

		this.snapshotService.saveSnapshot(snapshot).subscribe({
			next: () => {
				this.snapshotsChanged.emit();
				this.newSnapshotValue.set(null);
				this.newSnapshotDate.set(new Date());
			}
		});
	}

	protected deleteSelectedSnapshotsHistory() {
		const assetId = this.selectedAsset()?.id;
		const ids = this.selectedSnapshotsHistory()
			.map(s => s.id)
			.filter(isTruthy);
		if (!assetId || !ids.length) return;

		this.snapshotService.deleteSnapshotsBulk(assetId, ids).subscribe({
			next: () => {
				this.snapshotsChanged.emit();
				this.selectedSnapshotsHistory.set([]);
			}
		});
	}
}
