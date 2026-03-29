import { ComponentFixture, TestBed } from "@angular/core/testing";
import { signal } from "@angular/core";
import { MockComponent, MockModule, MockPipe, MockProvider } from "ng-mocks";
import { CommonModule } from "@angular/common";
import { PrivacyCurrencyPipe } from "../../shared/pipes/privacy-currency.pipe";
import { faker } from "@faker-js/faker";
import { of, throwError } from "rxjs";
import { Router } from "@angular/router";
import { DashboardComponent } from "./dashboard.component";
import { NetworthChartComponent } from "./components/networth-chart/networth-chart.component";
import { TopAssetsWidgetComponent } from "./components/top-assets/top-assets-widget.component";
import { TargetWidgetComponent } from "./components/target-widget/target-widget.component";
import { KpiCardComponent } from "./components/kpi-card/kpi-card.component";
import { DashboardService } from "./dashboard.service";
import { ProfileService } from "../profile/profile.service";
import { TargetService } from "../target/target.service";
import { ThemeService } from "../../shared/services/theme/theme.service";
import { AnalyticsSummary, ChartData, Projections } from "./model/dashboard.model";
import { Currency } from "../profile/model/currency.model";
import { Locale } from "../profile/model/locale.model";
import { UserProfile } from "../profile/model/user-profile.model";
import { Skeleton } from "primeng/skeleton";

const buildMockSummary = (): AnalyticsSummary => ({
	totalNetWorth: faker.number.float({ min: 10000, max: 500000 }),
	totalGrossAssets: faker.number.float({ min: 10000, max: 500000 }),
	variations: {
		oneMonth: { absolute: 100, percentage: 1 },
		sixMonths: { absolute: 500, percentage: 5 },
		oneYear: { absolute: 1000, percentage: 10 }
	},
	assetVariations: {
		oneMonth: { absolute: 100, percentage: 1 },
		sixMonths: { absolute: 500, percentage: 5 },
		oneYear: { absolute: 2000, percentage: 20 }
	},
	assetAllocation: { Stocks: 0.6, Cash: 0.4 },
	currencyImpact: faker.number.float({ min: -500, max: 500 }),
	totalLiabilities: faker.number.float({ min: 0, max: 50000 }),
	debtToAssetRatio: faker.number.float({ min: 0, max: 1 }),
	topAssets: [],
	oldestSnapshotYear: 2020
});

const buildMockChartData = (): ChartData => ({
	labels: ["Jan", "Feb"],
	totalNetWorth: [10000, 12000],
	totalAssetsOnly: [12000, 14000],
	favoriteAssetsValues: {},
	favoriteAssetsTypes: {}
});

const buildMockProjections = (): Projections => ({ 1: 15000, 2: 18000 });

const buildMockProfile = (): UserProfile => ({
	id: faker.string.uuid(),
	email: faker.internet.email(),
	currency: Currency.EUR,
	locale: Locale.EN_US
});

