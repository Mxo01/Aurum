import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MockProvider } from "ng-mocks";
import { faker } from "@faker-js/faker";
import { of } from "rxjs";
import { ConfirmationService } from "primeng/api";
import { CategoriesComponent } from "./categories.component";
import { AssetService } from "../../asset.service";
import { NavigationService } from "../../../../shared/services/navigation/navigation.service";
import { AssetCategory, AssetType } from "../../model/asset.model";

const buildMockCategory = (overrides: Partial<AssetCategory> = {}): AssetCategory => ({
	id: faker.string.uuid(),
	name: faker.lorem.word(),
	type: AssetType.ASSET,
	icon: null,
	isDefault: false,
	...overrides
});

describe("CategoriesComponent", () => {
	let fixture: ComponentFixture<CategoriesComponent>;
	let testSubject: CategoriesComponent;
	let mockAssetService: AssetService;

	beforeEach(() => {
		TestBed.overrideComponent(CategoriesComponent, { set: { template: "", imports: [] } });
		TestBed.configureTestingModule({
			imports: [CategoriesComponent],
			providers: [
				MockProvider(AssetService, {
					getAssetCategories: vi.fn().mockReturnValue(of([])),
					saveCategory: vi.fn().mockReturnValue(of({ categories: [], assets: [] })),
					deleteCategory: vi.fn().mockReturnValue(of({ categories: [], assets: [] }))
				}),
				MockProvider(NavigationService, { previousRoute: "/assets" }),
				MockProvider(ConfirmationService)
			]
		});
		fixture = TestBed.createComponent(CategoriesComponent);
		testSubject = fixture.componentInstance;
		mockAssetService = TestBed.inject(AssetService);
		fixture.detectChanges();
	});

	describe("ngOnInit", () => {
		it("should load categories on init", () => {
			// GIVEN
			const stubbedCategories = [buildMockCategory(), buildMockCategory()];
			vi.spyOn(mockAssetService, "getAssetCategories").mockReturnValue(of(stubbedCategories));
			fixture = TestBed.createComponent(CategoriesComponent);

			// WHEN
			fixture.detectChanges();

			// THEN
			expect(fixture.componentInstance.categories()).toHaveLength(2);
		});
	});

	describe("defaultCategories and customCategories", () => {
		it("should separate default and custom categories", () => {
			// GIVEN
			const mockDefault = buildMockCategory({ isDefault: true });
			const mockCustom = buildMockCategory({ isDefault: false });
			vi.spyOn(mockAssetService, "getAssetCategories").mockReturnValue(
				of([mockDefault, mockCustom])
			);
			fixture = TestBed.createComponent(CategoriesComponent);

			// WHEN
			fixture.detectChanges();

			// THEN
			expect(fixture.componentInstance.defaultCategories()).toHaveLength(1);
			expect(fixture.componentInstance.customCategories()).toHaveLength(1);
		});
	});

	describe("openAdd", () => {
		it("should clear selectedCategory and open the dialog", () => {
			// WHEN
			testSubject.openAdd();

			// THEN
			expect(testSubject.selectedCategory()).toBeNull();
			expect(testSubject.isDialogVisible()).toBe(true);
		});
	});

	describe("openEdit", () => {
		it("should set selectedCategory and open the dialog", () => {
			// GIVEN
			const mockCategory = buildMockCategory();

			// WHEN
			testSubject.openEdit(mockCategory);

			// THEN
			expect(testSubject.selectedCategory()?.id).toBe(mockCategory.id);
			expect(testSubject.isDialogVisible()).toBe(true);
		});
	});

	describe("saveCategory", () => {
		it("should call assetService.saveCategory and update the categories list on success", () => {
			// GIVEN
			const mockCategory = buildMockCategory();
			const stubbedCategories = [mockCategory];
			vi.spyOn(mockAssetService, "saveCategory").mockReturnValue(
				of({ categories: stubbedCategories, assets: [] })
			);

			// WHEN
			testSubject.saveCategory(mockCategory);
			fixture.detectChanges();

			// THEN
			expect(testSubject.categories()).toHaveLength(1);
			expect(testSubject.isDialogVisible()).toBe(false);
		});
	});

	describe("deleteCategory", () => {
		it("should call assetService.deleteCategory and update categories when confirmed", () => {
			// GIVEN
			const mockCategory = buildMockCategory();
			const stubbedCategories: AssetCategory[] = [];
			const mockConfirmationService = TestBed.inject(ConfirmationService);
			vi.spyOn(mockConfirmationService, "confirm").mockImplementation(({ accept }) => accept?.());
			vi.spyOn(mockAssetService, "deleteCategory").mockReturnValue(
				of({ categories: stubbedCategories, assets: [] })
			);

			// WHEN
			testSubject.deleteCategory({ target: document.body as EventTarget, id: mockCategory.id! });
			fixture.detectChanges();

			// THEN
			expect(testSubject.categories()).toHaveLength(0);
		});
	});
});
