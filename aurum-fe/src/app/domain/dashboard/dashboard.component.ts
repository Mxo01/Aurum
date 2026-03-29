import {
	ChangeDetectionStrategy,
	Component,
	computed,
	DestroyRef,
	inject,
	OnInit,
	signal
} from "@angular/core";
import { CommonModule } from "@angular/common";
import { PrivacyCurrencyPipe } from "../../shared/pipes/privacy-currency.pipe";
import { Skeleton } from "primeng/skeleton";
import { DashboardService } from "./dashboard.service";
import { AnalyticsSummary, ChartData, Projections } from "./model/dashboard.model";
import { NetworthChartComponent } from "./components/networth-chart/networth-chart.component";
import { TopAssetsWidgetComponent } from "./components/top-assets/top-assets-widget.component";
import { TargetWidgetComponent } from "./components/target-widget/target-widget.component";
import { KpiCardComponent } from "./components/kpi-card/kpi-card.component";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { catchError, finalize, forkJoin, of } from "rxjs";
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
		PrivacyCurrencyPipe,
		Skeleton
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
	private readonly destroyRef = inject(DestroyRef);

	readonly isLoading = signal(false);
	readonly userCurrency = signal<Currency>(Currency.EUR);
	readonly userLocale = signal<Locale>(Locale.EN_US);
	readonly summary = signal<AnalyticsSummary | null>(null);
	readonly chartData = signal<ChartData | null>(null);
	readonly projections = signal<Projections | null>(null);
	readonly projectedNetWorth = computed<number>(() => this.projections()?.[1] ?? 0);
	readonly projectedGrowthVariation = computed<number | undefined>(() => {
		const current = this.summary()?.totalGrossAssets;
		const projected = this.projections()?.[1];
		if (!current || !projected || current === 0) return undefined;
		return +(((projected - current) / current) * 100).toFixed(2);
	});
	readonly targets = signal<Target[]>([]);
	readonly topAssets = signal<Asset[]>([]);
	readonly totalLiabilities = computed<number>(() => this.summary()?.totalLiabilities ?? 0);
	readonly currencyImpact = computed<number>(() => this.summary()?.currencyImpact ?? 0);
	readonly assetGrowth1Y = computed<number>(
		() => this.summary()?.assetVariations.oneYear.absolute ?? 0
	);
	readonly assetGrowth1YVariation = computed<number | undefined>(() => {
		const pct = this.summary()?.assetVariations.oneYear.percentage;
		return pct !== undefined ? +pct.toFixed(2) : undefined;
	});
	readonly debtToAssetRatio = computed<number>(() => this.summary()?.debtToAssetRatio ?? 0);
	readonly avgMonthlyAbsolute = computed<number | undefined>(() => {
		const abs = this.summary()?.assetVariations.oneYear.absolute;
		if (abs === undefined) return undefined;
		return +(abs / 12).toFixed(2);
	});
	readonly avgMonthlyPercent = computed<number | undefined>(() => {
		const pct = this.summary()?.assetVariations.oneYear.percentage;
		if (pct === undefined) return undefined;
		return +((Math.pow(1 + pct / 100, 1 / 12) - 1) * 100).toFixed(2);
	});
	readonly liabilitiesSparkline = computed<number[]>(() => {
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

	loadData() {
		this.isLoading.set(true);
		forkJoin({
			profile: this.profileService.getProfile().pipe(catchError(() => of(null))),
			summary: this.dashboardService.getSummary().pipe(catchError(() => of(null))),
			chart: this.dashboardService.getChartData().pipe(catchError(() => of(null))),
			targets: this.targetService.getTargets().pipe(catchError(() => of([]))),
			projections: this.dashboardService.getProjections().pipe(catchError(() => of(null)))
		})
			.pipe(
				takeUntilDestroyed(this.destroyRef),
				finalize(() => this.isLoading.set(false))
			)
			.subscribe({
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

	navigateToAssets() {
		this.router.navigate(["/assets"]);
	}

	onYearChanged(year: number | null) {
		if (year === null) {
			this.loadData();
		} else {
			this.dashboardService
				.getChartDataForYear(year)
				.pipe(
					catchError(() => of(null)),
					takeUntilDestroyed(this.destroyRef)
				)
				.subscribe(chartData => {
					this.chartData.set(chartData);
				});
		}
	}
}
