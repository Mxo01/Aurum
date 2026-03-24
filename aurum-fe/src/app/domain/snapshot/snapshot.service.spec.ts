import { provideHttpClient } from "@angular/common/http";
import { HttpTestingController, provideHttpClientTesting } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { faker } from "@faker-js/faker";
import { MockProvider } from "ng-mocks";
import { MessageService } from "primeng/api";
import { lastValueFrom } from "rxjs";
import { SnapshotService } from "./snapshot.service";
import { Snapshot } from "./model/snapshot.model";

const buildMockSnapshot = (overrides: Partial<Snapshot> = {}): Snapshot => ({
	id: faker.string.uuid(),
	assetId: faker.string.uuid(),
	assetName: faker.lorem.word(),
	referenceDate: faker.date.recent().toISOString().split("T")[0],
	amountOriginalCurrency: faker.number.float({ min: 1, max: 100000 }),
	exchangeRateToBase: faker.number.float({ min: 0.1, max: 10 }),
	amountBaseCurrency: faker.number.float({ min: 1, max: 100000 }),
	...overrides
});

describe("SnapshotService", () => {
	let testSubject: SnapshotService;
	let httpController: HttpTestingController;

	beforeEach(() => {
		TestBed.configureTestingModule({
			providers: [
				SnapshotService,
				MockProvider(MessageService),
				provideHttpClient(),
				provideHttpClientTesting()
			]
		});
		testSubject = TestBed.inject(SnapshotService);
		httpController = TestBed.inject(HttpTestingController);
	});

	afterEach(() => {
		httpController.verify();
	});

	describe("getSnapshotsByAssetId", () => {
		it("should return snapshots on success", async () => {
			// GIVEN
			const stubbedAssetId = faker.string.uuid();
			const stubbedSnapshots = [buildMockSnapshot({ assetId: stubbedAssetId })];

			// WHEN
			const result = lastValueFrom(testSubject.getSnapshotsByAssetId(stubbedAssetId));
			httpController
				.expectOne(
					req =>
						req.method === "GET" &&
						req.urlWithParams.includes("/snapshots") &&
						req.urlWithParams.includes(`assetId=${stubbedAssetId}`)
				)
				.flush(stubbedSnapshots);

			// THEN
			const expectedSnapshots = await result;
			expect(expectedSnapshots).toEqual(stubbedSnapshots);
		});

		it("should throw on error", async () => {
			// GIVEN
			const stubbedAssetId = faker.string.uuid();

			// WHEN
			const result = lastValueFrom(testSubject.getSnapshotsByAssetId(stubbedAssetId));
			httpController
				.expectOne(req => req.method === "GET" && req.urlWithParams.includes("/snapshots"))
				.flush(null, { status: 500, statusText: "Server Error" });

			// THEN
			await expect(result).rejects.toThrow();
		});
	});

	describe("saveSnapshot", () => {
		it("should make a POST request then refetch snapshots for the same asset", async () => {
			// GIVEN
			const stubbedSnapshot = buildMockSnapshot();
			const stubbedSnapshots = [stubbedSnapshot];

			// WHEN
			const result = lastValueFrom(testSubject.saveSnapshot(stubbedSnapshot));
			httpController
				.expectOne(req => req.method === "POST" && req.url.endsWith("/snapshots"))
				.flush(buildMockSnapshot());
			httpController
				.expectOne(
					req =>
						req.method === "GET" &&
						req.urlWithParams.includes("/snapshots") &&
						req.urlWithParams.includes(`assetId=${stubbedSnapshot.assetId}`)
				)
				.flush(stubbedSnapshots);

			// THEN
			const expectedSnapshots = await result;
			expect(expectedSnapshots).toEqual(stubbedSnapshots);
		});

		it("should throw on error", async () => {
			// GIVEN
			const stubbedSnapshot = buildMockSnapshot();

			// WHEN
			const result = lastValueFrom(testSubject.saveSnapshot(stubbedSnapshot));
			httpController
				.expectOne(req => req.method === "POST" && req.url.endsWith("/snapshots"))
				.flush(null, { status: 500, statusText: "Server Error" });

			// THEN
			await expect(result).rejects.toThrow();
		});
	});

	describe("deleteSnapshotsBulk", () => {
		it("should make a DELETE request then refetch snapshots for the asset", async () => {
			// GIVEN
			const stubbedAssetId = faker.string.uuid();
			const stubbedIds = [faker.string.uuid(), faker.string.uuid()];
			const stubbedSnapshots = [buildMockSnapshot({ assetId: stubbedAssetId })];

			// WHEN
			const result = lastValueFrom(testSubject.deleteSnapshotsBulk(stubbedAssetId, stubbedIds));
			httpController
				.expectOne(
					req =>
						req.method === "DELETE" &&
						req.urlWithParams.includes("/snapshots/bulk") &&
						req.urlWithParams.includes(`assetId=${stubbedAssetId}`)
				)
				.flush(null);
			httpController
				.expectOne(
					req =>
						req.method === "GET" &&
						req.urlWithParams.includes("/snapshots") &&
						req.urlWithParams.includes(`assetId=${stubbedAssetId}`)
				)
				.flush(stubbedSnapshots);

			// THEN
			const expectedSnapshots = await result;
			expect(expectedSnapshots).toEqual(stubbedSnapshots);
		});

		it("should throw on error", async () => {
			// GIVEN
			const stubbedAssetId = faker.string.uuid();
			const stubbedIds = [faker.string.uuid()];

			// WHEN
			const result = lastValueFrom(testSubject.deleteSnapshotsBulk(stubbedAssetId, stubbedIds));
			httpController
				.expectOne(req => req.method === "DELETE" && req.urlWithParams.includes("/snapshots/bulk"))
				.flush(null, { status: 500, statusText: "Server Error" });

			// THEN
			await expect(result).rejects.toThrow();
		});
	});
});
