import { ChangeDetectionStrategy, Component, computed, input } from "@angular/core";
import { ChartModule } from "primeng/chart";
import { Skeleton } from "primeng/skeleton";
import { CashFlowEntry } from "../../model/cashflow.model";
import { Currency } from "../../../profile/model/currency.model";
import { Locale } from "../../../profile/model/locale.model";
import { getCurrencySymbol } from "../../../profile/profile.utils";
import {
	buildBarLabelsPlugin,
	getCashFlowChartOptions,
	mapCashFlowToChartData
} from "./cashflow-chart.utils";

@Component({
	selector: "app-cashflow-chart",
	standalone: true,
	imports: [ChartModule, Skeleton],
	templateUrl: "./cashflow-chart.component.html",
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class CashflowChartComponent {
	isLoading = input.required<boolean>();
	entries = input<CashFlowEntry[]>([]);
	currency = input.required<Currency>();
	locale = input.required<Locale>();
	isDarkMode = input.required<boolean>();
	isPrivacyMode = input.required<boolean>();

	readonly chartData = computed(() => {
		const e = this.entries();
		if (!e.length) return null;
		return mapCashFlowToChartData(e);
	});
	readonly isChartEmpty = computed(() => {
		const data = this.chartData();
		const earnedEmpty = data?.datasets[0].data.every(v => !v);
		const spentEmpty = data?.datasets[1].data.every(v => !v);
		return earnedEmpty && spentEmpty;
	});
	readonly chartOptions = computed(() =>
		getCashFlowChartOptions(
			getCurrencySymbol(this.currency()),
			this.isDarkMode(),
			this.locale(),
			this.isPrivacyMode(),
			this.entries()
		)
	);
	readonly chartPlugins = computed(() => [
		buildBarLabelsPlugin(
			getCurrencySymbol(this.currency()),
			this.isDarkMode(),
			this.isPrivacyMode()
		)
	]);
}
