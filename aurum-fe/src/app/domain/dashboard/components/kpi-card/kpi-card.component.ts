import { ChangeDetectionStrategy, Component, computed, inject, input } from "@angular/core";
import { CommonModule } from "@angular/common";
import { Card } from "primeng/card";
import { ChartModule } from "primeng/chart";
import { ThemeService } from "../../../../shared/services/theme/theme.service";

@Component({
	selector: "app-kpi-card",
	standalone: true,
	imports: [CommonModule, Card, ChartModule],
	template: `
		<p-card class="rounded-xl h-full flex flex-col p-2 gap-2">
			<span class="text-secondary font-medium">{{ label() }}</span>
			<div class="flex items-center justify-between gap-2">
				<span class="text-2xl font-bold">{{ value() }}</span>
				@if (sparklineData() && sparklineData()!.length > 1) {
					<div style="width: 80px; height: 36px; flex-shrink: 0;">
						<p-chart
							type="line"
							[data]="sparklineChartData()"
							[options]="sparklineOptions"
							height="36px"
							width="80px"
						/>
					</div>
				} @else if (variation() !== undefined) {
					<span
						[class]="isPositive() ? 'text-green-500' : 'text-red-500'"
						class="font-medium"
					>
						{{ variation()! > 0 ? "+" : "" }}{{ variation() }}%
					</span>
				}
			</div>
			@if (subtext()) {
				<span class="text-sm text-secondary">{{ subtext() }}</span>
			}
		</p-card>
	`,
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class KpiCardComponent {
	private readonly themeService = inject(ThemeService);

	label = input.required<string>();
	value = input.required<string | number>();
	variation = input<number>();
	subtext = input<string>();
	invertVariation = input<boolean>(false);
	sparklineData = input<number[]>();

	protected readonly isPositive = computed(() =>
		this.invertVariation() ? (this.variation() ?? 0) < 0 : (this.variation() ?? 0) >= 0
	);

	protected readonly sparklineColor = computed(() => {
		const data = this.sparklineData();
		if (!data || data.length < 2) return "#6b7280";
		const diff = data[data.length - 1] - data[0];
		const isGood = this.invertVariation() ? diff <= 0 : diff >= 0;
		return isGood ? "#22c55e" : "#ef4444";
	});

	protected readonly sparklineChartData = computed(() => ({
		labels: (this.sparklineData() ?? []).map(() => ""),
		datasets: [
			{
				data: this.sparklineData() ?? [],
				borderColor: this.sparklineColor(),
				borderWidth: 1.5,
				tension: 0,
				pointRadius: 0,
				fill: false
			}
		]
	}));

	protected readonly sparklineOptions = {
		responsive: false,
		animation: false,
		plugins: {
			legend: { display: false },
			tooltip: { enabled: false }
		},
		scales: {
			x: { display: false },
			y: { display: false }
		}
	};
}