describe("DashboardComponent", () => {
	let fixture: ComponentFixture<DashboardComponent>;
	let testSubject: DashboardComponent;
	let mockDashboardService: DashboardService;
	let mockTargetService: TargetService;
	let mockProfileService: ProfileService;
	let mockRouter: Router;

	beforeEach(() => {
		TestBed.configureTestingModule({
			imports: [
				DashboardComponent,
				MockModule(CommonModule),
				MockComponent(Skeleton),
				MockComponent(NetworthChartComponent),
				MockComponent(TopAssetsWidgetComponent),
				MockComponent(TargetWidgetComponent),
				MockComponent(KpiCardComponent),
				MockPipe(PrivacyCurrencyPipe)
			],
			providers: [
				MockProvider(ProfileService, {
					getProfile: vi.fn().mockReturnValue(of(buildMockProfile()))
				}),
				MockProvider(DashboardService, {
					getSummary: vi.fn().mockReturnValue(of(buildMockSummary())),
					getChartData: vi.fn().mockReturnValue(of(buildMockChartData())),
					getProjections: vi.fn().mockReturnValue(of(buildMockProjections())),
					getChartDataForYear: vi.fn().mockReturnValue(of(buildMockChartData()))
				}),
				MockProvider(TargetService, { getTargets: vi.fn().mockReturnValue(of([])) }),
				MockProvider(ThemeService, { isDarkMode: signal(false), applyLocale: vi.fn() }),
				MockProvider(Router, { navigate: vi.fn().mockResolvedValue(true) })
			]
		});
		fixture = TestBed.createComponent(DashboardComponent);
		testSubject = fixture.componentInstance;
		mockDashboardService = TestBed.inject(DashboardService);
		mockTargetService = TestBed.inject(TargetService);
		mockProfileService = TestBed.inject(ProfileService);
		mockRouter = TestBed.inject(Router);
		fixture.detectChanges();
	});

	describe("ngOnInit", () => {
		it("should load summary and chart data on init", () => {
			// GIVEN
			const stubbedSummary = buildMockSummary();
			vi.spyOn(mockDashboardService, "getSummary").mockReturnValue(of(stubbedSummary));

			// WHEN
			testSubject.ngOnInit();

			// THEN
			expect(testSubject.summary()).not.toBeNull();
		});

		it("should leave signals null when all services throw errors", () => {
			// GIVEN
			vi.spyOn(mockDashboardService, "getSummary").mockReturnValue(
				throwError(() => new Error("fail"))
			);
			vi.spyOn(mockDashboardService, "getChartData").mockReturnValue(
				throwError(() => new Error("fail"))
			);
			vi.spyOn(mockDashboardService, "getProjections").mockReturnValue(
				throwError(() => new Error("fail"))
			);
			vi.spyOn(mockTargetService, "getTargets").mockReturnValue(
				throwError(() => new Error("fail"))
			);
			vi.spyOn(mockProfileService, "getProfile").mockReturnValue(
				throwError(() => new Error("fail"))
			);

			// WHEN
			testSubject.ngOnInit();

			// THEN
			expect(testSubject.summary()).toBeNull();
		});
	});

	describe("projectedNetWorth", () => {
		it("should return the projected net worth at year 1", () => {
			// WHEN
			testSubject.projections.set({ 1: 25000, 2: 30000 });

			// THEN
			expect(testSubject.projectedNetWorth()).toBe(25000);
		});

		it("should return 0 when projections is null", () => {
			// WHEN
			testSubject.projections.set(null);

			// THEN
			expect(testSubject.projectedNetWorth()).toBe(0);
		});
	});

	describe("projectedGrowthVariation", () => {
		it("should compute the growth percentage from current to projected", () => {
			// GIVEN
			const mockSummary = buildMockSummary();
			mockSummary.totalGrossAssets = 10000;

			// WHEN
			testSubject.summary.set(mockSummary);
			testSubject.projections.set({ 1: 12000, 2: 15000 });

			// THEN
			expect(testSubject.projectedGrowthVariation()).toBe(20);
		});

		it("should return undefined when summary is null", () => {
			// WHEN
			testSubject.summary.set(null);
			testSubject.projections.set({ 1: 12000, 2: 15000 });

			// THEN
			expect(testSubject.projectedGrowthVariation()).toBeUndefined();
		});

		it("should return undefined when projections is null", () => {
			// GIVEN
			const mockSummary = buildMockSummary();
			mockSummary.totalGrossAssets = 10000;

			// WHEN
			testSubject.summary.set(mockSummary);
			testSubject.projections.set(null);

			// THEN
			expect(testSubject.projectedGrowthVariation()).toBeUndefined();
		});

		it("should return undefined when totalGrossAssets is zero", () => {
			// GIVEN
			const mockSummary = buildMockSummary();
			mockSummary.totalGrossAssets = 0;

			// WHEN
			testSubject.summary.set(mockSummary);
			testSubject.projections.set({ 1: 12000, 2: 15000 });

			// THEN
			expect(testSubject.projectedGrowthVariation()).toBeUndefined();
		});
	});

	describe("debtToAssetRatio", () => {
		it("should compute the debt to asset ratio from summary", () => {
			// GIVEN
			const mockSummary = buildMockSummary();
			mockSummary.debtToAssetRatio = 0.35;

			// WHEN
			testSubject.summary.set(mockSummary);

			// THEN
			expect(testSubject.debtToAssetRatio()).toBe(0.35);
		});

		it("should return 0 when summary is null", () => {
			// WHEN
			testSubject.summary.set(null);

			// THEN
			expect(testSubject.debtToAssetRatio()).toBe(0);
		});
	});

	describe("assetGrowth1Y", () => {
		it("should return the 1-year absolute growth from summary", () => {
			// GIVEN
			const mockSummary = buildMockSummary();
			mockSummary.assetVariations.oneYear.absolute = 5000;

			// WHEN
			testSubject.summary.set(mockSummary);

			// THEN
			expect(testSubject.assetGrowth1Y()).toBe(5000);
		});

		it("should return 0 when summary is null", () => {
			// WHEN
			testSubject.summary.set(null);

			// THEN
			expect(testSubject.assetGrowth1Y()).toBe(0);
		});
	});

	describe("avgMonthlyAbsolute", () => {
		it("should return the 1-year absolute divided by 12", () => {
			// GIVEN
			const mockSummary = buildMockSummary();
			mockSummary.assetVariations.oneYear.absolute = 1200;

			// WHEN
			testSubject.summary.set(mockSummary);

			// THEN
			expect(testSubject.avgMonthlyAbsolute()).toBe(100);
		});

		it("should return undefined when summary is null", () => {
			// WHEN
			testSubject.summary.set(null);

			// THEN
			expect(testSubject.avgMonthlyAbsolute()).toBeUndefined();
		});
	});

	describe("avgMonthlyPercent", () => {
		it("should return the compound monthly rate derived from the 1-year percentage", () => {
			// GIVEN
			const mockSummary = buildMockSummary();
			mockSummary.assetVariations.oneYear.percentage = 0;

			// WHEN
			testSubject.summary.set(mockSummary);

			// THEN
			expect(testSubject.avgMonthlyPercent()).toBe(0);
		});

		it("should return undefined when summary is null", () => {
			// WHEN
			testSubject.summary.set(null);

			// THEN
			expect(testSubject.avgMonthlyPercent()).toBeUndefined();
		});

		it("should return a positive value when 1-year percentage is positive", () => {
			// GIVEN
			const mockSummary = buildMockSummary();
			mockSummary.assetVariations.oneYear.percentage = 12;

			// WHEN
			testSubject.summary.set(mockSummary);

			// THEN
			expect(testSubject.avgMonthlyPercent()).toBeGreaterThan(0);
		});
	});

	describe("assetGrowth1YVariation", () => {
		it("should return the 1-year percentage growth fixed to 2 decimals", () => {
			// GIVEN
			const mockSummary = buildMockSummary();
			mockSummary.assetVariations.oneYear.percentage = 12.345;

			// WHEN
			testSubject.summary.set(mockSummary);

			// THEN
			expect(testSubject.assetGrowth1YVariation()).toBe(12.35);
		});

		it("should return undefined when summary is null", () => {
			// WHEN
			testSubject.summary.set(null);

			// THEN
			expect(testSubject.assetGrowth1YVariation()).toBeUndefined();
		});
	});

	describe("currencyImpact", () => {
		it("should return the currency impact from summary", () => {
			// GIVEN
			const mockSummary = buildMockSummary();
			mockSummary.currencyImpact = 300;

			// WHEN
			testSubject.summary.set(mockSummary);

			// THEN
			expect(testSubject.currencyImpact()).toBe(300);
		});

		it("should return 0 when summary is null", () => {
			// WHEN
			testSubject.summary.set(null);

			// THEN
			expect(testSubject.currencyImpact()).toBe(0);
		});
	});

	describe("totalLiabilities", () => {
		it("should compute total liabilities from summary", () => {
			// GIVEN
			const mockSummary = buildMockSummary();
			mockSummary.totalLiabilities = 5000;

			// WHEN
			testSubject.summary.set(mockSummary);

			// THEN
			expect(testSubject.totalLiabilities()).toBe(5000);
		});

		it("should return 0 when summary is null", () => {
			// WHEN
			testSubject.summary.set(null);

			// THEN
			expect(testSubject.totalLiabilities()).toBe(0);
		});
	});

	describe("liabilitiesSparkline", () => {
		it("should compute sparkline data from chart data", () => {
			// GIVEN
			const mockChartData: ChartData = {
				labels: ["Jan", "Feb", "Mar"],
				totalNetWorth: [9000, 9500, 10000],
				totalAssetsOnly: [10000, 11000, 12000],
				favoriteAssetsValues: {},
				favoriteAssetsTypes: {}
			};

			// WHEN
			testSubject.chartData.set(mockChartData);

			// THEN
			expect(testSubject.liabilitiesSparkline().length).toBeGreaterThan(0);
		});

		it("should return an empty array when chartData is null", () => {
			// WHEN
			testSubject.chartData.set(null);

			// THEN
			expect(testSubject.liabilitiesSparkline()).toHaveLength(0);
		});
	});

	describe("onYearChanged", () => {
		it("should reload all data when year is null", () => {
			// GIVEN
			const loadDataSpy = vi.spyOn(testSubject, "loadData");

			// WHEN
			testSubject.onYearChanged(null);

			// THEN
			expect(loadDataSpy).toHaveBeenCalled();
		});

		it("should fetch chart data for the specified year when a year is provided", () => {
			// GIVEN
			const getChartDataForYear = vi
				.spyOn(mockDashboardService, "getChartDataForYear")
				.mockReturnValue(of(buildMockChartData()));

			// WHEN
			testSubject.onYearChanged(2023);
			fixture.detectChanges();

			// THEN
			expect(getChartDataForYear).toHaveBeenCalledWith(2023);
		});

		it("should set chartData to null when getChartDataForYear throws", () => {
			// GIVEN
			vi.spyOn(mockDashboardService, "getChartDataForYear").mockReturnValue(
				throwError(() => new Error("fail"))
			);

			// WHEN
			testSubject.onYearChanged(2023);
			fixture.detectChanges();

			// THEN
			expect(testSubject.chartData()).toBeNull();
		});
	});

	describe("navigateToAssets", () => {
		it("should navigate to /assets", () => {
			// GIVEN
			const navigate = vi.spyOn(mockRouter, "navigate").mockResolvedValue(true);

			// WHEN
			testSubject.navigateToAssets();

			// THEN
			expect(navigate).toHaveBeenCalledWith(["/assets"]);
		});
	});
});
