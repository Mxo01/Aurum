import { ComponentFixture, TestBed } from "@angular/core/testing";
import { faker } from "@faker-js/faker";
import { MockComponent, MockDirective, MockModule, MockPipe } from "ng-mocks";
import { DecimalPipe } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { PrivacyCurrencyPipe } from "../../../../shared/pipes/privacy-currency.pipe";
import { TableModule } from "primeng/table";
import { Tag } from "primeng/tag";
import { Badge } from "primeng/badge";
import { Button } from "primeng/button";
import { InputNumber } from "primeng/inputnumber";
import { Menu } from "primeng/menu";
import { AssetTableComponent } from "./asset-table.component";
import { Asset, AssetType } from "../../model/asset.model";
import { Currency } from "../../../profile/model/currency.model";
import { NoArrowSpinDirective } from "../../../../shared/directives/no-arrow-spin.directive";

const buildMockAsset = (overrides: Partial<Asset> = {}): Asset => ({
	id: faker.string.uuid(),
	name: faker.lorem.word(),
	categoryId: faker.string.uuid(),
	categoryName: "Stocks",
	categoryIcon: null,
	type: AssetType.ASSET,
	originalCurrency: Currency.EUR,
	isFavorite: false,
	latestValue: 1000,
	latestValueBase: 1000,
	...overrides
});

