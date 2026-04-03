import { ComponentFixture, TestBed } from "@angular/core/testing";
import { faker } from "@faker-js/faker";
import { MockComponent, MockDirective, MockModule } from "ng-mocks";
import { ReactiveFormsModule } from "@angular/forms";
import { InputText } from "primeng/inputtext";
import { ToggleButton } from "primeng/togglebutton";
import { Select } from "primeng/select";
import { InputNumber } from "primeng/inputnumber";
import { DatePicker } from "primeng/datepicker";
import { Button } from "primeng/button";
import { Drawer } from "primeng/drawer";
import { AssetFormComponent } from "./asset-form.component";
import { Asset, AssetCategory, AssetType, LiabilityType } from "../../model/asset.model";
import { Currency } from "../../../profile/model/currency.model";

const buildMockCategory = (overrides: Partial<AssetCategory> = {}): AssetCategory => ({
	id: faker.string.uuid(),
	name: faker.lorem.word(),
	type: AssetType.ASSET,
	icon: null,
	isDefault: false,
	...overrides
});

const buildMockAsset = (overrides: Partial<Asset> = {}): Asset => ({
	id: faker.string.uuid(),
	name: faker.lorem.word(),
	categoryId: faker.string.uuid(),
	categoryName: faker.lorem.word(),
	categoryIcon: null,
	type: AssetType.ASSET,
	originalCurrency: Currency.EUR,
	isFavorite: false,
	...overrides
});

