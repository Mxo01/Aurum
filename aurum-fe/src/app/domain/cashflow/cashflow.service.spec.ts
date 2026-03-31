import { provideHttpClient } from "@angular/common/http";
import { HttpTestingController, provideHttpClientTesting } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { faker } from "@faker-js/faker";
import { lastValueFrom } from "rxjs";
import { CashFlowService } from "./cashflow.service";
import { CashFlowEntry, CashFlowYear } from "./model/cashflow.model";

const buildMockEntry = (month = 1): CashFlowEntry => ({
	id: faker.string.uuid(),
	month,
	earned: faker.number.float({ min: 0, max: 5000, fractionDigits: 2 }),
	spent: faker.number.float({ min: 0, max: 3000, fractionDigits: 2 })
});

const buildMockYear = (year = 2026): CashFlowYear => ({
	year,
	entries: Array.from({ length: 12 }, (_, i) => buildMockEntry(i + 1)),
	previousYearEntries: null,
	availableYears: [year]
});

describe("CashFlowService", () => {
	let testSubject: CashFlowService;
	let httpController: HttpTestingController;

	beforeEach(() => {
		TestBed.configureTestingModule({
			providers: [CashFlowService, provideHttpClient(), provideHttpClientTesting()]
		});
		testSubject = TestBed.inject(CashFlowService);
		httpController = TestBed.inject(HttpTestingController);
	});

	afterEach(() => httpController.verify());

	describe("getYear", () => {
		it("should return year data on success", async () => {
			// GIVEN
			const stubbedYear = buildMockYear(2026);

			// WHEN
			const result = lastValueFrom(testSubject.getYear(2026));
			httpController
				.expectOne(req => req.method === "GET" && req.url.endsWith("/cashflows/2026"))
				.flush(stubbedYear);

			// THEN
			const expectedYear = await result;
			expect(expectedYear).toEqual(stubbedYear);
		});

		it("should throw on error", async () => {
			// WHEN
			const result = lastValueFrom(testSubject.getYear(2026));
			httpController
				.expectOne(req => req.method === "GET" && req.url.endsWith("/cashflows/2026"))
				.flush(null, { status: 500, statusText: "Server Error" });

			// THEN
			await expect(result).rejects.toThrow();
		});
	});

	describe("updateEntry", () => {
		it("should return updated entry on success", async () => {
			// GIVEN
			const stubbedEntry = buildMockEntry(3);
			const dto = { earned: stubbedEntry.earned, spent: stubbedEntry.spent };

			// WHEN
			const result = lastValueFrom(testSubject.updateEntry(2026, 3, dto));
			httpController
				.expectOne(req => req.method === "PUT" && req.url.endsWith("/cashflows/2026/3"))
				.flush(stubbedEntry);

			// THEN
			const expectedEntry = await result;
			expect(expectedEntry).toEqual(stubbedEntry);
		});

		it("should throw on error", async () => {
			// WHEN
			const result = lastValueFrom(testSubject.updateEntry(2026, 3, { earned: 100, spent: 50 }));
			httpController
				.expectOne(req => req.method === "PUT" && req.url.endsWith("/cashflows/2026/3"))
				.flush(null, { status: 500, statusText: "Server Error" });

			// THEN
			await expect(result).rejects.toThrow();
		});
	});
});
