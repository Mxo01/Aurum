import {
	ChangeDetectionStrategy,
	Component,
	computed,
	inject,
	input,
	model,
	signal
} from "@angular/core";
import { CommonModule, CurrencyPipe, DecimalPipe } from "@angular/common";
import { KnobModule } from "primeng/knob";
import { ButtonModule } from "primeng/button";
import { FormsModule } from "@angular/forms";
import { Target } from "../../../target/model/target.model";
import { Card } from "primeng/card";
import { TargetFormComponent } from "../../../target/components/target-form/target-form.component";
import { TargetListComponent } from "../../../target/components/target-list/target-list.component";
import { TargetService } from "../../../target/target.service";
import { finalize } from "rxjs";
import { Menu } from "primeng/menu";
import { MenuItem } from "primeng/api";
import { Currency } from "../../../profile/model/currency.model";
import { ThemeService } from "../../../../shared/services/theme/theme.service";

@Component({
	selector: "app-target-widget",
	standalone: true,
	imports: [
		CommonModule,
		KnobModule,
		ButtonModule,
		FormsModule,
		CurrencyPipe,
		DecimalPipe,
		Card,
		TargetFormComponent,
		TargetListComponent,
		Menu
	],
	templateUrl: "./target-widget.component.html",
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class TargetWidgetComponent {
	protected readonly themeService = inject(ThemeService);
	private readonly targetService = inject(TargetService);

	targets = model.required<Target[]>();
	currency = input.required<Currency>();

	protected readonly menuItems = computed<MenuItem[]>(() => [
		{
			label: this.activeTarget() ? "Edit Target" : "Set Target",
			icon: this.activeTarget() ? "pi pi-pencil" : "pi pi-plus",
			command: () =>
				this.activeTarget() ? this.onEditTarget(this.activeTarget()!) : this.onAddTarget()
		},
		{
			label: "Add Target",
			icon: "pi pi-plus",
			visible: this.activeTarget() !== null,
			command: () => this.onAddTarget()
		},
		{
			label: "View All Targets",
			icon: "pi pi-list",
			command: () => this.isListVisible.set(true)
		}
	]);

	protected readonly isFormVisible = signal(false);
	protected readonly isListVisible = signal(false);
	protected readonly selectedTarget = signal<Target | null>(null);
	protected readonly isSaving = signal(false);

	protected readonly activeTarget = computed(() => {
		const list = this.targets();
		return list.length > 0 ? list[0] : null;
	});

	protected onAddTarget() {
		this.selectedTarget.set(null);
		this.isFormVisible.set(true);
	}

	protected onEditTarget(target: Target) {
		this.selectedTarget.set(target);
		this.isFormVisible.set(true);
	}

	protected onSaveTarget(target: Target) {
		this.isSaving.set(true);

		this.targetService
			.saveTarget(target)
			.pipe(finalize(() => this.isSaving.set(false)))
			.subscribe({
				next: targets => {
					this.targets.set(targets);
					this.isFormVisible.set(false);
				}
			});
	}

	protected onDeleteTarget(id: string) {
		this.targetService.deleteTarget(id).subscribe({
			next: targets => this.targets.set(targets)
		});
	}
}
