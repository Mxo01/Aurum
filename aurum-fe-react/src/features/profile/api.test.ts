import { renderHook, waitFor } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { ApiError } from "@/lib/api/client";
import { env } from "@/lib/env";
import { server } from "@/test/msw/server";
import { TestProviders } from "@/test/render";
import { useProfile } from "./api";

describe("useProfile", () => {
	it("should expose the profile on success", async () => {
		// GIVEN
		const testSubject = () => useProfile();

		// WHEN
		const { result } = renderHook(testSubject, { wrapper: TestProviders });

		// THEN
		await waitFor(() => expect(result.current.isSuccess).toBe(true));
		expect(result.current.data?.id).toBeTruthy();
	});

	it("should expose an ApiError on failure", async () => {
		// GIVEN
		server.use(
			http.get(`${env.apiUrl}/users`, () => HttpResponse.json({ message: "boom" }, { status: 500 }))
		);
		const testSubject = () => useProfile();

		// WHEN
		const { result } = renderHook(testSubject, { wrapper: TestProviders });

		// THEN
		await waitFor(() => expect(result.current.isError).toBe(true));
		expect(result.current.error).toBeInstanceOf(ApiError);
	});
});
