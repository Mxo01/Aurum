import { ChangeDetectionStrategy, Component, computed, input, output } from "@angular/core";
import { CommonModule, DecimalPipe } from "@angular/common";
import { TableModule } from "primeng/table";
import { ButtonModule } from "primeng/button";
import { Asset, AssetType } from "../../../asset/model/asset.model";
import { Card } from "primeng/card";
import { mapAssetsToAssetsWithBalance } from "../../../asset/components/asset-table/asset-table.utils";
import { AssetAllocationChartComponent } from "../asset-allocation-chart/asset-allocation-chart.component";
import { AnalyticsSummary } from "../../model/dashboard.model";
import { Divider } from "primeng/divider";

@Component({
	selector: "app-top-assets",
	standalone: true,
	imports: [
		CommonModule,
		TableModule,
		ButtonModule,
		Card,
		DecimalPipe,
		AssetAllocationChartComponent,
		Divider
	],
	templateUrl: "./top-assets-widget.component.html",
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class TopAssetsWidgetComponent {
	assets = input.required<Asset[]>();
	summary = input.required<AnalyticsSummary | null>();
	viewMore = output<void>();

	protected readonly AssetType = AssetType;

	protected readonly assetsWithBalance = computed(() =>
		mapAssetsToAssetsWithBalance(this.assets())
	);
}
