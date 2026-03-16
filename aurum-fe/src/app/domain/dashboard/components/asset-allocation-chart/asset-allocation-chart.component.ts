import { ChangeDetectionStrategy, Component, computed, inject, input } from "@angular/core";
import { Card } from "primeng/card";
import { UIChart } from "primeng/chart";
import { AnalyticsSummary } from "../../model/dashboard.model";
import {
	getAssetAllocationOptions,
	mapSummaryToAssetAllocationChart
} from "./asset-allocation-chart.utils";
import { ThemeService } from "../../../../shared/services/theme/theme.service";

@Component({
	selector: "app-asset-allocation-chart",
	standalone: true,
	imports: [Card, UIChart],
	templateUrl: "./asset-allocation-chart.component.html",
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class AssetAllocationChartComponent {
	private readonly themeService = inject(ThemeService);

	summary = input.required<AnalyticsSummary | null>();

	protected readonly assetAllocationChartData = computed(() =>
		mapSummaryToAssetAllocationChart(this.summary())
	);
	protected readonly chartOptions = computed(() =>
		getAssetAllocationOptions(this.themeService.isDarkMode())
	);
}
