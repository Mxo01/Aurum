import { ChangeDetectionStrategy, Component, computed, input, output } from "@angular/core";
import { CommonModule, DecimalPipe } from "@angular/common";
import { TableModule } from "primeng/table";
import { ButtonModule } from "primeng/button";
import { Asset, AssetType } from "../../../asset/model/asset.model";
import { Card } from "primeng/card";
import { Tag } from "primeng/tag";
import { mapAssetsToAssetsWithBalance } from "../../../asset/components/asset-table/asset-table.utils";

@Component({
	selector: "app-top-assets",
	standalone: true,
	imports: [CommonModule, TableModule, ButtonModule, Card, Tag, DecimalPipe],
	templateUrl: "./top-assets-widget.component.html",
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class TopAssetsWidgetComponent {
	assets = input.required<Asset[]>();
	viewMore = output<void>();

	protected readonly AssetType = AssetType;

	protected readonly assetsWithBalance = computed(() =>
		mapAssetsToAssetsWithBalance(this.assets())
	);
}
