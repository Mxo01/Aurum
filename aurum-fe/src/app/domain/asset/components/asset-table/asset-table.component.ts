import { Component, computed, input, output, signal } from "@angular/core";
import { TableModule } from "primeng/table";
import { Tag } from "primeng/tag";
import { Button } from "primeng/button";
import { CurrencyPipe, DecimalPipe } from "@angular/common";
import { MenuItem, MenuItemCommandEvent } from "primeng/api";
import { Menu } from "primeng/menu";
import { Currency } from "../../../profile/model/currency.model";
import { Asset, AssetType } from "../../model/asset.model";
import { mapAssetsToAssetsWithBalance } from "./asset-table.utils";

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
	protected readonly assetsWithBalance = computed(() =>
		mapAssetsToAssetsWithBalance(this.assets())
	);

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
