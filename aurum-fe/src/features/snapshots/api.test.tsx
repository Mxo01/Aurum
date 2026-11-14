import { renderHook, waitFor } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { toast } from "sonner";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/lib/api/client";
import { queryKeys } from "@/lib/api/keys";
import { env } from "@/lib/env";
import { buildSnapshot } from "@/test/fixtures/portfolio";
import { server } from "@/test/msw/server";
import { createTestQueryClient, TestProviders } from "@/test/render";
import { useCreateSnapshot, useDeleteSnapshots, useSnapshots } from "./api";

vi.mock("sonner", () => ({ toast: { success: vi.fn(), error: vi.fn() } }));

const mockAssetId = "a1";
const mockSnapshot = buildSnapshot({ assetId: mockAssetId });

function snapshotsUrl() {
	return `${env.apiUrl}/snapshots`;
}

beforeEach(() => {
	vi.mocked(toast.success).mockClear();
	vi.mocked(toast.error).mockClear();
});

describe("useSnapshots", () => {
	it("should expose the snapshots of the asset", async () => {
		// GIVEN
		server.use(http.get(snapshotsUrl(), () => HttpResponse.json([mockSnapshot])));
		const testSubject = () => useSnapshots(mockAssetId);

		// WHEN
		const { result } = renderHook(testSubject, { wrapper: TestProviders });

		// THEN
		await waitFor(() => expect(result.current.isSuccess).toBe(true));
		expect(result.current.data).toEqual([mockSnapshot]);
	});

	it("should expose an ApiError on failure", async () => {
		// GIVEN
		server.use(
			http.get(snapshotsUrl(), () => HttpResponse.json({ message: "boom" }, { status: 500 }))
		);
		const testSubject = () => useSnapshots(mockAssetId);

		// WHEN
		const { result } = renderHook(testSubject, { wrapper: TestProviders });

		// THEN
		await waitFor(() => expect(result.current.isError).toBe(true));
		expect(result.current.error).toBeInstanceOf(ApiError);
	});

	it("should stay idle without an asset id", () => {
		// GIVEN
		const fetchSpy = vi.fn(() => HttpResponse.json([mockSnapshot]));
		server.use(http.get(snapshotsUrl(), fetchSpy));
		const testSubject = () => useSnapshots(null);

		// WHEN
		const { result } = renderHook(testSubject, { wrapper: TestProviders });

		// THEN
		expect(result.current.isPending).toBe(true);
		expect(result.current.fetchStatus).toBe("idle");
		expect(fetchSpy).not.toHaveBeenCalled();
	});
});

describe("useCreateSnapshot", () => {
	it("should invalidate the snapshot, asset and analytics queries on success", async () => {
		// GIVEN
		const queryClient = createTestQueryClient();
		const invalidateSpy = vi.spyOn(queryClient, "invalidateQueries");
		const testSubject = () => useCreateSnapshot();
		const { result } = renderHook(testSubject, {
			wrapper: ({ children }) => <TestProviders queryClient={queryClient}>{children}</TestProviders>
		});

		// WHEN
		result.current.mutate({ assetId: mockAssetId, referenceDate: "2026-11-01" });

		// THEN
		await waitFor(() => expect(result.current.isSuccess).toBe(true));
		expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: queryKeys.snapshots(mockAssetId) });
		expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: queryKeys.assets });
		expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ["analytics"] });
		expect(toast.success).toHaveBeenCalledWith("Snapshot saved");
	});

	it("should skip the snapshot invalidation without an asset id", async () => {
		// GIVEN
		const queryClient = createTestQueryClient();
		const invalidateSpy = vi.spyOn(queryClient, "invalidateQueries");
		const testSubject = () => useCreateSnapshot();
		const { result } = renderHook(testSubject, {
			wrapper: ({ children }) => <TestProviders queryClient={queryClient}>{children}</TestProviders>
		});

		// WHEN
		result.current.mutate({ referenceDate: "2026-11-01" });

		// THEN
		await waitFor(() => expect(result.current.isSuccess).toBe(true));
		expect(invalidateSpy).not.toHaveBeenCalledWith({
			queryKey: queryKeys.snapshots(mockAssetId)
		});
		expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: queryKeys.assets });
	});

	it("should report the failure when the snapshot cannot be saved", async () => {
		// GIVEN
		server.use(
			http.post(snapshotsUrl(), () => HttpResponse.json({ message: "boom" }, { status: 500 }))
		);
		const testSubject = () => useCreateSnapshot();
		const { result } = renderHook(testSubject, { wrapper: TestProviders });

		// WHEN
		result.current.mutate({ assetId: mockAssetId });

		// THEN
		await waitFor(() => expect(result.current.isError).toBe(true));
		expect(toast.error).toHaveBeenCalledWith("Could not save the snapshot");
		expect(toast.success).not.toHaveBeenCalled();
	});
});

describe("useDeleteSnapshots", () => {
	it("should invalidate the snapshot, asset and analytics queries on success", async () => {
		// GIVEN
		const queryClient = createTestQueryClient();
		const invalidateSpy = vi.spyOn(queryClient, "invalidateQueries");
		const testSubject = () => useDeleteSnapshots();
		const { result } = renderHook(testSubject, {
			wrapper: ({ children }) => <TestProviders queryClient={queryClient}>{children}</TestProviders>
		});

		// WHEN
		result.current.mutate({ assetId: mockAssetId, ids: ["s1"] });

		// THEN
		await waitFor(() => expect(result.current.isSuccess).toBe(true));
		expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: queryKeys.snapshots(mockAssetId) });
		expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: queryKeys.assets });
		expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ["analytics"] });
		expect(toast.success).toHaveBeenCalledWith("Snapshots deleted");
	});

	it("should report the failure when the snapshots cannot be deleted", async () => {
		// GIVEN
		server.use(
			http.delete(`${snapshotsUrl()}/bulk`, () =>
				HttpResponse.json({ message: "boom" }, { status: 500 })
			)
		);
		const testSubject = () => useDeleteSnapshots();
		const { result } = renderHook(testSubject, { wrapper: TestProviders });

		// WHEN
		result.current.mutate({ assetId: mockAssetId, ids: ["s1"] });

		// THEN
		await waitFor(() => expect(result.current.isError).toBe(true));
		expect(toast.error).toHaveBeenCalledWith("Could not delete the snapshots");
	});
});