describe("AssetFormComponent", () => {
	let fixture: ComponentFixture<AssetFormComponent>;
	let testSubject: AssetFormComponent;
	const mockCategory = buildMockCategory({ type: AssetType.ASSET });

	beforeEach(() => {
		TestBed.configureTestingModule({
			imports: [
				AssetFormComponent,
				MockModule(ReactiveFormsModule),
				MockDirective(InputText),
				MockComponent(ToggleButton),
				MockComponent(Select),
				MockComponent(InputNumber),
				MockComponent(DatePicker),
				MockComponent(Button),
				MockComponent(Drawer)
			]
		});

		fixture = TestBed.createComponent(AssetFormComponent);
		testSubject = fixture.componentInstance;

		fixture.componentRef.setInput("isVisible", true);
		fixture.componentRef.setInput("categoriesOptions", [mockCategory]);
	});

	describe("effect — selectedAsset patching", () => {
		it("should patch the form with the selected asset values when an asset is provided", () => {
			// GIVEN
			const mockAsset = buildMockAsset({
				categoryId: mockCategory.id,
				name: "My Asset",
				originalCurrency: Currency.USD
			});
			fixture.componentRef.setInput("selectedAsset", mockAsset);

			// WHEN
			fixture.detectChanges();

			// THEN
			expect(testSubject.assetForm.controls.name.value).toBe("My Asset");
		});

		it("should disable the currency control when editing an existing asset", () => {
			// GIVEN
			const mockAsset = buildMockAsset({ categoryId: mockCategory.id });
			fixture.componentRef.setInput("selectedAsset", mockAsset);

			// WHEN
			fixture.detectChanges();

			// THEN
			expect(testSubject.assetForm.controls.currency.disabled).toBe(true);
		});

		it("should reset the form when selectedAsset is null", () => {
			// GIVEN
			const mockAsset = buildMockAsset({ categoryId: mockCategory.id, name: "Some Asset" });
			fixture.componentRef.setInput("selectedAsset", mockAsset);

			// WHEN
			fixture.componentRef.setInput("selectedAsset", null);
			fixture.detectChanges();

			// THEN
			expect(testSubject.assetForm.controls.name.value).toBeFalsy();
		});
	});

	describe("liabilityType validators", () => {
		it("should add required validators on paymentFrequency and paymentAmount for AUTOMATIC liability", () => {
			// GIVEN
			fixture.componentRef.setInput("selectedAsset", null);
			fixture.detectChanges();

			// WHEN
			testSubject.assetForm.controls.liabilityType.setValue(LiabilityType.AUTOMATIC);

			// THEN
			expect(testSubject.assetForm.controls.paymentFrequency.validator).not.toBeNull();
			expect(testSubject.assetForm.controls.paymentAmount.validator).not.toBeNull();
		});

		it("should clear validators and reset paymentFrequency and paymentAmount for non-AUTOMATIC liability", () => {
			// GIVEN
			fixture.componentRef.setInput("selectedAsset", null);
			fixture.detectChanges();
			testSubject.assetForm.controls.liabilityType.setValue(LiabilityType.AUTOMATIC);

			// WHEN
			testSubject.assetForm.controls.liabilityType.setValue(LiabilityType.MANUAL);

			// THEN
			expect(testSubject.assetForm.controls.paymentFrequency.value).toBeNull();
			expect(testSubject.assetForm.controls.paymentAmount.value).toBeNull();
		});
	});

	describe("saveAssetFromForm", () => {
		it("should emit the save output with the correctly formatted asset for a new asset", () => {
			// GIVEN
			fixture.componentRef.setInput("selectedAsset", null);
			fixture.detectChanges();
			testSubject.assetForm.patchValue({
				name: "New Asset",
				category: mockCategory,
				type: AssetType.ASSET,
				currency: Currency.EUR,
				isFavorite: false,
				referenceDate: new Date("2024-01-15")
			});
			testSubject.assetForm.controls.type.enable();
			const emitted: Asset[] = [];
			testSubject.save.subscribe(a => emitted.push(a));

			// WHEN
			testSubject.saveAssetFromForm();

			// THEN
			expect(emitted).toHaveLength(1);
			expect(emitted[0].name).toBe("New Asset");
			expect(emitted[0].referenceDate).toBe("2024-01-15");
		});

		it("should not emit when required fields are missing", () => {
			// GIVEN
			fixture.componentRef.setInput("selectedAsset", null);
			fixture.detectChanges();
			const emitted: Asset[] = [];
			testSubject.save.subscribe(a => emitted.push(a));

			// WHEN
			testSubject.saveAssetFromForm();

			// THEN
			expect(emitted).toHaveLength(0);
		});

		it("should not include initialValue for an existing asset", () => {
			// GIVEN
			const mockAsset = buildMockAsset({ categoryId: mockCategory.id });
			fixture.componentRef.setInput("selectedAsset", mockAsset);
			fixture.detectChanges();
			testSubject.assetForm.controls.type.enable();
			testSubject.assetForm.patchValue({
				name: mockAsset.name,
				category: mockCategory,
				type: AssetType.ASSET,
				currency: Currency.EUR
			});
			const emitted: Asset[] = [];
			testSubject.save.subscribe(a => emitted.push(a));

			// WHEN
			testSubject.saveAssetFromForm();

			// THEN
			expect(emitted).toHaveLength(1);
			expect(emitted[0].initialValue).toBeNull();
			expect(emitted[0].referenceDate).toBeNull();
		});
	});

	describe("hideDrawer", () => {
		it("should reset the form and clear selectedAsset on hide", () => {
			// GIVEN
			const mockAsset = buildMockAsset({ categoryId: mockCategory.id });
			fixture.componentRef.setInput("selectedAsset", mockAsset);
			fixture.detectChanges();

			// WHEN
			testSubject.hideDrawer();
			fixture.detectChanges();

			// THEN
			expect(testSubject.selectedAsset()).toBeNull();
		});
	});

	describe("category valueChanges", () => {
		it("should auto-set the type control when a category is selected", () => {
			// GIVEN
			fixture.componentRef.setInput("selectedAsset", null);
			fixture.detectChanges();
			const mockLiabilityCategory = buildMockCategory({ type: AssetType.LIABILITY });

			// WHEN
			testSubject.assetForm.controls.category.setValue(mockLiabilityCategory);

			// THEN
			expect(testSubject.assetForm.controls.type.value).toBe(AssetType.LIABILITY);
		});
	});
});
