import { ComponentFixture, TestBed } from "@angular/core/testing";
import { signal } from "@angular/core";
import { MockComponent, MockModule, MockPipe, MockProvider } from "ng-mocks";
import { DatePipe } from "@angular/common";
import { PrivacyCurrencyPipe } from "../../../../shared/pipes/privacy-currency.pipe";
import { FormsModule } from "@angular/forms";
import { Dialog } from "primeng/dialog";
import { TableModule } from "primeng/table";
import { Button } from "primeng/button";
import { InputNumber } from "primeng/inputnumber";
import { DatePicker } from "primeng/datepicker";
import { Message } from "primeng/message";
import { SelectButton } from "primeng/selectbutton";
import { UIChart } from "primeng/chart";
import { faker } from "@faker-js/faker";
import { of } from "rxjs";
import { AssetHistoryComponent } from "./asset-history.component";
import { SnapshotService } from "../../../snapshot/snapshot.service";
import { ThemeService } from "../../../../shared/services/theme/theme.service";
import { Asset, AssetType, LiabilityType } from "../../model/asset.model";
import { Snapshot } from "../../../snapshot/model/snapshot.model";
import { Currency } from "../../../profile/model/currency.model";

const buildMockAsset = (overrides: Partial<Asset> = {}): Asset => ({
	id: faker.string.uuid(),
	name: faker.lorem.word(),
	categoryId: faker.string.uuid(),
	categoryName: faker.lorem.word(),
	categoryIcon: null,
	type: AssetType.ASSET,
	originalCurrency: Currency.EUR,
	isActive: true,
	isFavorite: false,
	...overrides
});

const buildMockSnapshot = (assetId: string, overrides: Partial<Snapshot> = {}): Snapshot => ({
	id: faker.string.uuid(),
	assetId,
	referenceDate: faker.date.past().toISOString().split("T")[0],
	amountOriginalCurrency: faker.number.float({ min: 100, max: 10000 }),
	...overrides
});

