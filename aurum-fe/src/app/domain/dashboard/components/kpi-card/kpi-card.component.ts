import { ChangeDetectionStrategy, Component, computed, input } from "@angular/core";
import { CommonModule } from "@angular/common";
import { ChartModule } from "primeng/chart";
import { createGradientFill } from "../../../../shared/utils";

@Component({
	selector: "app-kpi-card",
	standalone: true,
	imports: [CommonModule, ChartModule],
	templateUrl: "./kpi-card.component.html",
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class KpiCardComponent {
	label = input.required<string>();
	value = input.required<string | number>();
	variation = input<number>();
	subtext = input<string>();
	invertVariation = input<boolean>(false);
	sparklineData = input<number[]>();

	readonly isPositive = computed(() =>
		this.invertVariation() ? (this.variation() ?? 0) < 0 : (this.variation() ?? 0) >= 0
	);

	readonly sparklineColor = computed(() => {
		const data = this.sparklineData();
		if (!data || data.length < 2) return "#6b7280";
		const diff = data[data.length - 1] - data[0];
		const isGood = this.invertVariation() ? diff <= 0 : diff >= 0;
		return isGood ? "#22c55e" : "#ef4444";
	});

	readonly sparklineChartData = computed(() => {
		const color = this.sparklineColor();
		return {
			labels: (this.sparklineData() ?? []).map(() => ""),
			datasets: [
				{
					data: this.sparklineData() ?? [],
					borderColor: color,
					borderWidth: 1.5,
					tension: 0,
					pointRadius: 0,
					fill: true,
					backgroundColor: createGradientFill(color)
				}
			]
		};
	});

	readonly sparklineOptions = {
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
