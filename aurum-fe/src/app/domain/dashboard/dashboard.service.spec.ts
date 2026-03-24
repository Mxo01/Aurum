import { provideHttpClient } from "@angular/common/http";
import { HttpTestingController, provideHttpClientTesting } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { faker } from "@faker-js/faker";
import { lastValueFrom } from "rxjs";
import { DashboardService } from "./dashboard.service";

describe("DashboardService", () => {
	let testSubject: DashboardService;
	let httpController: HttpTestingController;

	beforeEach(() => {
		TestBed.configureTestingModule({
			providers: [DashboardService, provideHttpClient(), provideHttpClientTesting()]
		});
		testSubject = TestBed.inject(DashboardService);
		httpController = TestBed.inject(HttpTestingController);
	});

	afterEach(() => {
		httpController.verify();
	});

	describe("getSummary", () => {
		it("should return the analytics summary on success", async () => {
			// GIVEN
			const stubbedSummary = { totalNetWorth: faker.number.float() };

			// WHEN
			const result = lastValueFrom(testSubject.getSummary());
			httpController
				.expectOne(req => req.method === "GET" && req.url.endsWith("/analytics/summary"))
				.flush(stubbedSummary);

			// THEN
			const expectedSummary = await result;
			expect(expectedSummary).toEqual(stubbedSummary);
		});

		it("should throw on error", async () => {
			// WHEN
			const result = lastValueFrom(testSubject.getSummary());
			httpController
				.expectOne(req => req.method === "GET" && req.url.endsWith("/analytics/summary"))
				.flush(null, { status: 500, statusText: "Server Error" });

			// THEN
			await expect(result).rejects.toThrow();
		});
	});

	describe("getChartData", () => {
		it("should make a GET request without query params when allHistory is false", async () => {
			// GIVEN
			const stubbedChartData = { labels: ["Jan"] };

			// WHEN
			const result = lastValueFrom(testSubject.getChartData());
			httpController
				.expectOne(req => req.method === "GET" && req.urlWithParams.endsWith("/analytics/chart"))
				.flush(stubbedChartData);

			// THEN
			const expectedChartData = await result;
			expect(expectedChartData).toEqual(stubbedChartData);
		});

		it("should make a GET request with allHistory=true when requested", async () => {
			// WHEN
			const result = lastValueFrom(testSubject.getChartData(true));
			httpController
				.expectOne(
					req =>
						req.method === "GET" &&
						req.urlWithParams.includes("/analytics/chart") &&
						req.urlWithParams.includes("allHistory=true")
				)
				.flush({});

			// THEN
			await result;
		});

		it("should throw on error", async () => {
			// WHEN
			const result = lastValueFrom(testSubject.getChartData());
			httpController
				.expectOne(req => req.method === "GET" && req.urlWithParams.endsWith("/analytics/chart"))
				.flush(null, { status: 500, statusText: "Server Error" });

			// THEN
			await expect(result).rejects.toThrow();
		});
	});

	describe("getChartDataForYear", () => {
		it("should make a GET request with the given year as a query parameter", async () => {
			// GIVEN
			const stubbedYear = faker.number.int({ min: 2020, max: 2030 });
			const stubbedChartData = { labels: ["Jan"] };

			// WHEN
			const result = lastValueFrom(testSubject.getChartDataForYear(stubbedYear));
			httpController
				.expectOne(
					req =>
						req.method === "GET" &&
						req.urlWithParams.includes("/analytics/chart") &&
						req.urlWithParams.includes(`year=${stubbedYear}`)
				)
				.flush(stubbedChartData);

			// THEN
			const expectedChartData = await result;
			expect(expectedChartData).toEqual(stubbedChartData);
		});

		it("should throw on error", async () => {
			// WHEN
			const result = lastValueFrom(testSubject.getChartDataForYear(2023));
			httpController
				.expectOne(req => req.method === "GET" && req.urlWithParams.includes("/analytics/chart"))
				.flush(null, { status: 500, statusText: "Server Error" });

			// THEN
			await expect(result).rejects.toThrow();
		});
	});

	describe("getProjections", () => {
		it("should make a GET request with correct params and return projections", async () => {
			// GIVEN
			const stubbedProjections = { 1: 15000 };

			// WHEN
			const result = lastValueFrom(testSubject.getProjections());
			httpController
				.expectOne(
					req =>
						req.method === "GET" &&
						req.urlWithParams.includes("/analytics/projections") &&
						req.urlWithParams.includes("years=1") &&
						req.urlWithParams.includes("assetsOnly=true")
				)
				.flush(stubbedProjections);

			// THEN
			const expectedProjections = await result;
			expect(expectedProjections).toEqual(stubbedProjections);
		});

		it("should throw on error", async () => {
			// WHEN
			const result = lastValueFrom(testSubject.getProjections());
			httpController
				.expectOne(
					req => req.method === "GET" && req.urlWithParams.includes("/analytics/projections")
				)
				.flush(null, { status: 500, statusText: "Server Error" });

			// THEN
			await expect(result).rejects.toThrow();
		});
	});
});
