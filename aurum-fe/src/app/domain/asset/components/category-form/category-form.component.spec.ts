import { ComponentFixture, TestBed } from "@angular/core/testing";
import { faker } from "@faker-js/faker";
import { CategoryFormComponent } from "./category-form.component";
import { AssetCategory, AssetType } from "../../model/asset.model";

const buildMockCategory = (overrides: Partial<AssetCategory> = {}): AssetCategory => ({
	id: faker.string.uuid(),
	name: faker.lorem.word(),
	type: AssetType.ASSET,
	icon: null,
	isDefault: false,
	...overrides
});

describe("CategoryFormComponent", () => {
	let fixture: ComponentFixture<CategoryFormComponent>;
	let testSubject: CategoryFormComponent;

	beforeEach(() => {
		TestBed.overrideComponent(CategoryFormComponent, { set: { template: "", imports: [] } });
		TestBed.configureTestingModule({ imports: [CategoryFormComponent] });
		fixture = TestBed.createComponent(CategoryFormComponent);
		testSubject = fixture.componentInstance;
		fixture.componentRef.setInput("isVisible", true);
	});

	describe("effect — selectedCategory patching", () => {
		it("should patch the form with the selected category values when a category is provided", () => {
			// GIVEN
			const mockCategory = buildMockCategory({ name: "Crypto", type: AssetType.ASSET });
			fixture.componentRef.setInput("selectedCategory", mockCategory);

			// WHEN
			TestBed.flushEffects();
			fixture.detectChanges();

			// THEN
			expect(testSubject.categoryForm.controls["name"].value).toBe("Crypto");
			expect(testSubject.categoryForm.controls["type"].value).toBe(AssetType.ASSET);
		});

		it("should set the selectedIcon signal when a category with an icon is provided", () => {
			// GIVEN
			const mockCategory = buildMockCategory({ icon: "pi pi-star" });
			fixture.componentRef.setInput("selectedCategory", mockCategory);

			// WHEN
			TestBed.flushEffects();
			fixture.detectChanges();

			// THEN
			expect(testSubject.selectedIcon()).toBe("pi pi-star");
		});

		it("should reset the form when selectedCategory is null", () => {
			// GIVEN
			const mockCategory = buildMockCategory({ name: "Bonds" });
			fixture.componentRef.setInput("selectedCategory", mockCategory);
			TestBed.flushEffects();

			// WHEN
			fixture.componentRef.setInput("selectedCategory", null);
			TestBed.flushEffects();
			fixture.detectChanges();

			// THEN
			expect(testSubject.categoryForm.controls["name"].value).toBeFalsy();
			expect(testSubject.selectedIcon()).toBeNull();
		});
	});

	describe("selectIcon", () => {
		it("should set the selectedIcon and form icon control when a new icon is selected", () => {
			// GIVEN
			fixture.componentRef.setInput("selectedCategory", null);
			TestBed.flushEffects();
			fixture.detectChanges();

			// WHEN
			testSubject.selectIcon("pi pi-home");

			// THEN
			expect(testSubject.selectedIcon()).toBe("pi pi-home");
			expect(testSubject.categoryForm.controls["icon"].value).toBe("pi pi-home");
		});

		it("should deselect the icon if the same icon is selected again", () => {
			// GIVEN
			fixture.componentRef.setInput("selectedCategory", null);
			TestBed.flushEffects();
			fixture.detectChanges();
			testSubject.selectIcon("pi pi-home");

			// WHEN
			testSubject.selectIcon("pi pi-home");

			// THEN
			expect(testSubject.selectedIcon()).toBeNull();
			expect(testSubject.categoryForm.controls["icon"].value).toBeNull();
		});
	});

	describe("saveCategory", () => {
		it("should emit the save output with the correctly formatted category", () => {
			// GIVEN
			fixture.componentRef.setInput("selectedCategory", null);
			TestBed.flushEffects();
			fixture.detectChanges();
			testSubject.categoryForm.setValue({ name: "Stocks", type: AssetType.ASSET, icon: null });
			const emitted: AssetCategory[] = [];
			testSubject.save.subscribe(c => emitted.push(c));

			// WHEN
			testSubject.saveCategory();

			// THEN
			expect(emitted).toHaveLength(1);
			expect(emitted[0].name).toBe("Stocks");
			expect(emitted[0].type).toBe(AssetType.ASSET);
		});

		it("should not emit when required fields are missing", () => {
			// GIVEN
			fixture.componentRef.setInput("selectedCategory", null);
			TestBed.flushEffects();
			fixture.detectChanges();
			const emitted: AssetCategory[] = [];
			testSubject.save.subscribe(c => emitted.push(c));

			// WHEN
			testSubject.saveCategory();

			// THEN
			expect(emitted).toHaveLength(0);
		});
	});

	describe("deleteCategory", () => {
		it("should emit the delete output with the event target and category id", () => {
			// GIVEN
			const mockCategory = buildMockCategory();
			fixture.componentRef.setInput("selectedCategory", mockCategory);
			TestBed.flushEffects();
			fixture.detectChanges();
			const emitted: { target: EventTarget; id: string }[] = [];
			testSubject.delete.subscribe(e => emitted.push(e));
			const mockEvent = new MouseEvent("click");

			// WHEN
			testSubject.deleteCategory(mockEvent);

			// THEN
			expect(emitted).toHaveLength(1);
			expect(emitted[0].id).toBe(mockCategory.id);
		});
	});

	describe("onDialogHide", () => {
		it("should reset the form and clear selectedIcon on hide", () => {
			// GIVEN
			fixture.componentRef.setInput("selectedCategory", buildMockCategory({ icon: "pi pi-star" }));
			TestBed.flushEffects();
			fixture.detectChanges();

			// WHEN
			testSubject.onDialogHide();

			// THEN
			expect(testSubject.categoryForm.controls["name"].value).toBeFalsy();
			expect(testSubject.selectedIcon()).toBeNull();
		});
	});
});
