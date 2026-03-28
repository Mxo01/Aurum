import { ComponentFixture, TestBed } from "@angular/core/testing";
import { faker } from "@faker-js/faker";
import { MockModule } from "ng-mocks";
import { CommonModule } from "@angular/common";
import { ChartModule } from "primeng/chart";
import { KpiCardComponent } from "./kpi-card.component";

describe("KpiCardComponent", () => {
	let fixture: ComponentFixture<KpiCardComponent>;
	let testSubject: KpiCardComponent;

	beforeEach(() => {
		TestBed.configureTestingModule({
			imports: [KpiCardComponent, MockModule(CommonModule), MockModule(ChartModule)]
		});
		fixture = TestBed.createComponent(KpiCardComponent);
		testSubject = fixture.componentInstance;
		fixture.componentRef.setInput("label", faker.lorem.word());
		fixture.componentRef.setInput("value", faker.number.float());
	});

	describe("isPositive", () => {
		it("should be true when variation is positive and invertVariation is false", () => {
			// GIVEN
			fixture.componentRef.setInput("variation", 5);
			fixture.componentRef.setInput("invertVariation", false);
			fixture.detectChanges();

			// THEN
			expect(testSubject.isPositive()).toBe(true);
		});

		it("should be false when variation is negative and invertVariation is false", () => {
			// GIVEN
			fixture.componentRef.setInput("variation", -5);
			fixture.componentRef.setInput("invertVariation", false);
			fixture.detectChanges();

			// THEN
			expect(testSubject.isPositive()).toBe(false);
		});

		it("should be false when variation is positive and invertVariation is true", () => {
			// GIVEN
			fixture.componentRef.setInput("variation", 5);
			fixture.componentRef.setInput("invertVariation", true);
			fixture.detectChanges();

			// THEN
			expect(testSubject.isPositive()).toBe(false);
		});

		it("should be true when variation is negative and invertVariation is true", () => {
			// GIVEN
			fixture.componentRef.setInput("variation", -5);
			fixture.componentRef.setInput("invertVariation", true);
			fixture.detectChanges();

			// THEN
			expect(testSubject.isPositive()).toBe(true);
		});
	});

	describe("sparklineColor", () => {
		it("should be green when the last value is greater than the first", () => {
			// GIVEN
			fixture.componentRef.setInput("sparklineData", [100, 200, 300]);
			fixture.componentRef.setInput("invertVariation", false);
			fixture.detectChanges();

			// THEN
			expect(testSubject.sparklineColor()).toBe("#22c55e");
		});

		it("should be red when the last value is less than the first", () => {
			// GIVEN
			fixture.componentRef.setInput("sparklineData", [300, 200, 100]);
			fixture.componentRef.setInput("invertVariation", false);
			fixture.detectChanges();

			// THEN
			expect(testSubject.sparklineColor()).toBe("#ef4444");
		});

		it("should be gray when sparklineData has fewer than 2 points", () => {
			// GIVEN
			fixture.componentRef.setInput("sparklineData", [100]);
			fixture.detectChanges();

			// THEN
			expect(testSubject.sparklineColor()).toBe("#6b7280");
		});

		it("should be green when trend is down and invertVariation is true", () => {
			// GIVEN
			fixture.componentRef.setInput("sparklineData", [300, 200, 100]);
			fixture.componentRef.setInput("invertVariation", true);
			fixture.detectChanges();

			// THEN
			expect(testSubject.sparklineColor()).toBe("#22c55e");
		});
	});

	describe("sparklineChartData", () => {
		it("should return a dataset with the sparkline data and the computed border color", () => {
			// GIVEN
			fixture.componentRef.setInput("sparklineData", [100, 200, 300]);
			fixture.componentRef.setInput("invertVariation", false);
			fixture.detectChanges();

			// THEN
			const expectedData = testSubject.sparklineChartData();
			expect(expectedData.datasets[0].data).toEqual([100, 200, 300]);
			expect(expectedData.datasets[0].borderColor).toBe("#22c55e");
		});

		it("should return empty labels for each data point", () => {
			// GIVEN
			fixture.componentRef.setInput("sparklineData", [10, 20, 30]);
			fixture.detectChanges();

			// THEN
			expect(testSubject.sparklineChartData().labels).toHaveLength(3);
		});
	});
});
