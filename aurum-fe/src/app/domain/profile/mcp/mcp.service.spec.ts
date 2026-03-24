import { provideHttpClient } from "@angular/common/http";
import { HttpTestingController, provideHttpClientTesting } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { faker } from "@faker-js/faker";
import { lastValueFrom } from "rxjs";
import { McpService } from "./mcp.service";
import { ApiKeyMeta, GeneratedKey } from "./model/mcp.model";

describe("McpService", () => {
	let testSubject: McpService;
	let httpController: HttpTestingController;

	beforeEach(() => {
		TestBed.configureTestingModule({
			providers: [McpService, provideHttpClient(), provideHttpClientTesting()]
		});
		testSubject = TestBed.inject(McpService);
		httpController = TestBed.inject(HttpTestingController);
	});

	afterEach(() => {
		httpController.verify();
	});

	describe("generateKey", () => {
		it("should return the generated key on success", async () => {
			// GIVEN
			const stubbedKey: GeneratedKey = { key: faker.string.uuid() };

			// WHEN
			const result = lastValueFrom(testSubject.generateKey());
			httpController
				.expectOne(req => req.method === "POST" && req.url.endsWith("/mcp/keys"))
				.flush(stubbedKey);

			// THEN
			const expectedKey = await result;
			expect(expectedKey).toEqual(stubbedKey);
		});

		it("should throw on error", async () => {
			// WHEN
			const result = lastValueFrom(testSubject.generateKey());
			httpController
				.expectOne(req => req.method === "POST" && req.url.endsWith("/mcp/keys"))
				.flush(null, { status: 500, statusText: "Server Error" });

			// THEN
			await expect(result).rejects.toThrow();
		});
	});

	describe("getKeyMeta", () => {
		it("should return the key metadata on success", async () => {
			// GIVEN
			const stubbedMeta: ApiKeyMeta = {
				id: faker.string.uuid(),
				name: faker.lorem.word(),
				createdAt: faker.date.past().toISOString(),
				lastUsedAt: null
			};

			// WHEN
			const result = lastValueFrom(testSubject.getKeyMeta());
			httpController
				.expectOne(req => req.method === "GET" && req.url.endsWith("/mcp/keys"))
				.flush(stubbedMeta);

			// THEN
			const expectedMeta = await result;
			expect(expectedMeta).toEqual(stubbedMeta);
		});

		it("should throw on error", async () => {
			// WHEN
			const result = lastValueFrom(testSubject.getKeyMeta());
			httpController
				.expectOne(req => req.method === "GET" && req.url.endsWith("/mcp/keys"))
				.flush(null, { status: 500, statusText: "Server Error" });

			// THEN
			await expect(result).rejects.toThrow();
		});
	});

	describe("revokeKey", () => {
		it("should make a DELETE request and succeed", async () => {
			// WHEN
			const result = lastValueFrom(testSubject.revokeKey());
			httpController
				.expectOne(req => req.method === "DELETE" && req.url.endsWith("/mcp/keys"))
				.flush(null);

			// THEN
			await result;
		});

		it("should throw on error", async () => {
			// WHEN
			const result = lastValueFrom(testSubject.revokeKey());
			httpController
				.expectOne(req => req.method === "DELETE" && req.url.endsWith("/mcp/keys"))
				.flush(null, { status: 500, statusText: "Server Error" });

			// THEN
			await expect(result).rejects.toThrow();
		});
	});
});
