import { signal } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { faker } from "@faker-js/faker";
import { of } from "rxjs";
import { ConfirmationService } from "primeng/api";
import { MockComponent, MockDirective, MockModule, MockProvider } from "ng-mocks";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { RouterLink } from "@angular/router";
import { Button } from "primeng/button";
import { TableModule } from "primeng/table";
import { DatePicker } from "primeng/datepicker";
import { Dialog } from "primeng/dialog";
import { AssetComponent } from "./asset.component";
import { AssetService } from "./asset.service";
import { ProfileService } from "../profile/profile.service";
import { NavigationService } from "../../shared/services/navigation/navigation.service";
import { ThemeService } from "../../shared/services/theme/theme.service";
import { AssetFormComponent } from "./components/asset-form/asset-form.component";
import { AssetHistoryComponent } from "./components/asset-history/asset-history.component";
import { AssetTableComponent } from "./components/asset-table/asset-table.component";
import { Asset, AssetType } from "./model/asset.model";
import { Currency } from "../profile/model/currency.model";
import { Locale } from "../profile/model/locale.model";
import { UserProfile } from "../profile/model/user-profile.model";

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
	latestValue: faker.number.float({ min: 100, max: 10000 }),
	latestValueBase: faker.number.float({ min: 100, max: 10000 }),
	...overrides
});

const buildMockProfile = (): UserProfile => ({
	id: faker.string.uuid(),
	email: faker.internet.email(),
	currency: Currency.EUR,
	locale: Locale.EN_US
});

