import {
	ChangeDetectionStrategy,
	Component,
	input,
	signal,
	computed,
	inject,
	output
} from "@angular/core";
import { CommonModule, DecimalPipe } from "@angular/common";
import { ChartModule } from "primeng/chart";
import { SelectButtonModule } from "primeng/selectbutton";
import { DatePicker } from "primeng/datepicker";
import { FormsModule } from "@angular/forms";
import { AnalyticsSummary, ChartData, Variation } from "../../model/dashboard.model";
import { Card } from "primeng/card";
import { Currency } from "../../../profile/model/currency.model";
import { Locale } from "../../../profile/model/locale.model";
import { SelectItem } from "primeng/api";
import { getNetworthChartOptions, mapDataIntoNetworthChartData } from "./networth-chart.utils";
import { getCurrencySymbol } from "../../../profile/profile.utils";
import { Button } from "primeng/button";
import { ThemeService } from "../../../../shared/services/theme/theme.service";

@Component({
	selector: "app-networth-chart",
	standalone: true,
	imports: [
		CommonModule,
		ChartModule,
		SelectButtonModule,
		DatePicker,
		FormsModule,
		Card,
		DecimalPipe,
		Button
	],
	templateUrl: "./networth-chart.component.html",
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class NetworthChartComponent {
	private readonly themeService = inject(ThemeService);

	data = input<ChartData | null>(null);
	summary = input<AnalyticsSummary | null>(null);
	currency = input.required<Currency>();
	locale = input<string>(Locale.EN_US);

	yearChanged = output<number | null>();

	protected readonly percentageView = signal<boolean>(true);
	protected readonly selectedDate = signal<Date | null>(null);
	protected readonly minDate = signal<Date>(new Date());
	protected readonly maxDate = signal<Date>(new Date());
	protected readonly isSpecificYearSelected = computed(() => this.selectedDate() !== null);
	protected readonly totalAmount = computed(() => this.summary()?.totalGrossAssets ?? 0);
	protected readonly variation = computed(
		() => this.summary()?.assetVariations?.[this.selectedPeriod()]
	);
	protected readonly hasEnoughDataToDisplayChart = computed(() =>
		this.data()?.totalAssetsOnly?.some(value => value !== 0)
	);
	protected readonly timePeriods = signal<SelectItem<keyof Variation>[]>([
		{ label: "1M", value: "oneMonth" },
		{ label: "6M", value: "sixMonths" },
		{ label: "1Y", value: "oneYear" }
	]);
	protected readonly selectedPeriod = signal<keyof Variation>("oneMonth");
	protected readonly chartData = computed(() => mapDataIntoNetworthChartData(this.data()));
	protected readonly chartOptions = computed(() =>
		getNetworthChartOptions(
			getCurrencySymbol(this.currency()),
			this.themeService.isDarkMode(),
			this.locale()
		)
	);

	protected onDateChange() {
		const date = this.selectedDate();
		const year = date ? date.getFullYear() : null;
		this.yearChanged.emit(year);
	}
}
