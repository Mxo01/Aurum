import { ChangeDetectionStrategy, Component, computed, input } from "@angular/core";
import { MeterGroup } from "primeng/metergroup";
import { AnalyticsSummary } from "../../model/dashboard.model";
import { mapSummaryToMeterItems } from "./asset-allocation-chart.utils";

@Component({
	selector: "app-asset-allocation-chart",
	standalone: true,
	imports: [MeterGroup],
	templateUrl: "./asset-allocation-chart.component.html",
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class AssetAllocationChartComponent {
	summary = input.required<AnalyticsSummary | null>();

	protected readonly meterItems = computed(() => mapSummaryToMeterItems(this.summary()));
}
