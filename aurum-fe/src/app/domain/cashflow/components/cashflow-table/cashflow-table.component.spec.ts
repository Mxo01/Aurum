import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MockComponent, MockDirective, MockModule, MockPipe, MockProvider } from "ng-mocks";
import { NoArrowSpinDirective } from "../../../../shared/directives/no-arrow-spin.directive";
import { FormsModule } from "@angular/forms";
import { TableModule } from "primeng/table";
import { InputNumber } from "primeng/inputnumber";
import { Tooltip } from "primeng/tooltip";
import { of } from "rxjs";
import { DecimalPipe } from "@angular/common";
import { CashflowTableComponent } from "./cashflow-table.component";
import { CashFlowService } from "../../cashflow.service";
import { PrivacyCurrencyPipe } from "../../../../shared/pipes/privacy-currency.pipe";
import { EditableCellTooltipComponent } from "../../../../shared/components/editable-cell-tooltip/editable-cell-tooltip.component";
import { CashFlowEntry, CashFlowRow } from "../../model/cashflow.model";
import { Currency } from "../../../profile/model/currency.model";
import { Locale } from "../../../profile/model/locale.model";

const buildMockRow = (month: number, earned = 1000, spent = 500): CashFlowRow => ({
	month,
	monthLabel: "JAN",
	earned,
	spent,
	balance: earned - spent,
	prevMonthBalance: null,
	deltaAbsolute: null,
	deltaPercent: null,
	prevYearEarned: null,
	prevYearSpent: null,
	prevYearBalance: null,
	prevYearDeltaAbsolute: null,
	prevYearDeltaPercent: null
});

const buildMockEntry = (month: number, earned = 1000, spent = 500): CashFlowEntry => ({
	id: null,
	month,
	earned,
	spent
});

describe("CashflowTableComponent", () => {
	let fixture: ComponentFixture<CashflowTableComponent>;
	let testSubject: CashflowTableComponent;
	let mockCashFlowService: CashFlowService;

	beforeEach(() => {
		TestBed.configureTestingModule({
			imports: [
				CashflowTableComponent,
				MockModule(FormsModule),
				MockModule(TableModule),
				MockComponent(InputNumber),
				MockDirective(Tooltip),
				MockDirective(NoArrowSpinDirective),
				MockPipe(PrivacyCurrencyPipe),
				MockPipe(DecimalPipe),
				MockComponent(EditableCellTooltipComponent)
			],
			providers: [
				MockProvider(CashFlowService, {
					updateEntry: vi.fn().mockReturnValue(of(buildMockEntry(1)))
				})
			]
		});
		fixture = TestBed.createComponent(CashflowTableComponent);
		testSubject = fixture.componentInstance;
		mockCashFlowService = TestBed.inject(CashFlowService);
		fixture.componentRef.setInput("isLoading", false);
		fixture.componentRef.setInput("year", 2025);
		fixture.componentRef.setInput("currency", Currency.EUR);
		fixture.componentRef.setInput("locale", Locale.EN_US);
		fixture.detectChanges();
	});

	describe("getEditingValue", () => {
		it("should return fallback when no editing value is set", () => {
			// THEN
			expect(testSubject.getEditingValue(1, "earned", 999)).toBe(999);
		});

		it("should return the editing value when one has been set", () => {
			// WHEN
			testSubject.onValueChange(1, "earned", 1500);

			// THEN
			expect(testSubject.getEditingValue(1, "earned", 999)).toBe(1500);
		});

		it("should return fallback for a field that was not edited on an otherwise edited month", () => {
			// WHEN
			testSubject.onValueChange(1, "earned", 1500);

			// THEN
			expect(testSubject.getEditingValue(1, "spent", 999)).toBe(999);
		});

		it("should return fallback for a different month", () => {
			// WHEN
			testSubject.onValueChange(1, "earned", 1500);

			// THEN
			expect(testSubject.getEditingValue(2, "earned", 999)).toBe(999);
		});
	});

	describe("onValueChange", () => {
		it("should update editingValues with the new value", () => {
			// WHEN
			testSubject.onValueChange(3, "spent", 750);

			// THEN
			expect(testSubject.editingValues()["3"]?.spent).toBe(750);
		});

		it("should not update editingValues when value is null", () => {
			// WHEN
			testSubject.onValueChange(3, "spent", null);

			// THEN
			expect(testSubject.editingValues()["3"]).toBeUndefined();
		});

		it("should preserve existing field when updating a different field on the same month", () => {
			// GIVEN
			testSubject.onValueChange(2, "earned", 2000);

			// WHEN
			testSubject.onValueChange(2, "spent", 800);

			// THEN
			expect(testSubject.editingValues()["2"]?.earned).toBe(2000);
			expect(testSubject.editingValues()["2"]?.spent).toBe(800);
		});
	});

	describe("editingValues reset on year change", () => {
		it("should clear editingValues when the year input changes", () => {
			// GIVEN
			testSubject.onValueChange(1, "earned", 2000);
			expect(testSubject.editingValues()["1"]?.earned).toBe(2000);

			// WHEN
			fixture.componentRef.setInput("year", 2024);
			fixture.detectChanges();

			// THEN
			expect(testSubject.editingValues()).toEqual({});
		});
	});

	describe("entryUpdated output", () => {
		it("should emit the updated entry returned by the service on cell blur", () => {
			// GIVEN
			const stubbedEntry = buildMockEntry(1, 1500, 500);
			vi.spyOn(mockCashFlowService, "updateEntry").mockReturnValue(of(stubbedEntry));
			fixture.componentRef.setInput("rows", [buildMockRow(1)]);
			fixture.detectChanges();

			const emittedValues: CashFlowEntry[] = [];
			testSubject.entryUpdated.subscribe(e => emittedValues.push(e));

			// WHEN
			testSubject.onValueChange(1, "earned", 1500);
			testSubject.onCellEditBlur(1, "earned");

			// THEN
			expect(emittedValues[0]).toEqual(stubbedEntry);
		});

		it("should not emit when onCellEditBlur is called with no pending editing value", () => {
			// GIVEN
			const emittedValues: CashFlowEntry[] = [];
			testSubject.entryUpdated.subscribe(e => emittedValues.push(e));

			// WHEN
			testSubject.onCellEditBlur(1, "earned");

			// THEN
			expect(emittedValues).toHaveLength(0);
		});
	});
});
