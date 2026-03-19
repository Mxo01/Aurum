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
import { AnalyticsSummary, ChartData } from "./model/dashboard.model";
import { NetworthChartComponent } from "./components/networth-chart/networth-chart.component";
import { TopAssetsWidgetComponent } from "./components/top-assets/top-assets-widget.component";
import { TargetWidgetComponent } from "./components/target-widget/target-widget.component";
import { KpiCardComponent } from "./components/kpi-card/kpi-card.component";
import { catchError, forkJoin, map, of } from "rxjs";
import { Router } from "@angular/router";
import { Asset } from "../asset/model/asset.model";
import { Currency } from "../profile/model/currency.model";
import { ProfileService } from "../profile/profile.service";
import { Target } from "../target/model/target.model";
import { TargetService } from "../target/target.service";

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

	protected readonly userCurrency = signal<Currency>(Currency.EUR);
	protected readonly summary = signal<AnalyticsSummary | null>(null);
	protected readonly chartData = signal<ChartData | null>(null);
	protected readonly targets = signal<Target[]>([]);
	protected readonly topAssets = signal<Asset[]>([]);
	protected readonly totalLiabilities = computed<number>(
		() => this.summary()?.totalLiabilities ?? 0
	);
	protected readonly liabilitiesSparkline = computed<number[]>(() => {
		const chart = this.chartData();
		if (!chart?.totalAssetsOnly || !chart?.totalNetWorth) return [];
		return chart.totalAssetsOnly.map((a, i) => a - chart.totalNetWorth[i]).slice(-6);
	});

	ngOnInit() {
		this.loadData();
	}

	protected loadData() {
		forkJoin({
			userCurrency: this.profileService.getProfile().pipe(
				map(profile => profile.currency),
				catchError(() => of(Currency.EUR))
			),
			summary: this.dashboardService.getSummary().pipe(catchError(() => of(null))),
			chart: this.dashboardService.getChartData().pipe(catchError(() => of(null))),
			targets: this.targetService.getTargets().pipe(catchError(() => of([])))
		}).subscribe({
			next: data => {
				this.userCurrency.set(data.userCurrency);
				this.summary.set(data.summary);
				this.chartData.set(data.chart);
				this.targets.set(data.targets);
				this.topAssets.set(data.summary?.topAssets || []);
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
