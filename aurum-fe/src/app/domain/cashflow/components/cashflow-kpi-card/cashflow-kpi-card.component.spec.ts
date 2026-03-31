import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MockComponent } from "ng-mocks";
import { Skeleton } from "primeng/skeleton";
import { ProgressBar } from "primeng/progressbar";
import { CashflowKpiCardComponent } from "./cashflow-kpi-card.component";

describe("CashflowKpiCardComponent", () => {
	let fixture: ComponentFixture<CashflowKpiCardComponent>;
	let testSubject: CashflowKpiCardComponent;

	beforeEach(() => {
		TestBed.configureTestingModule({
			imports: [CashflowKpiCardComponent, MockComponent(Skeleton), MockComponent(ProgressBar)]
		});
		fixture = TestBed.createComponent(CashflowKpiCardComponent);
		testSubject = fixture.componentInstance;
		fixture.componentRef.setInput("leftLabel", "Total");
		fixture.detectChanges();
	});

	describe("leftIsPositive", () => {
		it("should return true when leftDelta is positive and not inverted", () => {
			// GIVEN
			fixture.componentRef.setInput("leftDelta", 5);
			fixture.componentRef.setInput("leftDeltaInverted", false);
			fixture.detectChanges();

			// THEN
			expect(testSubject.leftIsPositive()).toBe(true);
		});

		it("should return true when leftDelta is zero and not inverted", () => {
			// GIVEN
			fixture.componentRef.setInput("leftDelta", 0);
			fixture.componentRef.setInput("leftDeltaInverted", false);
			fixture.detectChanges();

			// THEN
			expect(testSubject.leftIsPositive()).toBe(true);
		});

		it("should return false when leftDelta is negative and not inverted", () => {
			// GIVEN
			fixture.componentRef.setInput("leftDelta", -3);
			fixture.componentRef.setInput("leftDeltaInverted", false);
			fixture.detectChanges();

			// THEN
			expect(testSubject.leftIsPositive()).toBe(false);
		});

		it("should return true when leftDelta is negative and inverted", () => {
			// GIVEN
			fixture.componentRef.setInput("leftDelta", -3);
			fixture.componentRef.setInput("leftDeltaInverted", true);
			fixture.detectChanges();

			// THEN
			expect(testSubject.leftIsPositive()).toBe(true);
		});

		it("should return false when leftDelta is positive and inverted", () => {
			// GIVEN
			fixture.componentRef.setInput("leftDelta", 5);
			fixture.componentRef.setInput("leftDeltaInverted", true);
			fixture.detectChanges();

			// THEN
			expect(testSubject.leftIsPositive()).toBe(false);
		});

		it("should default to true when leftDelta is null", () => {
			// GIVEN
			fixture.componentRef.setInput("leftDelta", null);
			fixture.detectChanges();

			// THEN
			expect(testSubject.leftIsPositive()).toBe(true);
		});
	});

	describe("rightIsPositive", () => {
		it("should return true when rightDelta is positive and not inverted", () => {
			// GIVEN
			fixture.componentRef.setInput("rightDelta", 10);
			fixture.componentRef.setInput("rightDeltaInverted", false);
			fixture.detectChanges();

			// THEN
			expect(testSubject.rightIsPositive()).toBe(true);
		});

		it("should return false when rightDelta is negative and not inverted", () => {
			// GIVEN
			fixture.componentRef.setInput("rightDelta", -2);
			fixture.componentRef.setInput("rightDeltaInverted", false);
			fixture.detectChanges();

			// THEN
			expect(testSubject.rightIsPositive()).toBe(false);
		});

		it("should return true when rightDelta is negative and inverted", () => {
			// GIVEN
			fixture.componentRef.setInput("rightDelta", -7);
			fixture.componentRef.setInput("rightDeltaInverted", true);
			fixture.detectChanges();

			// THEN
			expect(testSubject.rightIsPositive()).toBe(true);
		});

		it("should return false when rightDelta is positive and inverted", () => {
			// GIVEN
			fixture.componentRef.setInput("rightDelta", 8);
			fixture.componentRef.setInput("rightDeltaInverted", true);
			fixture.detectChanges();

			// THEN
			expect(testSubject.rightIsPositive()).toBe(false);
		});

		it("should default to true when rightDelta is null", () => {
			// GIVEN
			fixture.componentRef.setInput("rightDelta", null);
			fixture.detectChanges();

			// THEN
			expect(testSubject.rightIsPositive()).toBe(true);
		});
	});

	describe("formatDelta", () => {
		it("should return em dash when value is null", () => {
			// THEN
			expect(testSubject.formatDelta(null)).toBe("—");
		});

		it("should format positive value as absolute percentage with two decimals", () => {
			// THEN
			expect(testSubject.formatDelta(12.5)).toBe("12.50%");
		});

		it("should format negative value as absolute percentage without sign", () => {
			// THEN
			expect(testSubject.formatDelta(-8.75)).toBe("8.75%");
		});

		it("should format zero as percentage", () => {
			// THEN
			expect(testSubject.formatDelta(0)).toBe("0.00%");
		});
	});
});
