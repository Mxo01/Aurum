import { ChangeDetectionStrategy, Component, input, model, output } from "@angular/core";
import { CommonModule, CurrencyPipe, DecimalPipe } from "@angular/common";
import { TableModule } from "primeng/table";
import { Tag } from "primeng/tag";
import { Button } from "primeng/button";
import { Dialog } from "primeng/dialog";
import { Target, TargetStatus } from "../../model/target.model";
import { Currency } from "../../../profile/model/currency.model";
import { Locale } from "../../../profile/model/locale.model";
import { ProgressBar } from "primeng/progressbar";

@Component({
	selector: "app-target-list",
	standalone: true,
	templateUrl: "./target-list.component.html",
	imports: [CommonModule, TableModule, Tag, Button, Dialog, ProgressBar, CurrencyPipe, DecimalPipe],
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class TargetListComponent {
	isVisible = model.required<boolean>();
	targets = input.required<Target[]>();
	currency = input.required<Currency>();
	locale = input<Locale>(Locale.EN_US);

	edit = output<Target>();
	delete = output<string>();

	protected statusLabel(status?: TargetStatus): string {
		switch (status) {
			case TargetStatus.COMPLETED:
				return "Completed";
			case TargetStatus.IN_PROGRESS:
				return "In Progress";
			case TargetStatus.NOT_REACHED:
				return "Not Reached";
			default:
				return "Unknown";
		}
	}

	protected statusSeverity(status?: TargetStatus): "success" | "secondary" | "danger" | "warn" {
		switch (status) {
			case TargetStatus.COMPLETED:
				return "success";
			case TargetStatus.IN_PROGRESS:
				return "secondary";
			case TargetStatus.NOT_REACHED:
				return "danger";
			default:
				return "warn";
		}
	}
}
