import { provideHttpClient } from "@angular/common/http";
import { HttpTestingController, provideHttpClientTesting } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { faker } from "@faker-js/faker";
import { MockProvider } from "ng-mocks";
import { MessageService } from "primeng/api";
import { lastValueFrom } from "rxjs";
import { TargetService } from "./target.service";
import { Target, TargetStatus, TargetType } from "./model/target.model";

const buildMockTarget = (overrides: Partial<Target> = {}): Target => ({
	id: faker.string.uuid(),
	name: faker.lorem.words(3),
	targetAmount: faker.number.float({ min: 1000, max: 1000000 }),
	currentAmount: faker.number.float({ min: 0, max: 999999 }),
	completionPercentage: faker.number.float({ min: 0, max: 100 }),
	deadline: faker.date.future().toISOString().split("T")[0],
	isCompleted: false,
	type: faker.helpers.arrayElement(Object.values(TargetType)),
	status: faker.helpers.arrayElement(Object.values(TargetStatus)),
	...overrides
});

describe("TargetService", () => {
	let testSubject: TargetService;
	let httpController: HttpTestingController;

	beforeEach(() => {
		TestBed.configureTestingModule({
			providers: [
				TargetService,
				MockProvider(MessageService),
				provideHttpClient(),
				provideHttpClientTesting()
			]
		});
		testSubject = TestBed.inject(TargetService);
		httpController = TestBed.inject(HttpTestingController);
	});

	afterEach(() => {
		httpController.verify();
	});

	describe("getTargets", () => {
		it("should return targets on success", async () => {
			// GIVEN
			const stubbedTargets = [buildMockTarget()];

			// WHEN
			const result = lastValueFrom(testSubject.getTargets());
			httpController
				.expectOne(req => req.method === "GET" && req.url.endsWith("/targets"))
				.flush(stubbedTargets);

			// THEN
			const expectedTargets = await result;
			expect(expectedTargets).toEqual(stubbedTargets);
		});

		it("should throw on error", async () => {
			// WHEN
			const result = lastValueFrom(testSubject.getTargets());
			httpController
				.expectOne(req => req.method === "GET" && req.url.endsWith("/targets"))
				.flush(null, { status: 500, statusText: "Server Error" });

			// THEN
			await expect(result).rejects.toThrow();
		});
	});

	describe("saveTarget", () => {
		it("should make a POST request and return updated targets when the target has no id", async () => {
			// GIVEN
			const mockNewTarget = buildMockTarget({ id: undefined });
			const stubbedTargets = [buildMockTarget()];

			// WHEN
			const result = lastValueFrom(testSubject.saveTarget(mockNewTarget));
			httpController
				.expectOne(req => req.method === "POST" && req.url.endsWith("/targets"))
				.flush([buildMockTarget()]);
			httpController
				.expectOne(req => req.method === "GET" && req.url.endsWith("/targets"))
				.flush(stubbedTargets);

			// THEN
			const expectedTargets = await result;
			expect(expectedTargets).toEqual(stubbedTargets);
		});

		it("should make a PUT request and return updated targets when the target has an id", async () => {
			// GIVEN
			const mockExistingTarget = buildMockTarget();
			const stubbedTargets = [mockExistingTarget];

			// WHEN
			const result = lastValueFrom(testSubject.saveTarget(mockExistingTarget));
			httpController
				.expectOne(
					req => req.method === "PUT" && req.url.endsWith(`/targets/${mockExistingTarget.id}`)
				)
				.flush([mockExistingTarget]);
			httpController
				.expectOne(req => req.method === "GET" && req.url.endsWith("/targets"))
				.flush(stubbedTargets);

			// THEN
			const expectedTargets = await result;
			expect(expectedTargets).toEqual(stubbedTargets);
		});

		it("should throw on error when the target has no id", async () => {
			// GIVEN
			const mockTarget = buildMockTarget({ id: undefined });

			// WHEN
			const result = lastValueFrom(testSubject.saveTarget(mockTarget));
			httpController
				.expectOne(req => req.method === "POST" && req.url.endsWith("/targets"))
				.flush(null, { status: 500, statusText: "Server Error" });

			// THEN
			await expect(result).rejects.toThrow();
		});

		it("should throw on error when the target has an id", async () => {
			// GIVEN
			const mockTarget = buildMockTarget();

			// WHEN
			const result = lastValueFrom(testSubject.saveTarget(mockTarget));
			httpController
				.expectOne(req => req.method === "PUT" && req.url.endsWith(`/targets/${mockTarget.id}`))
				.flush(null, { status: 500, statusText: "Server Error" });

			// THEN
			await expect(result).rejects.toThrow();
		});
	});

	describe("deleteTarget", () => {
		it("should make a DELETE request then refetch targets", async () => {
			// GIVEN
			const stubbedTargetId = faker.string.uuid();
			const stubbedTargets = [buildMockTarget()];

			// WHEN
			const result = lastValueFrom(testSubject.deleteTarget(stubbedTargetId));
			httpController
				.expectOne(
					req => req.method === "DELETE" && req.url.endsWith(`/targets/${stubbedTargetId}`)
				)
				.flush([]);
			httpController
				.expectOne(req => req.method === "GET" && req.url.endsWith("/targets"))
				.flush(stubbedTargets);

			// THEN
			const expectedTargets = await result;
			expect(expectedTargets).toEqual(stubbedTargets);
		});

		it("should throw on error", async () => {
			// GIVEN
			const stubbedTargetId = faker.string.uuid();

			// WHEN
			const result = lastValueFrom(testSubject.deleteTarget(stubbedTargetId));
			httpController
				.expectOne(
					req => req.method === "DELETE" && req.url.endsWith(`/targets/${stubbedTargetId}`)
				)
				.flush(null, { status: 500, statusText: "Server Error" });

			// THEN
			await expect(result).rejects.toThrow();
		});
	});
});
