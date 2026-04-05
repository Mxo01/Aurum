import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MockComponent, MockDirective, MockPipe, MockProvider } from "ng-mocks";
import { RouterLink } from "@angular/router";
import { Button } from "primeng/button";
import { Message } from "primeng/message";
import { of } from "rxjs";
import { signal } from "@angular/core";
import { CashFlowComponent } from "./cashflow.component";
import { CashFlowService } from "./cashflow.service";
import { ProfileService } from "../profile/profile.service";
import { ThemeService } from "../../shared/services/theme/theme.service";
import { PrivacyService } from "../../shared/services/privacy/privacy.service";
import { NavigationService } from "../../shared/services/navigation/navigation.service";
import { PrivacyCurrencyPipe } from "../../shared/pipes/privacy-currency.pipe";
import { CashflowKpiCardComponent } from "./components/cashflow-kpi-card/cashflow-kpi-card.component";
import { CashflowChartComponent } from "./components/cashflow-chart/cashflow-chart.component";
import { CashflowTableComponent } from "./components/cashflow-table/cashflow-table.component";
import { CashFlowEntry, CashFlowYear } from "./model/cashflow.model";

const buildMockEntry = (month: number, earned = 1000, spent = 500): CashFlowEntry => ({
	id: null,
	month,
	earned,
	spent
});

const buildMockYear = (overrides: Partial<CashFlowYear> = {}): CashFlowYear => ({
	year: 2025,
	entries: [],
	previousYearEntries: null,
	availableYears: [2025],
	...overrides
});

