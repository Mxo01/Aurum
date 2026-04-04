import { ChangeDetectionStrategy, Component, computed, inject, input } from "@angular/core";
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
import { DOCUMENT } from "@angular/common";
import { fromEvent } from "rxjs";
import { map, startWith } from "rxjs/operators";
import { toSignal } from "@angular/core/rxjs-interop";

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

	private readonly window = inject(DOCUMENT).defaultView!;
	private readonly isMobile = toSignal(
		fromEvent(this.window, "resize").pipe(
			startWith(null),
			map(() => this.window.innerWidth < 640)
		),
		{ initialValue: this.window.innerWidth < 640 }
	);

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
			this.entries(),
			this.isMobile()
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