describe("AssetHistoryComponent", () => {
	let fixture: ComponentFixture<AssetHistoryComponent>;
	let testSubject: AssetHistoryComponent;
	let mockSnapshotService: SnapshotService;

	beforeEach(() => {
		TestBed.configureTestingModule({
			imports: [
				AssetHistoryComponent,
				MockComponent(Dialog),
				MockModule(TableModule),
				MockComponent(Button),
				MockModule(FormsModule),
				MockComponent(InputNumber),
				MockComponent(DatePicker),
				MockPipe(DatePipe),
				MockPipe(PrivacyCurrencyPipe),
				MockComponent(Message),
				MockComponent(SelectButton),
				MockComponent(UIChart)
			],
			providers: [
				MockProvider(SnapshotService, {
					getSnapshotsByAssetId: vi.fn().mockReturnValue(of([])),
					saveSnapshot: vi.fn().mockReturnValue(of([])),
					deleteSnapshotsBulk: vi.fn().mockReturnValue(of([]))
				}),
				MockProvider(ThemeService, { isDarkMode: signal(false) })
			]
		});
		fixture = TestBed.createComponent(AssetHistoryComponent);
		testSubject = fixture.componentInstance;
		mockSnapshotService = TestBed.inject(SnapshotService);
		fixture.componentRef.setInput("isVisible", true);
		fixture.componentRef.setInput("selectedAsset", null);
		fixture.detectChanges();
	});

	describe("effect — snapshot loading", () => {
		it("should load snapshots when a selected asset is provided", () => {
			// GIVEN
			const mockAsset = buildMockAsset();
			const stubbedSnapshots = [buildMockSnapshot(mockAsset.id)];
			vi.spyOn(mockSnapshotService, "getSnapshotsByAssetId").mockReturnValue(of(stubbedSnapshots));
			fixture.componentRef.setInput("selectedAsset", mockAsset);

			// WHEN
			fixture.detectChanges();

			// THEN
			expect(testSubject.snapshotsForSelectedAsset()).toHaveLength(1);
		});

		it("should clear snapshots when selectedAsset is set to null", () => {
			// GIVEN
			const mockAsset = buildMockAsset();
			fixture.componentRef.setInput("selectedAsset", mockAsset);

			// WHEN
			fixture.componentRef.setInput("selectedAsset", null);
			fixture.detectChanges();

			// THEN
			expect(testSubject.snapshotsForSelectedAsset()).toHaveLength(0);
		});
	});

	describe("isAutomaticLiability", () => {
		it("should return true when the selected asset has AUTOMATIC liability type", () => {
			// GIVEN
			const mockAsset = buildMockAsset({ liabilityType: LiabilityType.AUTOMATIC });
			fixture.componentRef.setInput("selectedAsset", mockAsset);

			// WHEN
			fixture.detectChanges();

			// THEN
			expect(testSubject.isAutomaticLiability()).toBe(true);
		});

		it("should return false for a regular asset", () => {
			// GIVEN
			const mockAsset = buildMockAsset({ type: AssetType.ASSET });
			fixture.componentRef.setInput("selectedAsset", mockAsset);

			// WHEN
			fixture.detectChanges();

			// THEN
			expect(testSubject.isAutomaticLiability()).toBe(false);
		});
	});

	describe("onHideDrawer", () => {
		it("should reset state and clear selectedAsset on hide", () => {
			// GIVEN
			const mockAsset = buildMockAsset();
			fixture.componentRef.setInput("selectedAsset", mockAsset);
			fixture.detectChanges();

			// WHEN
			testSubject.onHideDrawer();
			fixture.detectChanges();

			// THEN
			expect(testSubject.selectedAsset()).toBeNull();
			expect(testSubject.newSnapshotValue()).toBeNull();
		});
	});

	describe("addSnapshotFromHistory", () => {
		it("should call snapshotService.saveSnapshot with correct params and update snapshots on success", () => {
			// GIVEN
			const mockAsset = buildMockAsset();
			const stubbedSnapshot = buildMockSnapshot(mockAsset.id);
			fixture.componentRef.setInput("selectedAsset", mockAsset);
			testSubject.newSnapshotValue.set(500);
			testSubject.newSnapshotDate.set(new Date("2024-01-15"));
			const saveSnapshot = vi
				.spyOn(mockSnapshotService, "saveSnapshot")
				.mockReturnValue(of([stubbedSnapshot]));
			const emitSpy = vi.spyOn(testSubject.snapshotsChanged, "emit");

			// WHEN
			testSubject.addSnapshotFromHistory();

			// THEN
			expect(saveSnapshot).toHaveBeenCalledWith(
				expect.objectContaining({ assetId: mockAsset.id, amountOriginalCurrency: 500 })
			);
			expect(testSubject.snapshotsForSelectedAsset()).toHaveLength(1);
			expect(emitSpy).toHaveBeenCalled();
			expect(testSubject.newSnapshotValue()).toBeNull();
		});

		it("should not call saveSnapshot when asset id is missing", () => {
			// GIVEN
			const saveSnapshot = vi.spyOn(mockSnapshotService, "saveSnapshot");
			fixture.componentRef.setInput("selectedAsset", null);
			testSubject.newSnapshotValue.set(500);

			// WHEN
			testSubject.addSnapshotFromHistory();

			// THEN
			expect(saveSnapshot).not.toHaveBeenCalled();
		});

		it("should not call saveSnapshot when amount is null", () => {
			// GIVEN
			const mockAsset = buildMockAsset();
			fixture.componentRef.setInput("selectedAsset", mockAsset);
			testSubject.newSnapshotValue.set(null);
			const saveSnapshot = vi.spyOn(mockSnapshotService, "saveSnapshot");

			// WHEN
			testSubject.addSnapshotFromHistory();

			// THEN
			expect(saveSnapshot).not.toHaveBeenCalled();
		});
	});

	describe("deleteSelectedSnapshotsHistory", () => {
		it("should call deleteSnapshotsBulk with correct params and update snapshots on success", () => {
			// GIVEN
			const mockAsset = buildMockAsset();
			const stubbedSnapshot = buildMockSnapshot(mockAsset.id);
			fixture.componentRef.setInput("selectedAsset", mockAsset);
			testSubject.selectedSnapshotsHistory.set([stubbedSnapshot]);
			const deleteSnapshotsBulk = vi
				.spyOn(mockSnapshotService, "deleteSnapshotsBulk")
				.mockReturnValue(of([]));
			const emitSpy = vi.spyOn(testSubject.snapshotsChanged, "emit");

			// WHEN
			testSubject.deleteSelectedSnapshotsHistory();

			// THEN
			expect(deleteSnapshotsBulk).toHaveBeenCalledWith(
				mockAsset.id,
				expect.arrayContaining([stubbedSnapshot.id])
			);
			expect(testSubject.snapshotsForSelectedAsset()).toHaveLength(0);
			expect(testSubject.selectedSnapshotsHistory()).toHaveLength(0);
			expect(emitSpy).toHaveBeenCalled();
		});

		it("should not call deleteSnapshotsBulk when no snapshots are selected", () => {
			// GIVEN
			const mockAsset = buildMockAsset();
			fixture.componentRef.setInput("selectedAsset", mockAsset);
			testSubject.selectedSnapshotsHistory.set([]);
			const deleteSnapshotsBulk = vi.spyOn(mockSnapshotService, "deleteSnapshotsBulk");

			// WHEN
			testSubject.deleteSelectedSnapshotsHistory();

			// THEN
			expect(deleteSnapshotsBulk).not.toHaveBeenCalled();
		});
	});

	describe("chartOptions", () => {
		it("should return chart options using EUR when no asset is selected", () => {
			// WHEN
			const result = testSubject.chartOptions();

			// THEN
			expect(result).toBeDefined();
		});

		it("should return chart options using asset originalCurrency when an asset is selected", () => {
			// GIVEN
			const mockAsset = buildMockAsset({ originalCurrency: Currency.EUR });
			fixture.componentRef.setInput("selectedAsset", mockAsset);

			// WHEN
			const result = testSubject.chartOptions();

			// THEN
			expect(result).toBeDefined();
		});
	});

	describe("snapshotsForSelectedAsset", () => {
		it("should return snapshots sorted by referenceDate ascending", () => {
			// GIVEN
			const mockAsset = buildMockAsset();
			const stubbedOlder = buildMockSnapshot(mockAsset.id, { referenceDate: "2023-01-01" });
			const stubbedNewer = buildMockSnapshot(mockAsset.id, { referenceDate: "2024-06-15" });
			vi.spyOn(mockSnapshotService, "getSnapshotsByAssetId").mockReturnValue(
				of([stubbedNewer, stubbedOlder])
			);
			fixture.componentRef.setInput("selectedAsset", mockAsset);

			// WHEN
			fixture.detectChanges();

			// THEN
			expect(testSubject.snapshotsForSelectedAsset()[0].referenceDate).toBe("2023-01-01");
			expect(testSubject.snapshotsForSelectedAsset()[1].referenceDate).toBe("2024-06-15");
		});
	});
});
