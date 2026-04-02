import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MockComponent } from "ng-mocks";
import { MeterGroup } from "primeng/metergroup";
import { AssetAllocationChartComponent } from "./asset-allocation-chart.component";
import { AnalyticsSummary } from "../../model/dashboard.model";
import { ComponentRef } from "@angular/core";

const buildMockSummary = (assetAllocation: Record<string, number> = {}): AnalyticsSummary =>
	({
		assetAllocation,
		totalNetWorth: 0,
		totalGrossAssets: 0,
		totalLiabilities: 0,
		debtToAssetRatio: 0,
		variations: { oneMonth: null, sixMonths: null, oneYear: null },
		assetVariations: { oneMonth: null, sixMonths: null, oneYear: null },
		topAssets: [],
		currencyImpact: 0,
		oldestSnapshotYear: null
	}) as unknown as AnalyticsSummary;

describe("AssetAllocationChartComponent", () => {
	let fixture: ComponentFixture<AssetAllocationChartComponent>;
	let testSubject: AssetAllocationChartComponent;
	let componentRef: ComponentRef<AssetAllocationChartComponent>;

	beforeEach(() => {
		TestBed.configureTestingModule({
			imports: [AssetAllocationChartComponent, MockComponent(MeterGroup)]
		});
		fixture = TestBed.createComponent(AssetAllocationChartComponent);
		testSubject = fixture.componentInstance;
		componentRef = fixture.componentRef;
	});

	describe("meterItems", () => {
		it("should return an empty array when summary is null", () => {
			// GIVEN
			componentRef.setInput("summary", null);
			fixture.detectChanges();

			// THEN
			expect(testSubject.meterItems()).toHaveLength(0);
		});

		it("should return mapped items when summary has allocation data", () => {
			// GIVEN
			componentRef.setInput("summary", buildMockSummary({ Stocks: 60, Bonds: 40 }));
			fixture.detectChanges();

			// THEN
			expect(testSubject.meterItems()).toHaveLength(2);
		});

		it("should filter out zero-value allocations", () => {
			// GIVEN
			componentRef.setInput("summary", buildMockSummary({ Stocks: 100, Cash: 0 }));
			fixture.detectChanges();

			// THEN
			expect(testSubject.meterItems()).toHaveLength(1);
		});
	});
});
