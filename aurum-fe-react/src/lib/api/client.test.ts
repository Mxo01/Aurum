import { afterEach, describe, expect, it, vi } from "vitest";
import {
	ApiError,
	createApiClient,
	isApiError,
	registerUnauthorizedHandler,
	unwrap
} from "./client";

function jsonResponse(body: unknown, status = 200) {
	return new Response(JSON.stringify(body), {
		status,
		headers: { "content-type": "application/json" }
	});
}

describe("api client", () => {
	afterEach(() => {
		vi.restoreAllMocks();
		registerUnauthorizedHandler(() => undefined);
	});

	describe("unwrap", () => {
		it("should return the data of a successful response", () => {
			// GIVEN
			const testSubject = unwrap;
			const stubbedData = { id: "1" };

			// WHEN
			const expected = testSubject({ data: stubbedData, response: jsonResponse(stubbedData) });

			// THEN
			expect(expected).toEqual(stubbedData);
		});

		it("should throw an ApiError with the response status on failure", () => {
			// GIVEN
			const testSubject = unwrap;

			// WHEN
			const call = () =>
				testSubject({ error: { message: "boom" }, response: jsonResponse({}, 500) });

			// THEN
			expect(call).toThrow(ApiError);
			expect(call).toThrow("500");
		});
	});

	describe("isApiError", () => {
		it("should narrow an ApiError", () => {
			// GIVEN
			const testSubject = isApiError;
			const error = new ApiError(404, null);

			// WHEN
			const expected = testSubject(error);

			// THEN
			expect(expected).toBe(true);
		});

		it("should reject other errors", () => {
			// GIVEN
			const testSubject = isApiError;

			// WHEN
			const expected = testSubject(new Error("nope"));

			// THEN
			expect(expected).toBe(false);
		});
	});

	describe("createApiClient", () => {
		it("should attach the bearer token to the request", async () => {
			// GIVEN
			const fetchSpy = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse({ id: "1" }));
			const testSubject = createApiClient(async () => "token-123");

			// WHEN
			await testSubject.GET("/users");

			// THEN
			const request = fetchSpy.mock.calls[0]?.[0] as Request;
			expect(request.headers.get("Authorization")).toBe("Bearer token-123");
		});

		it("should send no authorization header without a token", async () => {
			// GIVEN
			const fetchSpy = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse({ id: "1" }));
			const testSubject = createApiClient(async () => undefined);

			// WHEN
			await testSubject.GET("/users");

			// THEN
			const request = fetchSpy.mock.calls[0]?.[0] as Request;
			expect(request.headers.get("Authorization")).toBeNull();
		});

		it("should keep the request going when the token provider fails", async () => {
			// GIVEN
			const fetchSpy = vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse({ id: "1" }));
			const testSubject = createApiClient(async () => {
				throw new Error("no session");
			});

			// WHEN
			await testSubject.GET("/users");

			// THEN
			const request = fetchSpy.mock.calls[0]?.[0] as Request;
			expect(request.headers.get("Authorization")).toBeNull();
		});

		it("should call the unauthorized handler on a 401", async () => {
			// GIVEN
			vi.spyOn(globalThis, "fetch").mockResolvedValue(jsonResponse({}, 401));
			const onUnauthorized = vi.fn();
			registerUnauthorizedHandler(onUnauthorized);
			const testSubject = createApiClient(async () => "token");

			// WHEN
			await testSubject.GET("/users");

			// THEN
			expect(onUnauthorized).toHaveBeenCalledOnce();
		});
	});
});
