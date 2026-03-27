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
import { PrivacyService } from "../../../../shared/services/privacy/privacy.service";
import { PrivacyCurrencyPipe } from "../../../../shared/pipes/privacy-currency.pipe";

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
		Button,
		PrivacyCurrencyPipe
	],
	templateUrl: "./networth-chart.component.html",
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class NetworthChartComponent {
	private readonly themeService = inject(ThemeService);
	private readonly privacyService = inject(PrivacyService);

	data = input<ChartData | null>(null);
	summary = input<AnalyticsSummary | null>(null);
	minYear = input<number | null>(null);
	currency = input.required<Currency>();
	locale = input<string>(Locale.EN_US);

	yearChanged = output<number | null>();

	readonly percentageView = signal<boolean>(true);
	readonly selectedDate = signal<Date | null>(null);
	readonly minDate = computed(() => {
		const year = this.minYear();
		return new Date(year ?? new Date().getFullYear(), 0, 1);
	});
	readonly maxDate = signal<Date>(new Date());
	readonly isSpecificYearSelected = computed(() => this.selectedDate() !== null);
	readonly totalAmount = computed(() => this.summary()?.totalGrossAssets ?? 0);
	readonly variation = computed(() => this.summary()?.assetVariations?.[this.selectedPeriod()]);
	readonly hasEnoughDataToDisplayChart = computed(() =>
		this.data()?.totalAssetsOnly?.some(value => value !== 0)
	);
	readonly timePeriods = signal<SelectItem<keyof Variation>[]>([
		{ label: "1M", value: "oneMonth" },
		{ label: "6M", value: "sixMonths" },
		{ label: "1Y", value: "oneYear" }
	]);
	readonly selectedPeriod = signal<keyof Variation>("oneMonth");
	readonly chartData = computed(() => mapDataIntoNetworthChartData(this.data()));
	readonly chartOptions = computed(() =>
		getNetworthChartOptions(
			getCurrencySymbol(this.currency()),
			this.themeService.isDarkMode(),
			this.locale(),
			this.privacyService.isPrivacyMode()
		)
	);

	onDateChange() {
		const date = this.selectedDate();
		const year = date ? date.getFullYear() : null;
		this.yearChanged.emit(year);
	}
}
