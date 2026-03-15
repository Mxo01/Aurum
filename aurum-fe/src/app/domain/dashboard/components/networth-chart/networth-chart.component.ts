import { ChangeDetectionStrategy, Component, input, signal, computed } from "@angular/core";
import { CommonModule } from "@angular/common";
import { ChartModule } from "primeng/chart";
import { SelectButtonModule } from "primeng/selectbutton";
import { FormsModule } from "@angular/forms";
import { AnalyticsSummary, ChartData, Variation } from "../../model/dashboard.model";
import { Card } from "primeng/card";
import { Currency } from "../../../profile/model/currency.model";
import { mapDataIntoNetworthChartData, netWorthChartOptions } from "./networth-chart.utils";
import { SelectItem } from "primeng/api";

@Component({
	selector: "app-networth-chart",
	standalone: true,
	imports: [CommonModule, ChartModule, SelectButtonModule, FormsModule, Card],
	templateUrl: "./networth-chart.component.html",
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class NetworthChartComponent {
	data = input<ChartData | null>(null);
	summary = input<AnalyticsSummary | null>(null);
	currency = input.required<Currency>();

	protected readonly totalAmount = computed(() => this.summary()?.totalNetWorth ?? 0);
	protected readonly variation = computed(
		() => this.summary()?.variations?.[this.selectedPeriod()]?.percentage ?? 0
	);
	protected readonly timePeriods = signal<SelectItem<keyof Variation>[]>([
		{ label: "1M", value: "oneMonth" },
		{ label: "6M", value: "sixMonths" },
		{ label: "1Y", value: "oneYear" }
	]);
	protected readonly selectedPeriod = signal<keyof Variation>("oneMonth");
	protected readonly chartData = computed(() => mapDataIntoNetworthChartData(this.data()));
	protected readonly chartOptions = netWorthChartOptions;
}
