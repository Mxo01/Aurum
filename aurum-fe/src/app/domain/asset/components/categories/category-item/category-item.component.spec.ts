import { ComponentFixture, TestBed } from "@angular/core/testing";
import { faker } from "@faker-js/faker";
import { MockComponent } from "ng-mocks";
import { Button } from "primeng/button";
import { CategoryItemComponent } from "./category-item.component";
import { AssetCategory, AssetType } from "../../../model/asset.model";

const buildMockCategory = (overrides: Partial<AssetCategory> = {}): AssetCategory => ({
	id: faker.string.uuid(),
	name: faker.lorem.word(),
	type: AssetType.ASSET,
	icon: null,
	isDefault: false,
	...overrides
});

describe("CategoryItemComponent", () => {
	let fixture: ComponentFixture<CategoryItemComponent>;
	let testSubject: CategoryItemComponent;

	beforeEach(() => {
		TestBed.configureTestingModule({
			imports: [CategoryItemComponent, MockComponent(Button)]
		});
		fixture = TestBed.createComponent(CategoryItemComponent);
		testSubject = fixture.componentInstance;
	});

	describe("categoryColors", () => {
		it("should use blue tones for an ASSET category", () => {
			// GIVEN
			fixture.componentRef.setInput("category", buildMockCategory({ type: AssetType.ASSET }));
			fixture.detectChanges();

			// THEN
			expect(testSubject.categoryColors().bg).toContain("#0ea5e9");
			expect(testSubject.categoryColors().text).toBe("#7dd3fc");
		});

		it("should use orange tones for a LIABILITY category", () => {
			// GIVEN
			fixture.componentRef.setInput("category", buildMockCategory({ type: AssetType.LIABILITY }));
			fixture.detectChanges();

			// THEN
			expect(testSubject.categoryColors().bg).toContain("#f97316");
			expect(testSubject.categoryColors().text).toBe("#fdba74");
		});
	});

	describe("edit output", () => {
		it("should emit the category when the edit button is clicked on a non-default category", () => {
			// GIVEN
			const mockCategory = buildMockCategory({ isDefault: false });
			fixture.componentRef.setInput("category", mockCategory);
			fixture.detectChanges();
			const emitted: AssetCategory[] = [];
			testSubject.edit.subscribe(v => emitted.push(v));

			// WHEN
			testSubject.edit.emit(mockCategory);

			// THEN
			expect(emitted).toHaveLength(1);
			expect(emitted[0]).toBe(mockCategory);
		});
	});
});
