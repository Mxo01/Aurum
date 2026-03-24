import { ComponentFixture, TestBed } from "@angular/core/testing";
import { faker } from "@faker-js/faker";
import { TopAssetsWidgetComponent } from "./top-assets-widget.component";
import { Asset, AssetType } from "../../../asset/model/asset.model";
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
	latestValue: faker.number.float({ min: 100, max: 10000 }),
	latestValueBase: faker.number.float({ min: 100, max: 10000 }),
	...overrides
});

describe("TopAssetsWidgetComponent", () => {
	let fixture: ComponentFixture<TopAssetsWidgetComponent>;
	let testSubject: TopAssetsWidgetComponent;

	beforeEach(() => {
		TestBed.overrideComponent(TopAssetsWidgetComponent, { set: { template: "", imports: [] } });
		TestBed.configureTestingModule({ imports: [TopAssetsWidgetComponent] });
		fixture = TestBed.createComponent(TopAssetsWidgetComponent);
		testSubject = fixture.componentInstance;
		fixture.componentRef.setInput("assets", []);
		fixture.componentRef.setInput("summary", null);
		fixture.detectChanges();
	});

	describe("assetsWithBalance", () => {
		it("should compute assetsWithBalance from the assets input", () => {
			// GIVEN
			const mockAssets = [buildMockAsset({ latestValue: 1000, previousValue: 800 })];
			fixture.componentRef.setInput("assets", mockAssets);
			fixture.detectChanges();

			// THEN
			expect(testSubject.assetsWithBalance()).toHaveLength(1);
			expect(testSubject.assetsWithBalance()[0].currentValue).toBe(1000);
		});
	});

	describe("viewMore output", () => {
		it("should emit when viewMore is triggered", () => {
			// GIVEN
			const spy = vi.fn();
			testSubject.viewMore.subscribe(() => spy());

			// WHEN
			testSubject.viewMore.emit();

			// THEN
			expect(spy).toHaveBeenCalledTimes(1);
		});
	});
});
