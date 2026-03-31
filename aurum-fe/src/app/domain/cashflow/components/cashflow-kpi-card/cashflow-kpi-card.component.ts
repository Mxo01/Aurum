import { ChangeDetectionStrategy, Component, computed, input } from "@angular/core";
import { Skeleton } from "primeng/skeleton";
import { ProgressBar } from "primeng/progressbar";

@Component({
	selector: "app-cashflow-kpi-card",
	standalone: true,
	imports: [Skeleton, ProgressBar],
	templateUrl: "./cashflow-kpi-card.component.html",
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class CashflowKpiCardComponent {
	icon = input<string>("");
	title = input<string>();
	yearHint = input<number>();
	isLoading = input<boolean>(false);
	hasSubSkeleton = input<boolean>(false);
	leftLabel = input.required<string>();
	leftValue = input<string>("");
	leftDelta = input<number | null>(null);
	leftDeltaInverted = input<boolean>(false);
	leftValueClass = input<string>("");
	rightLabel = input<string>();
	rightValue = input<string>("");
	rightDelta = input<number | null>(null);
	rightDeltaInverted = input<boolean>(false);
	rightValueClass = input<string>("");
	footerText = input<string>();
	progressValue = input<number | null>(null);

	readonly leftIsPositive = computed(() =>
		this.leftDeltaInverted() ? (this.leftDelta() ?? 0) < 0 : (this.leftDelta() ?? 0) > 0
	);

	readonly rightIsPositive = computed(() =>
		this.rightDeltaInverted() ? (this.rightDelta() ?? 0) < 0 : (this.rightDelta() ?? 0) > 0
	);

	formatDelta(value: number | null): string {
		if (value === null) return "—";
		return `${Math.abs(value).toFixed(2)}%`;
	}
}
