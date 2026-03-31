import {
	ChangeDetectionStrategy,
	Component,
	DestroyRef,
	effect,
	inject,
	input,
	OnInit,
	output,
	signal
} from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { FormsModule } from "@angular/forms";
import { debounceTime, distinctUntilChanged, Subject } from "rxjs";
import { TableModule } from "primeng/table";
import { InputNumber } from "primeng/inputnumber";
import { Tooltip } from "primeng/tooltip";
import { CashFlowEntry, CashFlowRow } from "../../model/cashflow.model";
import { Currency } from "../../../profile/model/currency.model";
import { Locale } from "../../../profile/model/locale.model";
import { CashFlowService } from "../../cashflow.service";
import { PrivacyCurrencyPipe } from "../../../../shared/pipes/privacy-currency.pipe";
import { DecimalPipe } from "@angular/common";
import { EditableCellTooltipComponent } from "../../../../shared/components/editable-cell-tooltip/editable-cell-tooltip.component";

@Component({
	selector: "app-cashflow-table",
	standalone: true,
	imports: [
		FormsModule,
		TableModule,
		InputNumber,
		Tooltip,
		PrivacyCurrencyPipe,
		DecimalPipe,
		EditableCellTooltipComponent
	],
	templateUrl: "./cashflow-table.component.html",
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class CashflowTableComponent implements OnInit {
	private readonly cashFlowService = inject(CashFlowService);
	private readonly destroyRef = inject(DestroyRef);

	rows = input<CashFlowRow[]>([]);
	isLoading = input.required<boolean>();
	hasPrevYearData = input<boolean>(false);
	year = input.required<number>();
	currency = input.required<Currency>();
	locale = input.required<Locale>();
	totalEarned = input<number>(0);
	totalSpent = input<number>(0);
	totalBalance = input<number>(0);
	prevYearTotalEarned = input<number | null>(null);
	prevYearTotalSpent = input<number | null>(null);

	entryUpdated = output<CashFlowEntry>();

	readonly editingValues = signal<Record<string, { earned?: number; spent?: number }>>({});

	private readonly updateTrigger$ = new Subject<{
		month: number;
		field: "earned" | "spent";
		value: number;
	}>();

	constructor() {
		effect(() => {
			this.year();
			this.editingValues.set({});
		});
	}

	ngOnInit() {
		this.updateTrigger$
			.pipe(
				debounceTime(600),
				distinctUntilChanged(
					(a, b) => a.month === b.month && a.field === b.field && a.value === b.value
				),
				takeUntilDestroyed(this.destroyRef)
			)
			.subscribe({ next: ({ month, field, value }) => this.saveEntry(month, field, value) });
	}

	onValueChange(month: number, field: "earned" | "spent", value: number | null) {
		if (value === null) return;
		const key = `${month}`;
		this.editingValues.update(v => ({ ...v, [key]: { ...v[key], [field]: value } }));
		this.updateTrigger$.next({ month, field, value });
	}

	getEditingValue(month: number, field: "earned" | "spent", fallback: number): number {
		return this.editingValues()[`${month}`]?.[field] ?? fallback;
	}

	private saveEntry(month: number, field: "earned" | "spent", value: number) {
		const row = this.rows().find(r => r.month === month);
		const dto =
			field === "earned"
				? { earned: value, spent: row?.spent ?? 0 }
				: { earned: row?.earned ?? 0, spent: value };

		this.cashFlowService.updateEntry(this.year(), month, dto).subscribe({
			next: updated => this.entryUpdated.emit(updated)
		});
	}
}
