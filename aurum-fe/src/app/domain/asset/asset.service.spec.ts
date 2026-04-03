import { provideHttpClient } from "@angular/common/http";
import { HttpTestingController, provideHttpClientTesting } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { faker } from "@faker-js/faker";
import { MockProvider } from "ng-mocks";
import { MessageService } from "primeng/api";
import { lastValueFrom } from "rxjs";
import { Currency } from "../profile/model/currency.model";
import { AssetService } from "./asset.service";
import { Asset, AssetCategory, AssetType } from "./model/asset.model";

const buildMockCategory = (overrides: Partial<AssetCategory> = {}): AssetCategory => ({
	id: faker.string.uuid(),
	name: faker.lorem.word(),
	type: faker.helpers.arrayElement(Object.values(AssetType)),
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
	type: faker.helpers.arrayElement(Object.values(AssetType)),
	originalCurrency: faker.helpers.arrayElement(Object.values(Currency)),
	isFavorite: false,
	...overrides
});

describe("AssetService", () => {
	let testSubject: AssetService;
	let httpController: HttpTestingController;

	beforeEach(() => {
		TestBed.configureTestingModule({
			providers: [
				AssetService,
				MockProvider(MessageService),
				provideHttpClient(),
				provideHttpClientTesting()
			]
		});

		testSubject = TestBed.inject(AssetService);
		httpController = TestBed.inject(HttpTestingController);
	});

	afterEach(() => {
		httpController.verify();
	});

	describe("getAssets", () => {
		it("should return assets on success", async () => {
			// GIVEN
			const stubbedAssets = [buildMockAsset()];

			// WHEN
			const result = lastValueFrom(testSubject.getAssets());
			httpController
				.expectOne(req => req.method === "GET" && req.url.endsWith("/assets"))
				.flush(stubbedAssets);

			// THEN
			const expectedAssets = await result;
			expect(expectedAssets).toEqual(stubbedAssets);
		});

		it("should throw on error", async () => {
			// WHEN
			const result = lastValueFrom(testSubject.getAssets());
			httpController
				.expectOne(req => req.method === "GET" && req.url.endsWith("/assets"))
				.flush(null, { status: 500, statusText: "Server Error" });

			// THEN
			await expect(result).rejects.toThrow();
		});
	});

	describe("getAssetCategories", () => {
		it("should return categories on success", async () => {
			// GIVEN
			const stubbedCategories = [buildMockCategory()];

			// WHEN
			const result = lastValueFrom(testSubject.getAssetCategories());
			httpController
				.expectOne(req => req.method === "GET" && req.url.endsWith("/categories"))
				.flush(stubbedCategories);

			// THEN
			const expectedCategories = await result;
			expect(expectedCategories).toEqual(stubbedCategories);
		});

		it("should throw on error", async () => {
			// WHEN
			const result = lastValueFrom(testSubject.getAssetCategories());
			httpController
				.expectOne(req => req.method === "GET" && req.url.endsWith("/categories"))
				.flush(null, { status: 500, statusText: "Server Error" });

			// THEN
			await expect(result).rejects.toThrow();
		});
	});

	describe("saveAsset", () => {
		it("should make a POST request and return updated assets when the asset has no id", async () => {
			// GIVEN
			const mockNewAsset = buildMockAsset({ id: undefined });
			const stubbedAssets = [buildMockAsset()];

			// WHEN
			const result = lastValueFrom(testSubject.saveAsset(mockNewAsset));
			httpController
				.expectOne(req => req.method === "POST" && req.url.endsWith("/assets"))
				.flush(buildMockAsset());
			httpController
				.expectOne(req => req.method === "GET" && req.url.endsWith("/assets"))
				.flush(stubbedAssets);

			// THEN
			const expectedAssets = await result;
			expect(expectedAssets).toEqual(stubbedAssets);
		});

		it("should make a PUT request and return updated assets when the asset has an id", async () => {
			// GIVEN
			const mockExistingAsset = buildMockAsset();
			const stubbedAssets = [mockExistingAsset];

			// WHEN
			const result = lastValueFrom(testSubject.saveAsset(mockExistingAsset));
			httpController
				.expectOne(
					req => req.method === "PUT" && req.url.endsWith(`/assets/${mockExistingAsset.id}`)
				)
				.flush(mockExistingAsset);
			httpController
				.expectOne(req => req.method === "GET" && req.url.endsWith("/assets"))
				.flush(stubbedAssets);

			// THEN
			const expectedAssets = await result;
			expect(expectedAssets).toEqual(stubbedAssets);
		});

		it("should throw on error when creating a new asset", async () => {
			// GIVEN
			const mockAsset = buildMockAsset({ id: undefined });

			// WHEN
			const result = lastValueFrom(testSubject.saveAsset(mockAsset));
			httpController
				.expectOne(req => req.method === "POST" && req.url.endsWith("/assets"))
				.flush(null, { status: 500, statusText: "Server Error" });

			// THEN
			await expect(result).rejects.toThrow();
		});

		it("should throw on error when updating an existing asset", async () => {
			// GIVEN
			const mockAsset = buildMockAsset();

			// WHEN
			const result = lastValueFrom(testSubject.saveAsset(mockAsset));
			httpController
				.expectOne(req => req.method === "PUT" && req.url.endsWith(`/assets/${mockAsset.id}`))
				.flush(null, { status: 500, statusText: "Server Error" });

			// THEN
			await expect(result).rejects.toThrow();
		});

		it("should throw when POST succeeds but subsequent GET fails", async () => {
			// GIVEN
			const mockNewAsset = buildMockAsset({ id: undefined });

			// WHEN
			const result = lastValueFrom(testSubject.saveAsset(mockNewAsset));
			httpController
				.expectOne(req => req.method === "POST" && req.url.endsWith("/assets"))
				.flush(buildMockAsset());
			httpController
				.expectOne(req => req.method === "GET" && req.url.endsWith("/assets"))
				.flush(null, { status: 500, statusText: "Server Error" });

			// THEN
			await expect(result).rejects.toThrow();
		});
	});

	describe("saveCategory", () => {
		it("should make a POST request when the category has no id", async () => {
			// GIVEN
			const mockNewCategory = buildMockCategory({ id: undefined });
			const stubbedResult = { categories: [buildMockCategory()], assets: [] };

			// WHEN
			const result = lastValueFrom(testSubject.saveCategory(mockNewCategory));
			httpController
				.expectOne(req => req.method === "POST" && req.url.endsWith("/categories"))
				.flush(buildMockCategory());
			httpController
				.expectOne(req => req.method === "GET" && req.url.endsWith("/categories"))
				.flush(stubbedResult.categories);
			httpController
				.expectOne(req => req.method === "GET" && req.url.endsWith("/assets"))
				.flush(stubbedResult.assets);

			// THEN
			await result;
		});

		it("should make a PUT request when the category has an id", async () => {
			// GIVEN
			const mockExistingCategory = buildMockCategory();

			// WHEN
			const result = lastValueFrom(testSubject.saveCategory(mockExistingCategory));
			httpController
				.expectOne(
					req => req.method === "PUT" && req.url.endsWith(`/categories/${mockExistingCategory.id}`)
				)
				.flush(mockExistingCategory);
			httpController
				.expectOne(req => req.method === "GET" && req.url.endsWith("/categories"))
				.flush([mockExistingCategory]);
			httpController
				.expectOne(req => req.method === "GET" && req.url.endsWith("/assets"))
				.flush([]);

			// THEN
			await result;
		});

		it("should throw on error when creating a new category", async () => {
			// GIVEN
			const mockCategory = buildMockCategory({ id: undefined });

			// WHEN
			const result = lastValueFrom(testSubject.saveCategory(mockCategory));
			httpController
				.expectOne(req => req.method === "POST" && req.url.endsWith("/categories"))
				.flush(null, { status: 500, statusText: "Server Error" });

			// THEN
			await expect(result).rejects.toThrow();
		});

		it("should throw on error when updating an existing category", async () => {
			// GIVEN
			const mockCategory = buildMockCategory();

			// WHEN
			const result = lastValueFrom(testSubject.saveCategory(mockCategory));
			httpController
				.expectOne(
					req => req.method === "PUT" && req.url.endsWith(`/categories/${mockCategory.id}`)
				)
				.flush(null, { status: 500, statusText: "Server Error" });

			// THEN
			await expect(result).rejects.toThrow();
		});
	});

	describe("deleteCategory", () => {
		it("should make a DELETE request then refetch categories and assets", async () => {
			// GIVEN
			const stubbedCategoryId = faker.string.uuid();

			// WHEN
			const result = lastValueFrom(testSubject.deleteCategory(stubbedCategoryId));
			httpController
				.expectOne(
					req => req.method === "DELETE" && req.url.endsWith(`/categories/${stubbedCategoryId}`)
				)
				.flush([]);
			httpController
				.expectOne(req => req.method === "GET" && req.url.endsWith("/categories"))
				.flush([]);
			httpController
				.expectOne(req => req.method === "GET" && req.url.endsWith("/assets"))
				.flush([]);

			// THEN
			await result;
		});

		it("should throw on error", async () => {
			// GIVEN
			const stubbedCategoryId = faker.string.uuid();

			// WHEN
			const result = lastValueFrom(testSubject.deleteCategory(stubbedCategoryId));
			httpController
				.expectOne(
					req => req.method === "DELETE" && req.url.endsWith(`/categories/${stubbedCategoryId}`)
				)
				.flush(null, { status: 500, statusText: "Server Error" });

			// THEN
			await expect(result).rejects.toThrow();
		});
	});

	describe("deleteAssetPermanently", () => {
		it("should make a DELETE request then refetch assets", async () => {
			// GIVEN
			const stubbedAssetId = faker.string.uuid();
			const stubbedAssets = [buildMockAsset()];

			// WHEN
			const result = lastValueFrom(testSubject.deleteAssetPermanently(stubbedAssetId));
			httpController
				.expectOne(req => req.method === "DELETE" && req.url.endsWith(`/assets/${stubbedAssetId}`))
				.flush(null);
			httpController
				.expectOne(req => req.method === "GET" && req.url.endsWith("/assets"))
				.flush(stubbedAssets);

			// THEN
			const expectedAssets = await result;
			expect(expectedAssets).toEqual(stubbedAssets);
		});

		it("should throw on error", async () => {
			// GIVEN
			const stubbedAssetId = faker.string.uuid();

			// WHEN
			const result = lastValueFrom(testSubject.deleteAssetPermanently(stubbedAssetId));
			httpController
				.expectOne(req => req.method === "DELETE" && req.url.endsWith(`/assets/${stubbedAssetId}`))
				.flush(null, { status: 500, statusText: "Server Error" });

			// THEN
			await expect(result).rejects.toThrow();
		});
	});
});
