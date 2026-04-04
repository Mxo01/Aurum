import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MockComponent, MockModule } from "ng-mocks";
import { ChartModule } from "primeng/chart";
import { Skeleton } from "primeng/skeleton";
import { CashflowChartComponent } from "./cashflow-chart.component";
import { CashFlowEntry } from "../../model/cashflow.model";
import { Currency } from "../../../profile/model/currency.model";
import { Locale } from "../../../profile/model/locale.model";
import { DOCUMENT } from "@angular/common";

const buildMockEntry = (month: number, earned = 1000, spent = 500): CashFlowEntry => ({
	id: null,
	month,
	earned,
	spent
});

describe("CashflowChartComponent", () => {
	let fixture: ComponentFixture<CashflowChartComponent>;
	let testSubject: CashflowChartComponent;

	beforeEach(() => {
		TestBed.configureTestingModule({
			imports: [CashflowChartComponent, MockModule(ChartModule), MockComponent(Skeleton)],
			providers: [{ provide: DOCUMENT, useValue: document }]
		});
		fixture = TestBed.createComponent(CashflowChartComponent);
		testSubject = fixture.componentInstance;
		fixture.componentRef.setInput("isLoading", false);
		fixture.componentRef.setInput("currency", Currency.EUR);
		fixture.componentRef.setInput("locale", Locale.EN_US);
		fixture.componentRef.setInput("isDarkMode", false);
		fixture.componentRef.setInput("isPrivacyMode", false);
		fixture.componentRef.setInput("entries", []);
		fixture.detectChanges();
	});

	describe("chartData", () => {
		it("should return null when entries is empty", () => {
			// THEN
			expect(testSubject.chartData()).toBeNull();
		});

		it("should return mapped chart data when entries are present", () => {
			// GIVEN
			const stubbedEntries = [buildMockEntry(1), buildMockEntry(2)];

			// WHEN
			fixture.componentRef.setInput("entries", stubbedEntries);
			fixture.detectChanges();

			// THEN
			expect(testSubject.chartData()).not.toBeNull();
		});

		it("should include Earned and Spent datasets", () => {
			// GIVEN
			const stubbedEntries = [buildMockEntry(3, 1500, 700)];

			// WHEN
			fixture.componentRef.setInput("entries", stubbedEntries);
			fixture.detectChanges();

			// THEN
			const expectedData = testSubject.chartData();
			expect(expectedData?.datasets[0].label).toBe("Earned");
			expect(expectedData?.datasets[1].label).toBe("Spent");
		});

		it("should return null when entries are cleared after being set", () => {
			// GIVEN
			fixture.componentRef.setInput("entries", [buildMockEntry(1)]);
			fixture.detectChanges();

			// WHEN
			fixture.componentRef.setInput("entries", []);
			fixture.detectChanges();

			// THEN
			expect(testSubject.chartData()).toBeNull();
		});
	});

	describe("chartPlugins", () => {
		it("should return an array with one plugin", () => {
			// THEN
			expect(testSubject.chartPlugins()).toHaveLength(1);
		});

		it("should contain the bar labels plugin", () => {
			// THEN
			expect(testSubject.chartPlugins()[0].id).toBe("cashflowBarLabels");
		});
	});

	describe("chartOptions", () => {
		it("should return options with responsive true", () => {
			// THEN
			expect(testSubject.chartOptions()?.responsive).toBe(true);
		});
	});
});
