import { ComponentFixture, TestBed } from "@angular/core/testing";
import { signal } from "@angular/core";
import { MockComponent, MockDirective, MockProvider } from "ng-mocks";
import { provideRouter, RouterLink } from "@angular/router";
import { faker } from "@faker-js/faker";
import { of } from "rxjs";
import { Button } from "primeng/button";
import { ConfirmationService } from "primeng/api";
import { CategoryComponent } from "./category.component";
import { CategoryItemComponent } from "./category-item/category-item.component";
import { NavigationService } from "../../shared/services/navigation/navigation.service";
import { AssetService } from "../asset/asset.service";
import { AssetCategory, AssetType } from "../asset/model/asset.model";
import { CategoryFormComponent } from "./category-form/category-form.component";

const buildMockCategory = (overrides: Partial<AssetCategory> = {}): AssetCategory => ({
	id: faker.string.uuid(),
	name: faker.lorem.word(),
	type: AssetType.ASSET,
	icon: null,
	isDefault: false,
	...overrides
});

describe("CategoryComponent", () => {
	let fixture: ComponentFixture<CategoryComponent>;
	let testSubject: CategoryComponent;
	let mockAssetService: AssetService;
	let mockConfirmationService: ConfirmationService;

	beforeEach(() => {
		TestBed.configureTestingModule({
			imports: [
				CategoryComponent,
				MockComponent(Button),
				MockDirective(RouterLink),
				MockComponent(CategoryFormComponent),
				MockComponent(CategoryItemComponent)
			],
			providers: [
				MockProvider(AssetService, {
					getAssetCategories: vi.fn().mockReturnValue(of([])),
					saveCategory: vi.fn().mockReturnValue(of({ categories: [], assets: [] })),
					deleteCategory: vi.fn().mockReturnValue(of({ categories: [], assets: [] }))
				}),
				MockProvider(NavigationService, { previousRoute: signal("/assets") }),
				MockProvider(ConfirmationService),
				provideRouter([])
			]
		});
		fixture = TestBed.createComponent(CategoryComponent);
		testSubject = fixture.componentInstance;
		mockAssetService = TestBed.inject(AssetService);
		mockConfirmationService = TestBed.inject(ConfirmationService);
		fixture.detectChanges();
	});

	describe("ngOnInit", () => {
		it("should load categories on init", () => {
			// GIVEN
			const stubbedCategories = [buildMockCategory(), buildMockCategory()];
			vi.spyOn(mockAssetService, "getAssetCategories").mockReturnValue(of(stubbedCategories));

			// WHEN
			testSubject.ngOnInit();

			// THEN
			expect(testSubject.categories()).toHaveLength(2);
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

			// WHEN
			testSubject.ngOnInit();

			// THEN
			expect(testSubject.defaultCategories()).toHaveLength(1);
			expect(testSubject.customCategories()).toHaveLength(1);
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
		it("should call assetService.saveCategory with the category and update the categories list on success", () => {
			// GIVEN
			const mockCategory = buildMockCategory();
			const stubbedCategories = [mockCategory];
			const saveCategory = vi
				.spyOn(mockAssetService, "saveCategory")
				.mockReturnValue(of({ categories: stubbedCategories, assets: [] }));

			// WHEN
			testSubject.saveCategory(mockCategory);
			fixture.detectChanges();

			// THEN
			expect(saveCategory).toHaveBeenCalledWith(mockCategory);
			expect(testSubject.categories()).toHaveLength(1);
			expect(testSubject.isDialogVisible()).toBe(false);
		});
	});

	describe("deleteCategory", () => {
		it("should call assetService.deleteCategory and update categories when confirmed", () => {
			// GIVEN
			const mockCategory = buildMockCategory();
			const stubbedCategories: AssetCategory[] = [];
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

		it("should not call assetService.deleteCategory when id is empty", () => {
			// GIVEN
			vi.spyOn(mockConfirmationService, "confirm").mockImplementation(({ accept }) => accept?.());
			const deleteCategory = vi.spyOn(mockAssetService, "deleteCategory");

			// WHEN
			testSubject.deleteCategory({ target: document.body as EventTarget, id: "" });

			// THEN
			expect(deleteCategory).not.toHaveBeenCalled();
		});
	});
});
