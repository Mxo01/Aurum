import {
	ChangeDetectionStrategy,
	Component,
	computed,
	inject,
	OnInit,
	signal
} from "@angular/core";
import { CommonModule, CurrencyPipe } from "@angular/common";
import { DashboardService } from "./dashboard.service";
import { AnalyticsSummary, ChartData, Projections } from "./model/dashboard.model";
import { NetworthChartComponent } from "./components/networth-chart/networth-chart.component";
import { TopAssetsWidgetComponent } from "./components/top-assets/top-assets-widget.component";
import { TargetWidgetComponent } from "./components/target-widget/target-widget.component";
import { KpiCardComponent } from "./components/kpi-card/kpi-card.component";
import { catchError, forkJoin, of } from "rxjs";
import { Router } from "@angular/router";
import { Asset } from "../asset/model/asset.model";
import { Currency } from "../profile/model/currency.model";
import { ProfileService } from "../profile/profile.service";
import { Locale } from "../profile/model/locale.model";
import { Target } from "../target/model/target.model";
import { TargetService } from "../target/target.service";
import { ThemeService } from "../../shared/services/theme/theme.service";

@Component({
	selector: "app-dashboard",
	standalone: true,
	imports: [
		CommonModule,
		NetworthChartComponent,
		TopAssetsWidgetComponent,
		TargetWidgetComponent,
		KpiCardComponent,
		CurrencyPipe
	],
	templateUrl: "./dashboard.component.html",
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardComponent implements OnInit {
	private readonly profileService = inject(ProfileService);
	private readonly dashboardService = inject(DashboardService);
	private readonly targetService = inject(TargetService);
	private readonly router = inject(Router);
	private readonly themeService = inject(ThemeService);

	protected readonly userCurrency = signal<Currency>(Currency.EUR);
	protected readonly userLocale = signal<Locale>(Locale.EN_US);
	protected readonly summary = signal<AnalyticsSummary | null>(null);
	protected readonly chartData = signal<ChartData | null>(null);
	protected readonly projections = signal<Projections | null>(null);
	protected readonly projectedNetWorth = computed<number>(() => this.projections()?.[1] ?? 0);
	protected readonly projectedGrowthVariation = computed<number | undefined>(() => {
		const current = this.summary()?.totalGrossAssets;
		const projected = this.projections()?.[1];
		if (!current || !projected || current === 0) return undefined;
		return +(((projected - current) / current) * 100).toFixed(2);
	});
	protected readonly targets = signal<Target[]>([]);
	protected readonly topAssets = signal<Asset[]>([]);
	protected readonly totalLiabilities = computed<number>(
		() => this.summary()?.totalLiabilities ?? 0
	);
	protected readonly currencyImpact = computed<number>(() => this.summary()?.currencyImpact ?? 0);
	protected readonly assetGrowth1Y = computed<number>(
		() => this.summary()?.assetVariations.oneYear.absolute ?? 0
	);
	protected readonly assetGrowth1YVariation = computed<number | undefined>(() => {
		const pct = this.summary()?.assetVariations.oneYear.percentage;
		return pct !== undefined ? +pct.toFixed(2) : undefined;
	});
	protected readonly debtToAssetRatio = computed<number>(
		() => this.summary()?.debtToAssetRatio ?? 0
	);
	protected readonly liabilitiesSparkline = computed<number[]>(() => {
		const chart = this.chartData();
		if (!chart?.totalAssetsOnly || !chart?.totalNetWorth) return [];
		const liabilities = chart.totalAssetsOnly.map((a, i) => a - Math.abs(chart.totalNetWorth[i]));
		const deduped = liabilities
			.filter((v, i, arr) => i === 0 || v !== arr[i - 1])
			.map(v => Math.abs(v));
		return deduped;
	});

	ngOnInit() {
		this.loadData();
	}

	protected loadData() {
		forkJoin({
			profile: this.profileService.getProfile().pipe(catchError(() => of(null))),
			summary: this.dashboardService.getSummary().pipe(catchError(() => of(null))),
			chart: this.dashboardService.getChartData().pipe(catchError(() => of(null))),
			targets: this.targetService.getTargets().pipe(catchError(() => of([]))),
			projections: this.dashboardService.getProjections().pipe(catchError(() => of(null)))
		}).subscribe({
			next: data => {
				if (data.profile) {
					this.userCurrency.set(data.profile.currency);
					this.userLocale.set(data.profile.locale);
					this.themeService.applyLocale(data.profile.locale);
				}
				this.summary.set(data.summary);
				this.chartData.set(data.chart);
				this.targets.set(data.targets);
				this.topAssets.set(data.summary?.topAssets || []);
				this.projections.set(data.projections);
			}
		});
	}

	protected navigateToAssets() {
		this.router.navigate(["/assets"]);
	}

	protected onYearChanged(year: number | null) {
		if (year === null) {
			this.loadData();
		} else {
			this.dashboardService
				.getChartDataForYear(year)
				.pipe(catchError(() => of(null)))
				.subscribe(chartData => {
					this.chartData.set(chartData);
				});
		}
	}
}
