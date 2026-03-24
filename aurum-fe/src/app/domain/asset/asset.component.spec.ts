import { ComponentFixture, TestBed } from "@angular/core/testing";
import { signal } from "@angular/core";
import { MockComponent, MockDirective, MockModule, MockProvider } from "ng-mocks";
import { faker } from "@faker-js/faker";
import { of } from "rxjs";
import { ConfirmationService } from "primeng/api";
import { AssetComponent } from "./asset.component";
import { AssetService } from "./asset.service";
import { ProfileService } from "../profile/profile.service";
import { NavigationService } from "../../shared/services/navigation/navigation.service";
import { ThemeService } from "../../shared/services/theme/theme.service";
import { Asset, AssetType } from "./model/asset.model";
import { Currency } from "../profile/model/currency.model";
import { Locale } from "../profile/model/locale.model";
import { UserProfile } from "../profile/model/user-profile.model";
import { TableModule } from "primeng/table";
import { AssetFormComponent } from "./components/asset-form/asset-form.component";
import { AssetHistoryComponent } from "./components/asset-history/asset-history.component";
import { AssetTableComponent } from "./components/asset-table/asset-table.component";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { RouterLink } from "@angular/router";
import { Dialog } from "primeng/dialog";
import { DatePicker } from "primeng/datepicker";

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

	beforeEach(() => {
		TestBed.overrideComponent(AssetComponent, { set: { template: "", imports: [] } });
		TestBed.configureTestingModule({
			imports: [
				AssetComponent,
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
					patchAssetStatus: vi.fn().mockReturnValue(of([]))
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

		fixture = TestBed.createComponent(AssetComponent);
		testSubject = fixture.componentInstance;
	});

	describe("ngOnInit", () => {
		it("should load assets on init", () => {
			// GIVEN
			const stubbedAssets = [buildMockAsset(), buildMockAsset()];
			vi.spyOn(mockAssetService, "getAssets").mockReturnValue(of(stubbedAssets));

			// WHEN
			fixture.detectChanges();

			// THEN
			expect(fixture.componentInstance.assets()).toHaveLength(stubbedAssets.length);
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
		it("should close the status dialog", () => {
			// GIVEN
			const mockAsset = buildMockAsset();
			testSubject.toggleAssetStatus(mockAsset);

			// WHEN
			testSubject.cancelStatusToggle();

			// THEN
			expect(testSubject.isStatusDialogVisible()).toBe(false);
		});
	});

	describe("saveAssetAndSnapshot", () => {
		it("should call assetService.saveAsset and update the assets list on success", () => {
			// GIVEN
			const mockAsset = buildMockAsset();
			const stubbedAssets = [mockAsset];
			vi.spyOn(mockAssetService, "saveAsset").mockReturnValue(of(stubbedAssets));

			// WHEN
			testSubject.saveAssetAndSnapshot(mockAsset);

			// THEN
			expect(testSubject.assets()).toHaveLength(stubbedAssets.length);
		});
	});
});
