import { QueryClient } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { http, HttpResponse } from "msw";
import { toast } from "sonner";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { queryKeys } from "@/lib/api/keys";
import { env } from "@/lib/env";
import { buildCategory } from "@/test/fixtures/portfolio";
import { server } from "@/test/msw/server";
import { TestProviders } from "@/test/render";
import { useCategories, useCreateCategory, useDeleteCategory, useUpdateCategory } from "./api";

const mockCategory = buildCategory({ id: "c1", name: "Investments" });

function setup() {
	const queryClient = new QueryClient({
		defaultOptions: {
			queries: { retry: false, gcTime: Infinity },
			mutations: { retry: false }
		}
	});
	const wrapper = ({ children }: { children: ReactNode }) => (
		<TestProviders queryClient={queryClient}>{children}</TestProviders>
	);
	return { queryClient, wrapper };
}

describe("useCategories", () => {
	beforeEach(() => {
		vi.restoreAllMocks();
	});

	it("should expose the categories", async () => {
		// GIVEN
		server.use(http.get(`${env.apiUrl}/categories`, () => HttpResponse.json([mockCategory])));
		const testSubject = () => useCategories();

		// WHEN
		const { result } = renderHook(testSubject, { wrapper: TestProviders });

		// THEN
		await waitFor(() => expect(result.current.isSuccess).toBe(true));
		expect(result.current.data).toEqual([mockCategory]);
	});
});

describe("category mutations", () => {
	beforeEach(() => {
		vi.restoreAllMocks();
	});

	it("should invalidate the categories on create", async () => {
		// GIVEN
		const { queryClient, wrapper } = setup();
		const invalidateSpy = vi.spyOn(queryClient, "invalidateQueries");
		const testSubject = () => useCreateCategory();
		const { result } = renderHook(testSubject, { wrapper });

		// WHEN
		result.current.mutate({ name: "Cash", type: "ASSET" });

		// THEN
		await waitFor(() => expect(result.current.isSuccess).toBe(true));
		expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: queryKeys.categories });
	});

	it("should report a failed create", async () => {
		// GIVEN
		const errorSpy = vi.spyOn(toast, "error");
		server.use(http.post(`${env.apiUrl}/categories`, () => HttpResponse.json({}, { status: 500 })));
		const { wrapper } = setup();
		const testSubject = () => useCreateCategory();
		const { result } = renderHook(testSubject, { wrapper });

		// WHEN
		result.current.mutate({ name: "Cash", type: "ASSET" });

		// THEN
		await waitFor(() => expect(result.current.isError).toBe(true));
		expect(errorSpy).toHaveBeenCalledWith("Could not create the category");
	});

	it("should update a category", async () => {
		// GIVEN
		const { queryClient, wrapper } = setup();
		const invalidateSpy = vi.spyOn(queryClient, "invalidateQueries");
		const testSubject = () => useUpdateCategory();
		const { result } = renderHook(testSubject, { wrapper });

		// WHEN
		result.current.mutate({ id: "c1", body: { name: "Renamed" } });

		// THEN
		await waitFor(() => expect(result.current.isSuccess).toBe(true));
		expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: queryKeys.categories });
	});

	it("should report a failed update", async () => {
		// GIVEN
		const errorSpy = vi.spyOn(toast, "error");
		server.use(
			http.put(`${env.apiUrl}/categories/:id`, () => HttpResponse.json({}, { status: 500 }))
		);
		const { wrapper } = setup();
		const testSubject = () => useUpdateCategory();
		const { result } = renderHook(testSubject, { wrapper });

		// WHEN
		result.current.mutate({ id: "c1", body: { name: "Renamed" } });

		// THEN
		await waitFor(() => expect(result.current.isError).toBe(true));
		expect(errorSpy).toHaveBeenCalledWith("Could not update the category");
	});

	it("should delete a category", async () => {
		// GIVEN
		const { queryClient, wrapper } = setup();
		const invalidateSpy = vi.spyOn(queryClient, "invalidateQueries");
		const testSubject = () => useDeleteCategory();
		const { result } = renderHook(testSubject, { wrapper });

		// WHEN
		result.current.mutate("c1");

		// THEN
		await waitFor(() => expect(result.current.isSuccess).toBe(true));
		expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: queryKeys.categories });
	});

	it("should report a failed delete", async () => {
		// GIVEN
		const errorSpy = vi.spyOn(toast, "error");
		server.use(
			http.delete(`${env.apiUrl}/categories/:id`, () => HttpResponse.json({}, { status: 500 }))
		);
		const { wrapper } = setup();
		const testSubject = () => useDeleteCategory();
		const { result } = renderHook(testSubject, { wrapper });

		// WHEN
		result.current.mutate("c1");

		// THEN
		await waitFor(() => expect(result.current.isError).toBe(true));
		expect(errorSpy).toHaveBeenCalledWith("Could not delete the category");
	});
});