describe("AssetTableComponent", () => {
	let fixture: ComponentFixture<AssetTableComponent>;
	let testSubject: AssetTableComponent;

	beforeEach(() => {
		TestBed.configureTestingModule({
			imports: [
				AssetTableComponent,
				MockModule(TableModule),
				MockModule(FormsModule),
				MockComponent(Tag),
				MockComponent(Badge),
				MockComponent(Button),
				MockComponent(InputNumber),
				MockDirective(NoArrowSpinDirective),
				MockPipe(PrivacyCurrencyPipe),
				MockPipe(DecimalPipe),
				MockComponent(Menu)
			]
		});
		fixture = TestBed.createComponent(AssetTableComponent);
		testSubject = fixture.componentInstance;
		fixture.componentRef.setInput("assets", []);
		fixture.componentRef.setInput("isLoading", false);
		fixture.detectChanges();
	});

	describe("assetsWithBalance", () => {
		it("should compute assetsWithBalance from the assets input", () => {
			// GIVEN
			const mockAssets = [buildMockAsset({ latestValue: 5000, latestValueBase: 5500 })];
			fixture.componentRef.setInput("assets", mockAssets);
			fixture.detectChanges();

			// THEN
			expect(testSubject.assetsWithBalance()[0].currentValue).toBe(5000);
			expect(testSubject.assetsWithBalance()[0].currentValueBase).toBe(5500);
		});
	});

	describe("categoryCounts", () => {
		it("should count assets per category name", () => {
			// GIVEN
			const mockAssets = [
				buildMockAsset({ categoryName: "Stocks" }),
				buildMockAsset({ categoryName: "Stocks" }),
				buildMockAsset({ categoryName: "Cash" })
			];
			fixture.componentRef.setInput("assets", mockAssets);
			fixture.detectChanges();

			// THEN
			expect(testSubject.categoryCounts()["Stocks"]).toBe(2);
			expect(testSubject.categoryCounts()["Cash"]).toBe(1);
		});
	});

	describe("categoryTotals", () => {
		it("should sum latestValueBase per category name", () => {
			// GIVEN
			const mockAssets = [
				buildMockAsset({ categoryName: "Stocks", latestValueBase: 1000 }),
				buildMockAsset({ categoryName: "Stocks", latestValueBase: 2000 })
			];
			fixture.componentRef.setInput("assets", mockAssets);
			fixture.detectChanges();

			// THEN
			expect(testSubject.categoryTotals()["Stocks"]).toBe(3000);
		});
	});

	describe("showMenu", () => {
		it("should set three menu items when called with an asset", () => {
			// GIVEN
			const mockAsset = buildMockAsset();
			const mockMenu = { toggle: vi.fn() };

			// WHEN
			testSubject.showMenu(new MouseEvent("click"), mockMenu as never, mockAsset);

			// THEN
			expect(testSubject.rowMenuItems()).toHaveLength(3);
			expect(testSubject.rowMenuItems()[0].label).toBe("Edit");
		});

		it("should emit editAsset when the Edit command is invoked", () => {
			// GIVEN
			const mockAsset = buildMockAsset();
			const mockMenu = { toggle: vi.fn() };
			const emitted: Asset[] = [];
			testSubject.editAsset.subscribe(a => emitted.push(a));
			testSubject.showMenu(new MouseEvent("click"), mockMenu as never, mockAsset);

			// WHEN
			testSubject.rowMenuItems()[0].command!({} as never);

			// THEN
			expect(emitted[0].id).toBe(mockAsset.id);
		});

		it("should emit viewHistory when the View History command is invoked", () => {
			// GIVEN
			const mockAsset = buildMockAsset();
			const mockMenu = { toggle: vi.fn() };
			const emitted: Asset[] = [];
			testSubject.viewHistory.subscribe(a => emitted.push(a));
			testSubject.showMenu(new MouseEvent("click"), mockMenu as never, mockAsset);

			// WHEN
			testSubject.rowMenuItems()[1].command!({} as never);

			// THEN
			expect(emitted[0].id).toBe(mockAsset.id);
		});

		it("should emit deleteAsset when the Delete Forever command is invoked", () => {
			// GIVEN
			const mockAsset = buildMockAsset();
			const mockMenu = { toggle: vi.fn() };
			const emitted: { asset: Asset }[] = [];
			testSubject.deleteAsset.subscribe(e => emitted.push(e));
			testSubject.showMenu(new MouseEvent("click"), mockMenu as never, mockAsset);

			// WHEN
			testSubject.rowMenuItems()[2].command!({} as never);

			// THEN
			expect(emitted[0].asset.id).toBe(mockAsset.id);
		});
	});

	describe("onCurrentValueChange", () => {
		it("should update editingValues with the new value", () => {
			// GIVEN
			const mockAsset = buildMockAsset({ id: "asset-1" });
			fixture.componentRef.setInput("assets", [mockAsset]);
			fixture.detectChanges();

			// WHEN
			testSubject.onCurrentValueChange("asset-1", 5000);

			// THEN
			expect(testSubject.editingValues()["asset-1"]).toBe(5000);
		});

		it("should not update editingValues or emit when value is null", () => {
			// GIVEN
			const mockAsset = buildMockAsset({ id: "asset-1" });
			fixture.componentRef.setInput("assets", [mockAsset]);
			fixture.detectChanges();
			const emitted: { assetId: string; value: number }[] = [];
			testSubject.currentValueUpdate.subscribe(e => emitted.push(e));

			// WHEN
			testSubject.onCurrentValueChange("asset-1", null);
			testSubject.onCellEditBlur("asset-1");

			// THEN
			expect(testSubject.editingValues()["asset-1"]).toBeUndefined();
			expect(emitted).toHaveLength(0);
		});

		it("should emit currentValueUpdate on blur after value change", () => {
			// GIVEN
			const mockAsset = buildMockAsset({ id: "asset-1" });
			fixture.componentRef.setInput("assets", [mockAsset]);
			fixture.detectChanges();
			const emitted: { assetId: string; value: number }[] = [];
			testSubject.currentValueUpdate.subscribe(e => emitted.push(e));

			// WHEN
			testSubject.onCurrentValueChange("asset-1", 2000);
			testSubject.onCellEditBlur("asset-1");

			// THEN
			expect(emitted).toHaveLength(1);
			expect(emitted[0]).toEqual({ assetId: "asset-1", value: 2000 });
		});

		it("should not emit currentValueUpdate for duplicate consecutive values", () => {
			// GIVEN
			const mockAsset = buildMockAsset({ id: "asset-1" });
			fixture.componentRef.setInput("assets", [mockAsset]);
			fixture.detectChanges();
			const emitted: { assetId: string; value: number }[] = [];
			testSubject.currentValueUpdate.subscribe(e => emitted.push(e));

			// WHEN
			testSubject.onCurrentValueChange("asset-1", 2000);
			testSubject.onCellEditBlur("asset-1");
			testSubject.onCurrentValueChange("asset-1", 2000);
			testSubject.onCellEditBlur("asset-1");

			// THEN
			expect(emitted).toHaveLength(1);
		});
	});
});
