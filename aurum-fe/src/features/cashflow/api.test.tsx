import { QueryClient } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { http, HttpResponse } from "msw";
import { toast } from "sonner";
import { describe, expect, it, vi } from "vitest";
import { queryKeys } from "@/lib/api/keys";
import { env } from "@/lib/env";
import { buildCashflowYear } from "@/test/fixtures/portfolio";
import { server } from "@/test/msw/server";
import { TestProviders } from "@/test/render";
import { useCashflowYear, useUpdateCashflowEntry } from "./api";

const mockYear = buildCashflowYear({ year: 2026 });

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

describe("useCashflowYear", () => {
	it("should expose the year data", async () => {
		// GIVEN
		server.use(http.get(`${env.apiUrl}/cashflows/:year`, () => HttpResponse.json(mockYear)));
		const testSubject = () => useCashflowYear(2026);

		// WHEN
		const { result } = renderHook(testSubject, { wrapper: TestProviders });

		// THEN
		await waitFor(() => expect(result.current.isSuccess).toBe(true));
		expect(result.current.data?.year).toBe(2026);
	});
});

describe("useUpdateCashflowEntry", () => {
	it("should update the entry optimistically", async () => {
		// GIVEN
		const { queryClient, wrapper } = setup();
		queryClient.setQueryData(queryKeys.cashflow(2026), mockYear);
		server.use(
			http.put(`${env.apiUrl}/cashflows/:year/:month`, () => new Promise(() => undefined))
		);
		const testSubject = () => useUpdateCashflowEntry();
		const { result } = renderHook(testSubject, { wrapper });

		// WHEN
		result.current.mutate({ year: 2026, month: 1, entry: { earned: 1, spent: 2 } });

		// THEN
		await waitFor(() => {
			const data = queryClient.getQueryData<typeof mockYear>(queryKeys.cashflow(2026));
			expect(data?.entries[0]?.earned).toBe(1);
		});
	});

	it("should restore the entry and report a failure", async () => {
		// GIVEN
		const { queryClient, wrapper } = setup();
		queryClient.setQueryData(queryKeys.cashflow(2026), mockYear);
		server.use(
			http.put(`${env.apiUrl}/cashflows/:year/:month`, () => HttpResponse.json({}, { status: 500 }))
		);
		const errorSpy = vi.spyOn(toast, "error");
		const testSubject = () => useUpdateCashflowEntry();
		const { result } = renderHook(testSubject, { wrapper });

		// WHEN
		result.current.mutate({ year: 2026, month: 1, entry: { earned: 1, spent: 2 } });

		// THEN
		await waitFor(() => expect(result.current.isError).toBe(true));
		expect(errorSpy).toHaveBeenCalledWith("Could not save the entry");
		expect(queryClient.getQueryData(queryKeys.cashflow(2026))).toEqual(mockYear);
	});
});

it("should skip the optimistic update without a cache", async () => {
	// GIVEN
	const { queryClient, wrapper } = setup();
	server.use(http.put(`${env.apiUrl}/cashflows/:year/:month`, () => new Promise(() => undefined)));
	const testSubject = () => useUpdateCashflowEntry();
	const { result } = renderHook(testSubject, { wrapper });

	// WHEN
	result.current.mutate({ year: 2026, month: 1, entry: { earned: 1, spent: 2 } });

	// THEN
	await waitFor(() => expect(queryClient.getQueryData(queryKeys.cashflow(2026))).toBeUndefined());
});

it("should report a failure without a cached year", async () => {
	// GIVEN
	const { wrapper } = setup();
	server.use(
		http.put(`${env.apiUrl}/cashflows/:year/:month`, () => HttpResponse.json({}, { status: 500 }))
	);
	const testSubject = () => useUpdateCashflowEntry();
	const { result } = renderHook(testSubject, { wrapper });

	// WHEN
	result.current.mutate({ year: 2026, month: 1, entry: { earned: 1, spent: 2 } });

	// THEN
	await waitFor(() => expect(result.current.isError).toBe(true));
});
