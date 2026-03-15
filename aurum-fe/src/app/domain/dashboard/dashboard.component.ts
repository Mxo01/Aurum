import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from "@angular/core";
import { CommonModule } from "@angular/common";
import { DashboardService } from "./dashboard.service";
import { AssetService } from "../asset/asset.service";
import { AnalyticsSummary, ChartData, Target } from "./model/dashboard.model";
import { KpiCardComponent } from "./components/kpi-card/kpi-card.component";
import { NetworthChartComponent } from "./components/networth-chart/networth-chart.component";
import { TopAssetsWidgetComponent } from "./components/top-assets/top-assets-widget.component";
import { TargetWidgetComponent } from "./components/target-widget/target-widget.component";
import { catchError, forkJoin, map, of } from "rxjs";
import { Router } from "@angular/router";
import { Asset } from "../asset/model/asset.model";
import { ChartModule } from "primeng/chart";
import { Currency } from "../profile/model/currency.model";
import { ProfileService } from "../profile/profile.service";
import { AssetAllocationChartComponent } from "./components/asset-allocation-chart/asset-allocation-chart.component";

@Component({
	selector: "app-dashboard",
	standalone: true,
	imports: [
		CommonModule,
		KpiCardComponent,
		NetworthChartComponent,
		TopAssetsWidgetComponent,
		TargetWidgetComponent,
		ChartModule,
		AssetAllocationChartComponent
	],
	templateUrl: "./dashboard.component.html",
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardComponent implements OnInit {
	private readonly profileService = inject(ProfileService);
	private readonly dashboardService = inject(DashboardService);
	private readonly assetService = inject(AssetService);
	private readonly router = inject(Router);

	protected readonly userCurrency = signal<Currency>(Currency.EUR);
	protected readonly summary = signal<AnalyticsSummary | null>(null);
	protected readonly chartData = signal<ChartData | null>(null);
	protected readonly targets = signal<Target[]>([]);
	protected readonly topAssets = signal<Asset[]>([]);

	ngOnInit() {
		forkJoin({
			userCurrency: this.profileService.getProfile().pipe(
				map(profile => profile.currency),
				catchError(() => of(Currency.EUR))
			),
			summary: this.dashboardService.getSummary().pipe(catchError(() => of(null))),
			chart: this.dashboardService.getChartData().pipe(catchError(() => of(null))),
			targets: this.dashboardService.getTargets().pipe(catchError(() => of([]))),
			assets: this.assetService.getAssets().pipe(
				catchError(() => of([])),
				map((assets: Asset[]) =>
					assets
						.toSorted((a, b) => {
							const valA = a.snapshots?.[0]?.amountOriginalCurrency || 0;
							const valB = b.snapshots?.[0]?.amountOriginalCurrency || 0;
							return valB - valA;
						})
						.slice(0, 5)
				)
			)
		}).subscribe({
			next: (data: {
				userCurrency: Currency;
				summary: AnalyticsSummary | null;
				chart: ChartData | null;
				targets: Target[];
				assets: Asset[];
			}) => {
				this.userCurrency.set(data.userCurrency);
				this.summary.set(data.summary);
				this.chartData.set(data.chart);
				this.targets.set(data.targets);
				this.topAssets.set(data.assets);
			}
		});
	}

	protected navigateToAssets() {
		this.router.navigate(["/assets"]);
	}

	protected handleUpdateTarget() {
		console.log("Update target clicked");
	}
}
