import { ChangeDetectionStrategy, Component, computed, input } from "@angular/core";
import { Card } from "primeng/card";
import { UIChart } from "primeng/chart";
import { AnalyticsSummary } from "../../model/dashboard.model";
import { mapSummaryToAssetAllocationChart } from "./asset-allocation-chart.utils";

@Component({
	selector: "app-asset-allocation-chart",
	standalone: true,
	imports: [Card, UIChart],
	templateUrl: "./asset-allocation-chart.component.html",
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class AssetAllocationChartComponent {
	summary = input.required<AnalyticsSummary | null>();

	protected readonly assetAllocationChartData = computed(() =>
		mapSummaryToAssetAllocationChart(this.summary())
	);
	protected readonly chartOptions = {
		plugins: { legend: { position: "right" } },
		responsive: true,
		maintainAspectRatio: false
	};
}
