import { ChangeDetectionStrategy, Component, input } from "@angular/core";
import { CommonModule } from "@angular/common";
import { Card } from "primeng/card";

@Component({
	selector: "app-kpi-card",
	standalone: true,
	imports: [CommonModule, Card],
	template: `
		<p-card class="rounded-xl h-full flex flex-col">
			<div class="flex flex-col h-full p-6 gap-2">
				<span class="text-secondary font-medium">{{ label() }}</span>
				<div class="flex items-center justify-between">
					<span class="text-2xl font-bold">{{ value() }}</span>
					@if (variation() !== undefined) {
						<span
							[class]="variation()! >= 0 ? 'text-green-500' : 'text-red-500'"
							class="font-medium"
						>
							{{ variation()! > 0 ? "+" : "" }}{{ variation() }}%
						</span>
					}
				</div>
				@if (subtext()) {
					<span class="text-sm text-secondary">{{ subtext() }}</span>
				}
			</div>
		</p-card>
	`,
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class KpiCardComponent {
	label = input.required<string>();
	value = input.required<string | number>();
	variation = input<number>();
	subtext = input<string>();
}
