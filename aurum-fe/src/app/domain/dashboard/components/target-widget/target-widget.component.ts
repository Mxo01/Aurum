import { ChangeDetectionStrategy, Component, input, output } from "@angular/core";
import { CommonModule } from "@angular/common";
import { KnobModule } from "primeng/knob";
import { ButtonModule } from "primeng/button";
import { FormsModule } from "@angular/forms";
import { Target } from "../../model/dashboard.model";
import { Card } from "primeng/card";

@Component({
	selector: "app-target-widget",
	standalone: true,
	imports: [CommonModule, KnobModule, ButtonModule, FormsModule, Card],
	template: `
		<p-card class="rounded-xl p-6 h-full flex flex-col items-center justify-between">
			<div class="w-full flex justify-between items-center mb-4">
				<span class="text-xl font-bold">Financial Target</span>
				<p-button
					icon="pi pi-pencil"
					[rounded]="true"
					[text]="true"
					(onClick)="updateTarget.emit()"
				/>
			</div>

			@if (target()) {
				<div class="flex flex-col items-center gap-4">
					<p-knob
						[ngModel]="target()!.completionPercentage"
						[readonly]="true"
						valueTemplate="{value}%"
						[size]="150"
						valueColor="#42A5F5"
						rangeColor="var(--p-surface-200)"
					/>

					<div class="text-center">
						<div class="text-2xl font-bold mb-1">{{ target()!.name }}</div>
						<div class="text-secondary mb-2 font-mono">
							{{ target()!.currentAmount | currency: currency() }} /
							{{ target()!.targetAmount | currency: currency() }}
						</div>
						<div class="text-sm text-secondary">Deadline: {{ target()!.deadline | date }}</div>
					</div>
				</div>
			} @else {
				<div class="grow flex flex-col items-center justify-center gap-3">
					<i class="pi pi-flag text-4xl text-secondary"></i>
					<span class="text-secondary">No target set</span>
					<p-button
						label="Set Target"
						(onClick)="updateTarget.emit()"
					/>
				</div>
			}
		</p-card>
	`,
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class TargetWidgetComponent {
	target = input<Target | null>(null);
	currency = input<string>("EUR");
	updateTarget = output<void>();
}
