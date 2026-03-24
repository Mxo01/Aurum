import { ComponentFixture, TestBed } from "@angular/core/testing";
import { signal } from "@angular/core";
import { MockProvider } from "ng-mocks";
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
		TestBed.overrideComponent(AssetHistoryComponent, { set: { template: "", imports: [] } });
		TestBed.configureTestingModule({
			imports: [AssetHistoryComponent],
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
			TestBed.flushEffects();
			fixture.detectChanges();

			// THEN
			expect(testSubject.snapshotsForSelectedAsset()).toHaveLength(1);
		});

		it("should clear snapshots when selectedAsset is set to null", () => {
			// GIVEN
			const mockAsset = buildMockAsset();
			fixture.componentRef.setInput("selectedAsset", mockAsset);
			TestBed.flushEffects();

			// WHEN
			fixture.componentRef.setInput("selectedAsset", null);
			TestBed.flushEffects();
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
			TestBed.flushEffects();
			fixture.detectChanges();

			// THEN
			expect(testSubject.isAutomaticLiability()).toBe(true);
		});

		it("should return false for a regular asset", () => {
			// GIVEN
			const mockAsset = buildMockAsset({ type: AssetType.ASSET });
			fixture.componentRef.setInput("selectedAsset", mockAsset);

			// WHEN
			TestBed.flushEffects();
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
			TestBed.flushEffects();
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
		it("should call snapshotService.saveSnapshot and update snapshots on success", () => {
			// GIVEN
			const mockAsset = buildMockAsset();
			const stubbedSnapshot = buildMockSnapshot(mockAsset.id);
			fixture.componentRef.setInput("selectedAsset", mockAsset);
			TestBed.flushEffects();
			testSubject.newSnapshotValue.set(500);
			testSubject.newSnapshotDate.set(new Date("2024-01-15"));
			vi.spyOn(mockSnapshotService, "saveSnapshot").mockReturnValue(of([stubbedSnapshot]));

			// WHEN
			testSubject.addSnapshotFromHistory();
			fixture.detectChanges();

			// THEN
			expect(testSubject.snapshotsForSelectedAsset()).toHaveLength(1);
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
			TestBed.flushEffects();
			testSubject.newSnapshotValue.set(null);
			const saveSnapshot = vi.spyOn(mockSnapshotService, "saveSnapshot");

			// WHEN
			testSubject.addSnapshotFromHistory();

			// THEN
			expect(saveSnapshot).not.toHaveBeenCalled();
		});
	});

	describe("deleteSelectedSnapshotsHistory", () => {
		it("should call deleteSnapshotsBulk and update snapshots on success", () => {
			// GIVEN
			const mockAsset = buildMockAsset();
			const stubbedSnapshot = buildMockSnapshot(mockAsset.id);
			fixture.componentRef.setInput("selectedAsset", mockAsset);
			TestBed.flushEffects();
			testSubject.selectedSnapshotsHistory.set([stubbedSnapshot]);
			vi.spyOn(mockSnapshotService, "deleteSnapshotsBulk").mockReturnValue(of([]));

			// WHEN
			testSubject.deleteSelectedSnapshotsHistory();
			fixture.detectChanges();

			// THEN
			expect(testSubject.snapshotsForSelectedAsset()).toHaveLength(0);
			expect(testSubject.selectedSnapshotsHistory()).toHaveLength(0);
		});

		it("should not call deleteSnapshotsBulk when no snapshots are selected", () => {
			// GIVEN
			const mockAsset = buildMockAsset();
			fixture.componentRef.setInput("selectedAsset", mockAsset);
			TestBed.flushEffects();
			testSubject.selectedSnapshotsHistory.set([]);
			const deleteSnapshotsBulk = vi.spyOn(mockSnapshotService, "deleteSnapshotsBulk");

			// WHEN
			testSubject.deleteSelectedSnapshotsHistory();

			// THEN
			expect(deleteSnapshotsBulk).not.toHaveBeenCalled();
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
			TestBed.flushEffects();
			fixture.detectChanges();

			// THEN
			expect(testSubject.snapshotsForSelectedAsset()[0].referenceDate).toBe("2023-01-01");
			expect(testSubject.snapshotsForSelectedAsset()[1].referenceDate).toBe("2024-06-15");
		});
	});
});
