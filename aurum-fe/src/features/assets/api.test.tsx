import { renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { toast } from "sonner";
import type { AssetDTO } from "@/lib/api/types";
import { http, HttpResponse } from "msw";
import { describe, expect, it, vi } from "vitest";
import { ApiError } from "@/lib/api/client";
import { buildAsset } from "@/test/fixtures/portfolio";
import { queryKeys } from "@/lib/api/keys";
import { env } from "@/lib/env";
import { server } from "@/test/msw/server";
import { QueryClient } from "@tanstack/react-query";
import { createTestQueryClient, TestProviders } from "@/test/render";
import {
	useAssets,
	useCreateAsset,
	useDeleteAsset,
	useRenameAsset,
	useToggleFavorite,
	useUpdateAsset,
	useUpdateCurrentValue
} from "./api";

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

describe("asset mutations", () => {
	const mockAsset = buildAsset({ id: "a1", name: "Apartment", isFavorite: false });

	function setup() {
		const queryClient = new QueryClient({
			defaultOptions: {
				queries: { retry: false, gcTime: Infinity },
				mutations: { retry: false }
			}
		});
		queryClient.setQueryData(queryKeys.assets, [mockAsset]);
		const wrapper = ({ children }: { children: ReactNode }) => (
			<TestProviders queryClient={queryClient}>{children}</TestProviders>
		);
		return { queryClient, wrapper };
	}

	it("should report a failed create", async () => {
		// GIVEN
		const errorSpy = vi.spyOn(toast, "error");
		server.use(http.post(`${env.apiUrl}/assets`, () => HttpResponse.json({}, { status: 500 })));
		const { wrapper } = setup();
		const testSubject = () => useCreateAsset();
		const { result } = renderHook(testSubject, { wrapper });

		// WHEN
		result.current.mutate({ name: "Apartment" });

		// THEN
		await waitFor(() => expect(result.current.isError).toBe(true));
		expect(errorSpy).toHaveBeenCalledWith("Could not create the asset");
	});

	it("should report a failed update", async () => {
		// GIVEN
		const errorSpy = vi.spyOn(toast, "error");
		server.use(http.put(`${env.apiUrl}/assets/:id`, () => HttpResponse.json({}, { status: 500 })));
		const { wrapper } = setup();
		const testSubject = () => useUpdateAsset();
		const { result } = renderHook(testSubject, { wrapper });

		// WHEN
		result.current.mutate({ id: "a1", body: { name: "Apartment" } });

		// THEN
		await waitFor(() => expect(result.current.isError).toBe(true));
		expect(errorSpy).toHaveBeenCalledWith("Could not update the asset");
	});

	it("should restore the previous assets when the favorite fails", async () => {
		// GIVEN
		server.use(http.put(`${env.apiUrl}/assets/:id`, () => HttpResponse.json({}, { status: 500 })));
		const { queryClient, wrapper } = setup();
		const testSubject = () => useToggleFavorite();
		const { result } = renderHook(testSubject, { wrapper });

		// WHEN
		result.current.mutate({
			asset: buildAsset({ id: "a1", name: "Apartment", isFavorite: false })
		});

		// THEN
		await waitFor(() => expect(result.current.isError).toBe(true));
		expect(queryClient.getQueryData(queryKeys.assets)).toEqual([mockAsset]);
	});

	it("should rename an asset", async () => {
		// GIVEN
		const successSpy = vi.spyOn(toast, "success");
		const { wrapper } = setup();
		const testSubject = () => useRenameAsset();
		const { result } = renderHook(testSubject, { wrapper });

		// WHEN
		result.current.mutate({ asset: mockAsset, name: "New name" });

		// THEN
		await waitFor(() => expect(result.current.isSuccess).toBe(true));
		expect(successSpy).not.toHaveBeenCalledWith("Could not rename the asset");
	});

	it("should restore the previous assets when the rename fails", async () => {
		// GIVEN
		server.use(http.put(`${env.apiUrl}/assets/:id`, () => HttpResponse.json({}, { status: 500 })));
		const { queryClient, wrapper } = setup();
		const testSubject = () => useRenameAsset();
		const { result } = renderHook(testSubject, { wrapper });

		// WHEN
		result.current.mutate({ asset: mockAsset, name: "New name" });

		// THEN
		await waitFor(() => expect(result.current.isError).toBe(true));
		expect(queryClient.getQueryData(queryKeys.assets)).toEqual([mockAsset]);
	});

	it("should report a failed delete", async () => {
		// GIVEN
		const errorSpy = vi.spyOn(toast, "error");
		server.use(
			http.delete(`${env.apiUrl}/assets/:id`, () => HttpResponse.json({}, { status: 500 }))
		);
		const { wrapper } = setup();
		const testSubject = () => useDeleteAsset();
		const { result } = renderHook(testSubject, { wrapper });

		// WHEN
		result.current.mutate("a1");

		// THEN
		await waitFor(() => expect(result.current.isError).toBe(true));
		expect(errorSpy).toHaveBeenCalledWith("Could not delete the asset");
	});

	it("should optimistically update the current value with a zero base", async () => {
		// GIVEN
		server.use(http.post(`${env.apiUrl}/snapshots`, () => new Promise(() => undefined)));
		const zeroAsset = buildAsset({ id: "a1", latestValue: 0, latestValueBase: 0 });
		const { queryClient, wrapper } = setup();
		queryClient.setQueryData(queryKeys.assets, [zeroAsset]);
		const testSubject = () => useUpdateCurrentValue();
		const { result } = renderHook(testSubject, { wrapper });

		// WHEN
		result.current.mutate({ assetId: "a1", value: 500 });

		// THEN
		await waitFor(() => {
			const assets = queryClient.getQueryData<AssetDTO[]>(queryKeys.assets);
			expect(assets?.[0]?.latestValue).toBe(500);
			expect(assets?.[0]?.latestValueBase).toBe(500);
		});
	});

	it("should keep the currency ratio when updating the current value", async () => {
		// GIVEN
		server.use(http.post(`${env.apiUrl}/snapshots`, () => new Promise(() => undefined)));
		const { queryClient, wrapper } = setup();
		queryClient.setQueryData(queryKeys.assets, [
			buildAsset({ id: "a1", latestValue: 100, latestValueBase: 120 })
		]);
		const testSubject = () => useUpdateCurrentValue();
		const { result } = renderHook(testSubject, { wrapper });

		// WHEN
		result.current.mutate({ assetId: "a1", value: 200 });

		// THEN
		await waitFor(() => {
			const assets = queryClient.getQueryData<AssetDTO[]>(queryKeys.assets);
			expect(assets?.[0]?.latestValueBase).toBe(240);
		});
	});

	it("should fall back to the previous value when the base is missing", async () => {
		// GIVEN
		server.use(http.post(`${env.apiUrl}/snapshots`, () => new Promise(() => undefined)));
		const { queryClient, wrapper } = setup();
		queryClient.setQueryData(queryKeys.assets, [
			buildAsset({ id: "a1", latestValue: 100, latestValueBase: null })
		]);
		const testSubject = () => useUpdateCurrentValue();
		const { result } = renderHook(testSubject, { wrapper });

		// WHEN
		result.current.mutate({ assetId: "a1", value: 50 });

		// THEN
		await waitFor(() => {
			const assets = queryClient.getQueryData<AssetDTO[]>(queryKeys.assets);
			expect(assets?.[0]?.latestValueBase).toBe(50);
		});
	});

	it("should ignore a non-array cache and restore it on error", async () => {
		// GIVEN
		server.use(http.post(`${env.apiUrl}/snapshots`, () => HttpResponse.json({}, { status: 500 })));
		const { queryClient, wrapper } = setup();
		queryClient.setQueryData(queryKeys.assets, { not: "an array" });
		const testSubject = () => useUpdateCurrentValue();
		const { result } = renderHook(testSubject, { wrapper });

		// WHEN
		result.current.mutate({ assetId: "a1", value: 50 });

		// THEN
		await waitFor(() => expect(result.current.isError).toBe(true));
		expect(queryClient.getQueryData(queryKeys.assets)).toEqual({ not: "an array" });
	});
});

it("should report a failed favorite without a cached list", async () => {
	// GIVEN
	server.use(http.put(`${env.apiUrl}/assets/:id`, () => HttpResponse.json({}, { status: 500 })));
	const queryClient = new QueryClient({
		defaultOptions: { queries: { retry: false, gcTime: Infinity }, mutations: { retry: false } }
	});
	const wrapper = ({ children }: { children: ReactNode }) => (
		<TestProviders queryClient={queryClient}>{children}</TestProviders>
	);
	const testSubject = () => useToggleFavorite();
	const { result } = renderHook(testSubject, { wrapper });

	// WHEN
	result.current.mutate({ asset: buildAsset({ id: "a1", name: "Apartment", isFavorite: false }) });

	// THEN
	await waitFor(() => expect(result.current.isError).toBe(true));
});

it("should skip non-asset cache entries", async () => {
	// GIVEN
	server.use(http.post(`${env.apiUrl}/snapshots`, () => new Promise(() => undefined)));
	const queryClient = new QueryClient({
		defaultOptions: { queries: { retry: false, gcTime: Infinity }, mutations: { retry: false } }
	});
	const wrapper = ({ children }: { children: ReactNode }) => (
		<TestProviders queryClient={queryClient}>{children}</TestProviders>
	);
	queryClient.setQueryData(queryKeys.assets, [null, "text", { id: "other" }]);
	const testSubject = () => useUpdateCurrentValue();
	const { result } = renderHook(testSubject, { wrapper });

	// WHEN
	result.current.mutate({ assetId: "a1", value: 50 });

	// THEN
	await waitFor(() =>
		expect(queryClient.getQueryData(queryKeys.assets)).toEqual([null, "text", { id: "other" }])
	);
});

it("should report a failed rename without a cached list", async () => {
	// GIVEN
	server.use(http.put(`${env.apiUrl}/assets/:id`, () => HttpResponse.json({}, { status: 500 })));
	const queryClient = new QueryClient({
		defaultOptions: { queries: { retry: false, gcTime: Infinity }, mutations: { retry: false } }
	});
	const wrapper = ({ children }: { children: ReactNode }) => (
		<TestProviders queryClient={queryClient}>{children}</TestProviders>
	);
	const testSubject = () => useRenameAsset();
	const { result } = renderHook(testSubject, { wrapper });

	// WHEN
	result.current.mutate({ asset: buildAsset({ id: "a1", name: "Apartment" }), name: "New" });

	// THEN
	await waitFor(() => expect(result.current.isError).toBe(true));
});

it("should fall back to zero for a missing latest value", async () => {
	// GIVEN
	server.use(http.post(`${env.apiUrl}/snapshots`, () => new Promise(() => undefined)));
	const queryClient = new QueryClient({
		defaultOptions: { queries: { retry: false, gcTime: Infinity }, mutations: { retry: false } }
	});
	const wrapper = ({ children }: { children: ReactNode }) => (
		<TestProviders queryClient={queryClient}>{children}</TestProviders>
	);
	queryClient.setQueryData(queryKeys.assets, [
		buildAsset({ id: "a1", latestValue: null, latestValueBase: null })
	]);
	const testSubject = () => useUpdateCurrentValue();
	const { result } = renderHook(testSubject, { wrapper });

	// WHEN
	result.current.mutate({ assetId: "a1", value: 50 });

	// THEN
	await waitFor(() => {
		const assets = queryClient.getQueryData<AssetDTO[]>(queryKeys.assets);
		expect(assets?.[0]?.latestValueBase).toBe(50);
	});
});

it("should report a failed value update without a cached list", async () => {
	// GIVEN
	server.use(http.post(`${env.apiUrl}/snapshots`, () => HttpResponse.json({}, { status: 500 })));
	const queryClient = new QueryClient({
		defaultOptions: { queries: { retry: false, gcTime: Infinity }, mutations: { retry: false } }
	});
	const wrapper = ({ children }: { children: ReactNode }) => (
		<TestProviders queryClient={queryClient}>{children}</TestProviders>
	);
	const testSubject = () => useUpdateCurrentValue();
	const { result } = renderHook(testSubject, { wrapper });

	// WHEN
	result.current.mutate({ assetId: "a1", value: 50 });

	// THEN
	await waitFor(() => expect(result.current.isError).toBe(true));
});
