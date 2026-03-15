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
	templateUrl: "./target-widget.component.html",
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class TargetWidgetComponent {
	target = input<Target | null>(null);
	currency = input.required<string>();
	updateTarget = output<void>();
}
