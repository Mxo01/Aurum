import { Component, computed, input, output, signal } from "@angular/core";
import { TableModule } from "primeng/table";
import { Tag } from "primeng/tag";
import { Button } from "primeng/button";
import { CurrencyPipe, DecimalPipe } from "@angular/common";
import { Asset, AssetType } from "../model/asset.model";
import { Currency } from "../../profile/model/currency.model";
import { MenuItem, MenuItemCommandEvent } from "primeng/api";
import { Menu } from "primeng/menu";

@Component({
	selector: "app-asset-table",
	standalone: true,
	templateUrl: "./asset-table.component.html",
	imports: [TableModule, Tag, Button, CurrencyPipe, DecimalPipe, Menu]
})
export class AssetTableComponent {
	assets = input.required<Asset[]>();
	isLoading = input.required<boolean>();

	viewHistory = output<Asset>();
	editAsset = output<Asset>();
	toggleAssetStatus = output<Asset>();
	deleteAsset = output<{ event: MenuItemCommandEvent; asset: Asset }>();

	protected readonly Currency = Currency;
	protected readonly AssetType = AssetType;

	protected readonly rowMenuItems = signal<MenuItem[]>([]);
	protected readonly assetsWithBalance = computed(() => {
		return this.assets().map(asset => {
			const assetSnapshots = (asset.snapshots || []).sort(
				(a, b) => new Date(b.referenceDate).getTime() - new Date(a.referenceDate).getTime()
			);

			const currentValue = assetSnapshots.length > 0 ? assetSnapshots[0].amountOriginalCurrency : 0;
			const previousValue =
				assetSnapshots.length > 1 ? assetSnapshots[1].amountOriginalCurrency : null;

			let trend = null;
			let trendPercentage = null;

			if (previousValue !== null && previousValue !== 0) {
				trend = currentValue - previousValue;
				trendPercentage = (trend / previousValue) * 100;
			} else if (previousValue === 0 && currentValue > 0) {
				trend = currentValue;
				trendPercentage = 100;
			}

			return {
				...asset,
				currentValue,
				trend,
				trendPercentage,
				snapshots: assetSnapshots
			};
		});
	});

	protected showMenu(event: MouseEvent, menu: Menu, asset: Asset) {
		this.rowMenuItems.set([
			{
				label: "Edit",
				icon: "pi pi-pencil",
				command: () => this.editAsset.emit(asset)
			},
			{
				label: "View History",
				icon: "pi pi-history",
				command: () => this.viewHistory.emit(asset)
			},
			{
				label: asset.isActive ? "Archive" : "Activate",
				icon: asset.isActive ? "pi pi-inbox" : "pi pi-check-circle",
				command: () => this.toggleAssetStatus.emit(asset)
			},
			{
				label: "Delete Forever",
				labelClass: "text-red-500",
				icon: "pi pi-trash",
				iconClass: "text-red-500!",
				command: event => this.deleteAsset.emit({ event, asset })
			}
		]);

		menu.toggle(event);
	}
}