describe("CashFlowComponent", () => {
	let fixture: ComponentFixture<CashFlowComponent>;
	let testSubject: CashFlowComponent;
	let mockCashFlowService: CashFlowService;

	beforeEach(() => {
		TestBed.configureTestingModule({
			imports: [
				CashFlowComponent,
				MockComponent(Button),
				MockDirective(RouterLink),
				MockComponent(Message),
				MockPipe(PrivacyCurrencyPipe),
				MockComponent(CashflowKpiCardComponent),
				MockComponent(CashflowChartComponent),
				MockComponent(CashflowTableComponent)
			],
			providers: [
				MockProvider(ProfileService, { getProfile: vi.fn().mockReturnValue(of(null)) }),
				MockProvider(CashFlowService, {
					getYear: vi.fn().mockReturnValue(of(buildMockYear()))
				}),
				MockProvider(ThemeService, { isDarkMode: signal(false) }),
				MockProvider(PrivacyService, { isPrivacyMode: signal(false) }),
				MockProvider(NavigationService, { previousRoute: signal("/dashboard") })
			]
		});
		fixture = TestBed.createComponent(CashFlowComponent);
		testSubject = fixture.componentInstance;
		mockCashFlowService = TestBed.inject(CashFlowService);
		fixture.detectChanges();
	});

	describe("rows", () => {
		it("should return empty array when yearData is null", () => {
			// GIVEN
			testSubject.yearData.set(null);
			fixture.detectChanges();

			// THEN
			expect(testSubject.rows()).toHaveLength(0);
		});

		it("should map entries to rows", () => {
			// GIVEN
			const stubbedEntries = [buildMockEntry(1, 1000, 400), buildMockEntry(2, 1200, 600)];
			testSubject.yearData.set(buildMockYear({ entries: stubbedEntries }));
			fixture.detectChanges();

			// THEN
			expect(testSubject.rows()).toHaveLength(2);
		});

		it("should compute balance for each row", () => {
			// GIVEN
			testSubject.yearData.set(buildMockYear({ entries: [buildMockEntry(1, 1000, 400)] }));
			fixture.detectChanges();

			// THEN
			expect(testSubject.rows()[0].balance).toBe(600);
		});

		it("should compute deltaPercent relative to previous month", () => {
			// GIVEN
			const entries = [buildMockEntry(1, 1000, 500), buildMockEntry(2, 1000, 600)];
			testSubject.yearData.set(buildMockYear({ entries }));
			fixture.detectChanges();

			const expectedRows = testSubject.rows();
			expect(expectedRows[0].deltaPercent).toBeNull();
			expect(expectedRows[1].deltaPercent).not.toBeNull();
		});
	});

	describe("avgEarned / avgSpent", () => {
		it("should return zero when there are no entries with data", () => {
			// GIVEN
			testSubject.yearData.set(buildMockYear({ entries: [] }));
			fixture.detectChanges();

			// THEN
			expect(testSubject.avgEarned()).toBe(0);
			expect(testSubject.avgSpent()).toBe(0);
		});

		it("should divide totals by number of months with data, not by 12", () => {
			// GIVEN
			const entries = [buildMockEntry(1, 1200, 600), buildMockEntry(2, 600, 300)];
			testSubject.yearData.set(buildMockYear({ entries }));
			fixture.detectChanges();

			// THEN
			expect(testSubject.avgEarned()).toBe(900);
			expect(testSubject.avgSpent()).toBe(450);
		});

		it("should exclude months with zero earned and spent from the divisor", () => {
			// GIVEN
			const entries = [buildMockEntry(1, 1200, 600), { id: null, month: 2, earned: 0, spent: 0 }];
			testSubject.yearData.set(buildMockYear({ entries }));
			fixture.detectChanges();

			// THEN
			expect(testSubject.avgEarned()).toBe(1200);
			expect(testSubject.avgSpent()).toBe(600);
		});
	});

	describe("totalEarned / totalSpent / totalBalance", () => {
		it("should sum earned and spent across all rows", () => {
			// GIVEN
			const entries = [buildMockEntry(1, 1000, 400), buildMockEntry(2, 2000, 600)];
			testSubject.yearData.set(buildMockYear({ entries }));
			fixture.detectChanges();

			// THEN
			expect(testSubject.totalEarned()).toBe(3000);
			expect(testSubject.totalSpent()).toBe(1000);
			expect(testSubject.totalBalance()).toBe(2000);
		});

		it("should return zero when there are no entries", () => {
			// GIVEN
			testSubject.yearData.set(buildMockYear({ entries: [] }));
			fixture.detectChanges();

			// THEN
			expect(testSubject.totalEarned()).toBe(0);
			expect(testSubject.totalSpent()).toBe(0);
			expect(testSubject.totalBalance()).toBe(0);
		});
	});

	describe("totalBalanceClass", () => {
		it("should return green class when balance is positive", () => {
			// GIVEN
			testSubject.yearData.set(buildMockYear({ entries: [buildMockEntry(1, 1000, 400)] }));
			fixture.detectChanges();

			// THEN
			expect(testSubject.totalBalanceClass()).toContain("green");
		});

		it("should return red class when balance is negative", () => {
			// GIVEN
			testSubject.yearData.set(buildMockYear({ entries: [buildMockEntry(1, 400, 1000)] }));
			fixture.detectChanges();

			// THEN
			expect(testSubject.totalBalanceClass()).toContain("red");
		});
	});

	describe("activeMonths", () => {
		it("should count months with non-zero earned or spent", () => {
			// GIVEN
			const entries = [
				buildMockEntry(1, 1000, 500),
				buildMockEntry(2, 0, 0),
				buildMockEntry(3, 0, 200)
			];
			testSubject.yearData.set(buildMockYear({ entries }));
			fixture.detectChanges();

			// THEN
			expect(testSubject.activeMonths()).toBe(2);
		});
	});

	describe("savingsRatePercent", () => {
		it("should return null when totalEarned is zero", () => {
			// GIVEN
			testSubject.yearData.set(buildMockYear({ entries: [] }));
			fixture.detectChanges();

			// THEN
			expect(testSubject.savingsRatePercent()).toBeNull();
		});

		it("should compute savings rate as balance over earned", () => {
			// GIVEN
			testSubject.yearData.set(buildMockYear({ entries: [buildMockEntry(1, 1000, 600)] }));
			fixture.detectChanges();

			// THEN
			expect(testSubject.savingsRatePercent()).toBeCloseTo(40);
		});

		it("should cap savings rate at 100", () => {
			// GIVEN
			testSubject.yearData.set(buildMockYear({ entries: [buildMockEntry(1, 100, 0)] }));
			fixture.detectChanges();

			// THEN
			expect(testSubject.savingsRatePercent()).toBe(100);
		});
	});

	describe("prevYearTotalEarned / prevYearTotalSpent", () => {
		it("should return null when there are no previous year entries", () => {
			// GIVEN
			testSubject.yearData.set(buildMockYear({ previousYearEntries: null }));
			fixture.detectChanges();

			// THEN
			expect(testSubject.prevYearTotalEarned()).toBeNull();
			expect(testSubject.prevYearTotalSpent()).toBeNull();
		});

		it("should sum previous year entries", () => {
			// GIVEN
			const prevEntries = [buildMockEntry(1, 800, 300), buildMockEntry(2, 900, 350)];
			testSubject.yearData.set(buildMockYear({ previousYearEntries: prevEntries }));
			fixture.detectChanges();

			// THEN
			expect(testSubject.prevYearTotalEarned()).toBe(1700);
			expect(testSubject.prevYearTotalSpent()).toBe(650);
		});
	});

	describe("yoyEarnedPercent", () => {
		it("should return null when there is no previous year data", () => {
			// GIVEN
			testSubject.yearData.set(buildMockYear({ previousYearEntries: null }));
			fixture.detectChanges();

			// THEN
			expect(testSubject.yoyEarnedPercent()).toBeNull();
		});

		it("should compute year-over-year percentage change for earned", () => {
			// GIVEN
			testSubject.yearData.set(
				buildMockYear({
					entries: [buildMockEntry(1, 1100, 500)],
					previousYearEntries: [buildMockEntry(1, 1000, 500)]
				})
			);
			fixture.detectChanges();

			// THEN
			expect(testSubject.yoyEarnedPercent()).toBeCloseTo(10);
		});
	});

	describe("hasPrevYearData", () => {
		it("should return false when previousYearEntries is null", () => {
			// GIVEN
			testSubject.yearData.set(buildMockYear({ previousYearEntries: null }));
			fixture.detectChanges();

			// THEN
			expect(testSubject.hasPrevYearData()).toBe(false);
		});

		it("should return true when previousYearEntries is present", () => {
			// GIVEN
			testSubject.yearData.set(buildMockYear({ previousYearEntries: [buildMockEntry(1)] }));
			fixture.detectChanges();

			// THEN
			expect(testSubject.hasPrevYearData()).toBe(true);
		});
	});

	describe("availableYears", () => {
		it("should return empty array when yearData is null", () => {
			// GIVEN
			testSubject.yearData.set(null);
			fixture.detectChanges();

			// THEN
			expect(testSubject.availableYears()).toEqual([]);
		});

		it("should return available years from yearData", () => {
			// GIVEN
			testSubject.yearData.set(buildMockYear({ availableYears: [2023, 2024, 2025] }));
			fixture.detectChanges();

			// THEN
			expect(testSubject.availableYears()).toEqual([2023, 2024, 2025]);
		});
	});

	describe("onEntryUpdated", () => {
		it("should update the matching entry in yearData", () => {
			// GIVEN
			const stubbedEntries = [buildMockEntry(3, 1000, 500), buildMockEntry(4, 2000, 800)];
			testSubject.yearData.set(buildMockYear({ entries: stubbedEntries }));
			fixture.detectChanges();

			const stubbedUpdatedEntry: CashFlowEntry = { id: "abc", month: 3, earned: 1500, spent: 600 };

			// WHEN
			testSubject.onEntryUpdated(stubbedUpdatedEntry);
			fixture.detectChanges();

			// THEN
			const expectedEntry = testSubject.yearData()?.entries.find(e => e.month === 3);
			expect(expectedEntry?.earned).toBe(1500);
			expect(expectedEntry?.spent).toBe(600);
		});

		it("should not modify other entries when one is updated", () => {
			// GIVEN
			const stubbedEntries = [buildMockEntry(3, 1000, 500), buildMockEntry(4, 2000, 800)];
			testSubject.yearData.set(buildMockYear({ entries: stubbedEntries }));
			fixture.detectChanges();

			// WHEN
			testSubject.onEntryUpdated({ id: "abc", month: 3, earned: 1500, spent: 600 });
			fixture.detectChanges();

			// THEN
			const expectedEntry = testSubject.yearData()?.entries.find(e => e.month === 4);
			expect(expectedEntry?.earned).toBe(2000);
		});
	});

	describe("prevYear", () => {
		it("should load the previous year", () => {
			// GIVEN
			testSubject.currentYear.set(2025);
			const stubbedYear = buildMockYear({ year: 2024 });
			vi.spyOn(mockCashFlowService, "getYear").mockReturnValue(of(stubbedYear));

			// WHEN
			testSubject.prevYear();

			// THEN
			expect(testSubject.currentYear()).toBe(2024);
			expect(testSubject.yearData()).toEqual(stubbedYear);
		});
	});

	describe("nextYear", () => {
		it("should not load a year beyond the current year", () => {
			// GIVEN
			const currentCalendarYear = new Date().getFullYear();
			testSubject.currentYear.set(currentCalendarYear);

			// WHEN
			testSubject.nextYear();

			// THEN
			expect(mockCashFlowService.getYear).not.toHaveBeenCalledWith(currentCalendarYear + 1);
		});

		it("should load the next year when not at the current year", () => {
			// GIVEN
			testSubject.currentYear.set(2022);
			const stubbedYear = buildMockYear({ year: 2023 });
			vi.spyOn(mockCashFlowService, "getYear").mockReturnValue(of(stubbedYear));

			// WHEN
			testSubject.nextYear();

			// THEN
			expect(testSubject.currentYear()).toBe(2023);
			expect(testSubject.yearData()).toEqual(stubbedYear);
		});
	});

	describe("isCurrentYearMax", () => {
		it("should return true when currentYear is the current calendar year", () => {
			// GIVEN
			testSubject.currentYear.set(new Date().getFullYear());

			// THEN
			expect(testSubject.isCurrentYearMax()).toBe(true);
		});

		it("should return false when currentYear is before the current calendar year", () => {
			// GIVEN
			testSubject.currentYear.set(new Date().getFullYear() - 1);

			// THEN
			expect(testSubject.isCurrentYearMax()).toBe(false);
		});
	});
});
