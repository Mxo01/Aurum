import { renderHook, waitFor } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { describe, expect, it, vi } from "vitest";
import { ApiError } from "@/lib/api/client";
import { queryKeys } from "@/lib/api/keys";
import { env } from "@/lib/env";
import { server } from "@/test/msw/server";
import { createTestQueryClient, TestProviders } from "@/test/render";
import { useAssets, useCreateAsset } from "./api";

describe("useAssets", () => {
	it("should expose the assets on success", async () => {
		// GIVEN
		const testSubject = () => useAssets();

		// WHEN
		const { result } = renderHook(testSubject, { wrapper: TestProviders });

		// THEN
		await waitFor(() => expect(result.current.isSuccess).toBe(true));
		expect(result.current.data?.length).toBeGreaterThan(0);
	});

	it("should expose an ApiError on failure", async () => {
		// GIVEN
		server.use(
			http.get(`${env.apiUrl}/assets`, () =>
				HttpResponse.json({ message: "boom" }, { status: 500 })
			)
		);
		const testSubject = () => useAssets();

		// WHEN
		const { result } = renderHook(testSubject, { wrapper: TestProviders });

		// THEN
		await waitFor(() => expect(result.current.isError).toBe(true));
		expect(result.current.error).toBeInstanceOf(ApiError);
	});
});

describe("useCreateAsset", () => {
	it("should invalidate the assets and analytics queries on success", async () => {
		// GIVEN
		const queryClient = createTestQueryClient();
		const invalidateSpy = vi.spyOn(queryClient, "invalidateQueries");
		const testSubject = () => useCreateAsset();
		const { result } = renderHook(testSubject, {
			wrapper: ({ children }) => <TestProviders queryClient={queryClient}>{children}</TestProviders>
		});

		// WHEN
		result.current.mutate({ name: "Apartment" });

		// THEN
		await waitFor(() => expect(result.current.isSuccess).toBe(true));
		expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: queryKeys.assets });
		expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ["analytics"] });
	});
});