describe("AssetComponent", () => {
	let fixture: ComponentFixture<AssetComponent>;
	let testSubject: AssetComponent;
	let mockAssetService: AssetService;
	let mockConfirmationService: ConfirmationService;

	beforeEach(() => {
		TestBed.configureTestingModule({
			imports: [
				AssetComponent,
				MockComponent(Button),
				MockModule(TableModule),
				MockComponent(AssetFormComponent),
				MockModule(FormsModule),
				MockModule(ReactiveFormsModule),
				MockDirective(RouterLink),
				MockComponent(AssetHistoryComponent),
				MockComponent(AssetTableComponent),
				MockComponent(Dialog),
				MockComponent(DatePicker)
			],
			providers: [
				MockProvider(AssetService, {
					getAssets: vi.fn().mockReturnValue(of([])),
					getAssetCategories: vi.fn().mockReturnValue(of([])),
					saveAsset: vi.fn().mockReturnValue(of([])),
					patchAssetStatus: vi.fn().mockReturnValue(of([])),
					deleteAssetPermanently: vi.fn().mockReturnValue(of([]))
				}),
				MockProvider(ConfirmationService),
				MockProvider(NavigationService, { previousRoute: "/assets" }),
				MockProvider(ProfileService, {
					getProfile: vi.fn().mockReturnValue(of(buildMockProfile()))
				}),
				MockProvider(ThemeService, { isDarkMode: signal(false), applyLocale: vi.fn() })
			]
		});

		mockAssetService = TestBed.inject(AssetService);
		mockConfirmationService = TestBed.inject(ConfirmationService);

		fixture = TestBed.createComponent(AssetComponent);
		testSubject = fixture.componentInstance;
		fixture.detectChanges();
	});

	describe("ngOnInit", () => {
		it("should load assets on init", () => {
			// GIVEN
			const stubbedAssets = [buildMockAsset(), buildMockAsset()];
			vi.spyOn(mockAssetService, "getAssets").mockReturnValue(of(stubbedAssets));

			// WHEN
			testSubject.ngOnInit();

			// THEN
			expect(testSubject.assets()).toHaveLength(stubbedAssets.length);
		});

		it("should set userCurrency and userLocale from profile on init", () => {
			// GIVEN
			const stubbedProfile: UserProfile = {
				...buildMockProfile(),
				currency: Currency.USD,
				locale: Locale.EN_US
			};
			vi.spyOn(TestBed.inject(ProfileService), "getProfile").mockReturnValue(of(stubbedProfile));

			// WHEN
			testSubject.ngOnInit();

			// THEN
			expect(testSubject.userCurrency()).toBe(Currency.USD);
		});

		it("should update the selectedAsset reference when it is in the refreshed list", () => {
			// GIVEN
			const mockAsset = buildMockAsset();
			testSubject.selectedAsset.set(mockAsset);
			const updatedAsset = { ...mockAsset, name: "Updated Name" };
			vi.spyOn(mockAssetService, "getAssets").mockReturnValue(of([updatedAsset]));

			// WHEN
			testSubject.refreshAssets();

			// THEN
			expect(testSubject.selectedAsset()?.name).toBe("Updated Name");
		});
	});

	describe("viewHistory", () => {
		it("should set selectedAsset and open the history dialog", () => {
			// GIVEN
			const mockAsset = buildMockAsset();

			// WHEN
			testSubject.viewHistory(mockAsset);

			// THEN
			expect(testSubject.selectedAsset()?.id).toBe(mockAsset.id);
			expect(testSubject.isHistoryDialogVisible()).toBe(true);
		});
	});

	describe("editAsset", () => {
		it("should set selectedAsset and open the drawer", () => {
			// GIVEN
			const mockAsset = buildMockAsset();

			// WHEN
			testSubject.editAsset(mockAsset);

			// THEN
			expect(testSubject.selectedAsset()?.id).toBe(mockAsset.id);
			expect(testSubject.isDrawerVisible()).toBe(true);
		});
	});

	describe("toggleAssetStatus", () => {
		it("should set pending status toggle and open the status dialog", () => {
			// GIVEN
			const mockAsset = buildMockAsset({ isActive: true });

			// WHEN
			testSubject.toggleAssetStatus(mockAsset);

			// THEN
			expect(testSubject.isStatusDialogVisible()).toBe(true);
		});
	});

	describe("cancelStatusToggle", () => {
		it("should close the status dialog and clear the pending toggle", () => {
			// GIVEN
			const mockAsset = buildMockAsset();
			testSubject.toggleAssetStatus(mockAsset);

			// WHEN
			testSubject.cancelStatusToggle();

			// THEN
			expect(testSubject.isStatusDialogVisible()).toBe(false);
			expect(testSubject.pendingStatusToggle()).toBeNull();
		});
	});

	describe("confirmStatusToggle", () => {
		it("should do nothing when pendingStatusToggle is null", () => {
			// GIVEN
			testSubject.pendingStatusToggle.set(null);
			const patchAssetStatus = vi.spyOn(mockAssetService, "patchAssetStatus");

			// WHEN
			testSubject.confirmStatusToggle();

			// THEN
			expect(patchAssetStatus).not.toHaveBeenCalled();
		});

		it("should call patchAssetStatus with asset id and new status and update assets on success", () => {
			// GIVEN
			const mockAsset = buildMockAsset({ isActive: true });
			const stubbedAssets = [{ ...mockAsset, isActive: false }];
			testSubject.pendingStatusToggle.set({ asset: mockAsset, newStatus: false });
			const patchAssetStatus = vi
				.spyOn(mockAssetService, "patchAssetStatus")
				.mockReturnValue(of(stubbedAssets));

			// WHEN
			testSubject.confirmStatusToggle();
			fixture.detectChanges();

			// THEN
			expect(patchAssetStatus).toHaveBeenCalledWith(mockAsset.id, false, expect.any(String));
			expect(testSubject.assets()).toHaveLength(1);
			expect(testSubject.isStatusDialogVisible()).toBe(false);
			expect(testSubject.isSaveLoading()).toBe(false);
		});
	});

	describe("deleteAsset", () => {
		it("should do nothing when asset id is missing", () => {
			// GIVEN
			const confirm = vi.spyOn(mockConfirmationService, "confirm");
			const mockAsset = buildMockAsset({ id: undefined });

			// WHEN
			testSubject.deleteAsset({
				event: { originalEvent: { target: document.body } } as never,
				asset: mockAsset
			});

			// THEN
			expect(confirm).not.toHaveBeenCalled();
		});

		it("should call confirmationService.confirm with the asset target", () => {
			// GIVEN
			const confirm = vi.spyOn(mockConfirmationService, "confirm");
			const mockAsset = buildMockAsset();

			// WHEN
			testSubject.deleteAsset({
				event: { originalEvent: { target: document.body } } as never,
				asset: mockAsset
			});

			// THEN
			expect(confirm).toHaveBeenCalled();
		});

		it("should call deleteAssetPermanently with the asset id when confirmed", () => {
			// GIVEN
			vi.spyOn(mockConfirmationService, "confirm").mockImplementation(({ accept }) => accept?.());
			const deleteAssetPermanently = vi
				.spyOn(mockAssetService, "deleteAssetPermanently")
				.mockReturnValue(of([]));
			const mockAsset = buildMockAsset();

			// WHEN
			testSubject.deleteAsset({
				event: { originalEvent: { target: document.body } } as never,
				asset: mockAsset
			});
			fixture.detectChanges();

			// THEN
			expect(deleteAssetPermanently).toHaveBeenCalledWith(mockAsset.id);
			expect(testSubject.isDeletePermanentlyLoading()).toBe(false);
		});
	});

	describe("saveAssetAndSnapshot", () => {
		it("should call assetService.saveAsset with the asset and update the assets list on success", () => {
			// GIVEN
			const mockAsset = buildMockAsset();
			const stubbedAssets = [mockAsset];
			const saveAsset = vi.spyOn(mockAssetService, "saveAsset").mockReturnValue(of(stubbedAssets));

			// WHEN
			testSubject.saveAssetAndSnapshot(mockAsset);

			// THEN
			expect(saveAsset).toHaveBeenCalledWith(mockAsset);
			expect(testSubject.assets()).toHaveLength(stubbedAssets.length);
			expect(testSubject.isSaveLoading()).toBe(false);
		});
	});
});
