import { Component, computed, input, output, signal } from "@angular/core";
import { TableModule } from "primeng/table";
import { Tag } from "primeng/tag";
import { Badge } from "primeng/badge";
import { Button } from "primeng/button";
import { DecimalPipe } from "@angular/common";
import { PrivacyCurrencyPipe } from "../../../../shared/pipes/privacy-currency.pipe";
import { MenuItem, MenuItemCommandEvent } from "primeng/api";
import { Menu } from "primeng/menu";
import { Currency } from "../../../profile/model/currency.model";
import { Locale } from "../../../profile/model/locale.model";
import { Asset, AssetType } from "../../model/asset.model";
import { mapAssetsToAssetsWithBalance } from "./asset-table.utils";

@Component({
	selector: "app-asset-table",
	standalone: true,
	templateUrl: "./asset-table.component.html",
	imports: [TableModule, Tag, Badge, Button, PrivacyCurrencyPipe, DecimalPipe, Menu]
})
export class AssetTableComponent {
	assets = input.required<Asset[]>();
	isLoading = input.required<boolean>();
	currency = input<Currency>(Currency.EUR);
	locale = input<Locale>(Locale.EN_US);

	viewHistory = output<Asset>();
	editAsset = output<Asset>();
	toggleAssetStatus = output<Asset>();
	deleteAsset = output<{ event: MenuItemCommandEvent; asset: Asset }>();

	readonly Currency = Currency;
	readonly AssetType = AssetType;

	readonly rowMenuItems = signal<MenuItem[]>([]);
	expandedRowGroups: string[] = [];
	readonly assetsWithBalance = computed(() => mapAssetsToAssetsWithBalance(this.assets()));
	readonly categoryCounts = computed(() => {
		const counts: Record<string, number> = {};
		for (const asset of this.assetsWithBalance()) {
			counts[asset.categoryName] = (counts[asset.categoryName] ?? 0) + 1;
		}
		return counts;
	});
	readonly categoryTotals = computed(() => {
		const totals: Record<string, number> = {};

		for (const asset of this.assetsWithBalance()) {
			if (!totals[asset.categoryName]) totals[asset.categoryName] = 0;
			totals[asset.categoryName] += asset.currentValueBase ?? 0;
		}

		return totals;
	});

	showMenu(event: MouseEvent, menu: Menu, asset: Asset) {
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
				labelClass: "text-red-400",
				icon: "pi pi-trash",
				iconClass: "text-red-400!",
				command: event => this.deleteAsset.emit({ event, asset })
			}
		]);

		menu.toggle(event);
	}
}
