import { QueryClient } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { http, HttpResponse } from "msw";
import { toast } from "sonner";
import { afterEach, describe, expect, it, vi } from "vitest";
import { queryKeys } from "@/lib/api/keys";
import type { ApiErrorBody, ApiErrorDetail } from "@/lib/api/types";
import { env } from "@/lib/env";
import { buildCategory } from "@/test/fixtures/portfolio";
import { server } from "@/test/msw/server";
import { TestProviders } from "@/test/render";
import { useCategories, useCreateCategory, useDeleteCategory, useUpdateCategory } from "./api";

const mockCategory = buildCategory({ id: "c1", name: "Investments" });
const CATEGORY_ID = "c1";

const REJECTION_STATUS = 400;
const REJECTION_ERROR = "Bad Request";
const SERVER_MESSAGE = "A category named Cash already exists";
const SILENT_FAILURE_STATUS = 500;

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

function buildEnvelope(detail: ApiErrorDetail): ApiErrorBody {
	return { status: REJECTION_STATUS, error: REJECTION_ERROR, errors: [detail] };
}

interface CategoryMutationProbe {
	readonly mutate: () => void;
	readonly isError: boolean;
}

interface CategoryMutationCase {
	readonly label: string;
	readonly fallback: string;
	readonly reject: (status: number, body: ApiErrorBody) => void;
	readonly probe: () => CategoryMutationProbe;
}

function useCreateCategoryProbe(): CategoryMutationProbe {
	const mutation = useCreateCategory();
	return {
		mutate: () => mutation.mutate({ name: "Cash", type: "ASSET" }),
		isError: mutation.isError
	};
}

function useUpdateCategoryProbe(): CategoryMutationProbe {
	const mutation = useUpdateCategory();
	return {
		mutate: () => mutation.mutate({ id: CATEGORY_ID, body: { name: "Renamed" } }),
		isError: mutation.isError
	};
}

function useDeleteCategoryProbe(): CategoryMutationProbe {
	const mutation = useDeleteCategory();
	return {
		mutate: () => mutation.mutate(CATEGORY_ID),
		isError: mutation.isError
	};
}

const CATEGORY_MUTATION_CASES: readonly CategoryMutationCase[] = [
	{
		label: "create",
		fallback: "Could not create the category",
		reject: (status, body) =>
			server.use(http.post(`${env.apiUrl}/categories`, () => HttpResponse.json(body, { status }))),
		probe: useCreateCategoryProbe
	},
	{
		label: "update",
		fallback: "Could not update the category",
		reject: (status, body) =>
			server.use(
				http.put(`${env.apiUrl}/categories/:id`, () => HttpResponse.json(body, { status }))
			),
		probe: useUpdateCategoryProbe
	},
	{
		label: "delete",
		fallback: "Could not delete the category",
		reject: (status, body) =>
			server.use(
				http.delete(`${env.apiUrl}/categories/:id`, () => HttpResponse.json(body, { status }))
			),
		probe: useDeleteCategoryProbe
	}
];

describe("useCategories", () => {
	afterEach(() => vi.restoreAllMocks());

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
	afterEach(() => vi.restoreAllMocks());

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
});

describe("category mutation failures", () => {
	afterEach(() => vi.restoreAllMocks());

	it.each(CATEGORY_MUTATION_CASES)(
		"should show the server sentence on a failed $label",
		async ({ reject, probe }) => {
			// GIVEN
			const errorSpy = vi.spyOn(toast, "error");
			reject(REJECTION_STATUS, buildEnvelope({ field: "name", message: SERVER_MESSAGE }));
			const { wrapper } = setup();
			const testSubject = probe;
			const { result } = renderHook(testSubject, { wrapper });

			// WHEN
			result.current.mutate();

			// THEN
			await waitFor(() => expect(result.current.isError).toBe(true));
			expect(errorSpy).toHaveBeenCalledWith(SERVER_MESSAGE);
		}
	);

	it.each(CATEGORY_MUTATION_CASES)(
		"should fall back to its own text on a $label without an envelope",
		async ({ reject, probe, fallback }) => {
			// GIVEN
			const errorSpy = vi.spyOn(toast, "error");
			reject(SILENT_FAILURE_STATUS, {});
			const { wrapper } = setup();
			const testSubject = probe;
			const { result } = renderHook(testSubject, { wrapper });

			// WHEN
			result.current.mutate();

			// THEN
			await waitFor(() => expect(result.current.isError).toBe(true));
			expect(errorSpy).toHaveBeenCalledWith(fallback);
		}
	);
});
