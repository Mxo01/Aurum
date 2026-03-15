import { ChangeDetectionStrategy, Component, input, signal, computed, inject } from "@angular/core";
import { CommonModule, DecimalPipe } from "@angular/common";
import { ChartModule } from "primeng/chart";
import { SelectButtonModule } from "primeng/selectbutton";
import { FormsModule } from "@angular/forms";
import { AnalyticsSummary, ChartData, Variation } from "../../model/dashboard.model";
import { Card } from "primeng/card";
import { Currency } from "../../../profile/model/currency.model";
import { SelectItem } from "primeng/api";
import { getNetworthChartOptions, mapDataIntoNetworthChartData } from "./networth-chart.utils";
import { currencyMap } from "../../../profile/profile.utils";
import { Button } from "primeng/button";
import { ThemeService } from "../../../../shared/services/theme/theme.service";

@Component({
	selector: "app-networth-chart",
	standalone: true,
	imports: [CommonModule, ChartModule, SelectButtonModule, FormsModule, Card, DecimalPipe, Button],
	templateUrl: "./networth-chart.component.html",
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class NetworthChartComponent {
	private readonly themeService = inject(ThemeService);

	data = input<ChartData | null>(null);
	summary = input<AnalyticsSummary | null>(null);
	currency = input.required<Currency>();

	protected readonly percentageView = signal<boolean>(true);
	protected readonly totalAmount = computed(() => this.summary()?.totalNetWorth ?? 0);
	protected readonly variation = computed(
		() => this.summary()?.variations?.[this.selectedPeriod()]
	);
	protected readonly hasEnoughDataToDisplayChart = computed(() =>
		this.data()?.totalNetWorth.some(value => value !== 0)
	);
	protected readonly timePeriods = signal<SelectItem<keyof Variation>[]>([
		{ label: "1M", value: "oneMonth" },
		{ label: "6M", value: "sixMonths" },
		{ label: "1Y", value: "oneYear" }
	]);
	protected readonly selectedPeriod = signal<keyof Variation>("oneMonth");
	protected readonly chartData = computed(() =>
		mapDataIntoNetworthChartData(this.data(), this.themeService.isDarkMode())
	);
	protected readonly chartOptions = computed(() =>
		getNetworthChartOptions(currencyMap[this.currency()], this.themeService.isDarkMode())
	);
}
