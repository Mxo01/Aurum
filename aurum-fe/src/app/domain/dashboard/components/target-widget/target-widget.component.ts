import {
	ChangeDetectionStrategy,
	Component,
	computed,
	inject,
	input,
	model,
	signal
} from "@angular/core";
import { CommonModule, DecimalPipe } from "@angular/common";
import { PrivacyCurrencyPipe } from "../../../../shared/pipes/privacy-currency.pipe";
import { KnobModule } from "primeng/knob";
import { ButtonModule } from "primeng/button";
import { FormsModule } from "@angular/forms";
import { Target } from "../../../target/model/target.model";
import { TargetFormComponent } from "../../../target/components/target-form/target-form.component";
import { TargetListComponent } from "../../../target/components/target-list/target-list.component";
import { TargetService } from "../../../target/target.service";
import { finalize } from "rxjs";
import { Menu } from "primeng/menu";
import { MenuItem } from "primeng/api";
import { Currency } from "../../../profile/model/currency.model";
import { Locale } from "../../../profile/model/locale.model";
import { ThemeService } from "../../../../shared/services/theme/theme.service";

@Component({
	selector: "app-target-widget",
	standalone: true,
	imports: [
		CommonModule,
		KnobModule,
		ButtonModule,
		FormsModule,
		PrivacyCurrencyPipe,
		DecimalPipe,
		TargetFormComponent,
		TargetListComponent,
		Menu
	],
	templateUrl: "./target-widget.component.html",
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class TargetWidgetComponent {
	readonly themeService = inject(ThemeService);
	private readonly targetService = inject(TargetService);

	targets = model.required<Target[]>();
	currency = input.required<Currency>();
	locale = input<Locale>(Locale.EN_US);

	readonly menuItems = computed<MenuItem[]>(() => [
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

	readonly isFormVisible = signal(false);
	readonly isListVisible = signal(false);
	readonly selectedTarget = signal<Target | null>(null);
	readonly isSaving = signal(false);

	readonly activeTarget = computed(() => {
		const list = this.targets();
		return list.length > 0 ? list[0] : null;
	});

	onAddTarget() {
		this.selectedTarget.set(null);
		this.isFormVisible.set(true);
	}

	onEditTarget(target: Target) {
		this.selectedTarget.set(target);
		this.isFormVisible.set(true);
	}

	onSaveTarget(target: Target) {
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

	onDeleteTarget(id: string) {
		this.targetService.deleteTarget(id).subscribe({
			next: targets => this.targets.set(targets)
		});
	}
}
