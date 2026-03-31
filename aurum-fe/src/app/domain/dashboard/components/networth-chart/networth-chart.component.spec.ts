import { ComponentFixture, TestBed } from "@angular/core/testing";
import { signal } from "@angular/core";
import { MockComponent, MockModule, MockPipe, MockProvider } from "ng-mocks";
import { CommonModule, DecimalPipe } from "@angular/common";
import { PrivacyService } from "../../../../shared/services/privacy/privacy.service";
import { PrivacyCurrencyPipe } from "../../../../shared/pipes/privacy-currency.pipe";
import { ChartModule } from "primeng/chart";
import { SelectButtonModule } from "primeng/selectbutton";
import { DatePicker } from "primeng/datepicker";
import { FormsModule } from "@angular/forms";
import { Button } from "primeng/button";
import { NetworthChartComponent } from "./networth-chart.component";
import { ThemeService } from "../../../../shared/services/theme/theme.service";
import { Currency } from "../../../profile/model/currency.model";
import { ChartData } from "../../model/dashboard.model";

const buildMockChartData = (): ChartData => ({
	labels: ["Jan", "Feb", "Mar"],
	totalNetWorth: [9000, 10000, 11000],
	totalAssetsOnly: [10000, 11000, 12000],
	favoriteAssetsValues: {},
	favoriteAssetsTypes: {}
});

describe("NetworthChartComponent", () => {
	let fixture: ComponentFixture<NetworthChartComponent>;
	let testSubject: NetworthChartComponent;

	beforeEach(() => {
		TestBed.configureTestingModule({
			imports: [
				NetworthChartComponent,
				MockModule(CommonModule),
				MockModule(ChartModule),
				MockModule(SelectButtonModule),
				MockComponent(DatePicker),
				MockModule(FormsModule),
				MockPipe(DecimalPipe),
				MockPipe(PrivacyCurrencyPipe),
				MockComponent(Button)
			],
			providers: [
				MockProvider(ThemeService, { isDarkMode: signal(false) }),
				MockProvider(PrivacyService, { isPrivacyMode: signal(false) })
			]
		});
		fixture = TestBed.createComponent(NetworthChartComponent);
		testSubject = fixture.componentInstance;
		fixture.componentRef.setInput("currency", Currency.EUR);
		fixture.detectChanges();
	});

	describe("minDate", () => {
		it("should return January 1st of the given min year", () => {
			// GIVEN
			fixture.componentRef.setInput("minYear", 2020);
			fixture.detectChanges();

			// THEN
			expect(testSubject.minDate().getFullYear()).toBe(2020);
			expect(testSubject.minDate().getMonth()).toBe(0);
			expect(testSubject.minDate().getDate()).toBe(1);
		});

		it("should default to the current year when minYear is null", () => {
			// GIVEN
			fixture.componentRef.setInput("minYear", null);
			fixture.detectChanges();

			// THEN
			expect(testSubject.minDate().getFullYear()).toBe(new Date().getFullYear());
		});
	});

	describe("isSpecificYearSelected", () => {
		it("should be false when selectedDate is null", () => {
			// GIVEN
			testSubject.selectedDate.set(null);

			// THEN
			expect(testSubject.isSpecificYearSelected()).toBe(false);
		});

		it("should be true when a date is selected", () => {
			// GIVEN
			testSubject.selectedDate.set(new Date("2023-06-15"));

			// THEN
			expect(testSubject.isSpecificYearSelected()).toBe(true);
		});
	});

	describe("totalAmount", () => {
		it("should return the totalNetWorth from summary", () => {
			// GIVEN
			fixture.componentRef.setInput("summary", { totalNetWorth: 50000 });
			fixture.detectChanges();

			// THEN
			expect(testSubject.totalAmount()).toBe(50000);
		});

		it("should return 0 when summary is null", () => {
			// GIVEN
			fixture.componentRef.setInput("summary", null);
			fixture.detectChanges();

			// THEN
			expect(testSubject.totalAmount()).toBe(0);
		});
	});

	describe("variation", () => {
		it("should return the assetVariations entry for the selected period", () => {
			// GIVEN
			const stubbedVariation = { absolute: 500, percentage: 5 };
			fixture.componentRef.setInput("summary", {
				assetVariations: {
					oneMonth: stubbedVariation,
					sixMonths: stubbedVariation,
					oneYear: stubbedVariation
				}
			});
			testSubject.selectedPeriod.set("oneMonth");
			fixture.detectChanges();

			// THEN
			expect(testSubject.variation()).toEqual(stubbedVariation);
		});

		it("should return undefined when summary is null", () => {
			// GIVEN
			fixture.componentRef.setInput("summary", null);
			fixture.detectChanges();

			// THEN
			expect(testSubject.variation()).toBeUndefined();
		});
	});

	describe("hasEnoughDataToDisplayChart", () => {
		it("should return true when there is at least one non-zero value in totalAssetsOnly", () => {
			// GIVEN
			fixture.componentRef.setInput("data", buildMockChartData());
			fixture.detectChanges();

			// THEN
			expect(testSubject.hasEnoughDataToDisplayChart()).toBe(true);
		});

		it("should return false when all values are zero", () => {
			// GIVEN
			fixture.componentRef.setInput("data", {
				...buildMockChartData(),
				totalAssetsOnly: [0, 0, 0]
			});
			fixture.detectChanges();

			// THEN
			expect(testSubject.hasEnoughDataToDisplayChart()).toBe(false);
		});
	});

	describe("isAvgMonthlyPositive", () => {
		it("should be true when avgMonthlyPercent is positive", () => {
			// GIVEN
			fixture.componentRef.setInput("avgMonthlyPercent", 1.5);
			fixture.detectChanges();

			// THEN
			expect(testSubject.isAvgMonthlyPositive()).toBe(true);
		});

		it("should be false when avgMonthlyPercent is negative", () => {
			// GIVEN
			fixture.componentRef.setInput("avgMonthlyPercent", -2);
			fixture.detectChanges();

			// THEN
			expect(testSubject.isAvgMonthlyPositive()).toBe(false);
		});

		it("should be true when avgMonthlyPercent is undefined", () => {
			// GIVEN
			fixture.detectChanges();

			// THEN
			expect(testSubject.isAvgMonthlyPositive()).toBe(true);
		});
	});

	describe("onDateChange", () => {
		it("should emit the year from the selected date", () => {
			// GIVEN
			testSubject.selectedDate.set(new Date("2023-06-15"));
			const emitted: (number | null)[] = [];
			testSubject.yearChanged.subscribe(y => emitted.push(y));

			// WHEN
			testSubject.onDateChange();

			// THEN
			expect(emitted[0]).toBe(2023);
		});

		it("should emit null when selected date is null", () => {
			// GIVEN
			testSubject.selectedDate.set(null);
			const emitted: (number | null)[] = [];
			testSubject.yearChanged.subscribe(y => emitted.push(y));

			// WHEN
			testSubject.onDateChange();

			// THEN
			expect(emitted[0]).toBeNull();
		});
	});
});
