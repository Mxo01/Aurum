import { renderHook, waitFor } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { toast } from "sonner";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/lib/api/client";
import { queryKeys } from "@/lib/api/keys";
import type { ApiErrorBody } from "@/lib/api/types";
import { env } from "@/lib/env";
import { buildSnapshot } from "@/test/fixtures/portfolio";
import { server } from "@/test/msw/server";
import { createTestQueryClient, TestProviders } from "@/test/render";
import { useCreateSnapshot, useDeleteSnapshots, useSnapshots } from "./api";

vi.mock("sonner", () => ({ toast: { success: vi.fn(), error: vi.fn() } }));

const mockAssetId = "a1";
const mockSnapshotId = "s1";
const mockSnapshot = buildSnapshot({ id: mockSnapshotId, assetId: mockAssetId });
const mockReferenceDate = "2026-11-01";
const rejectionStatus = 400;
const rejectionError = "Bad Request";
const rejectedField = "referenceDate";
const saveFallback = "Could not save the snapshot";
const deleteFallback = "Could not delete the snapshots";

function snapshotsUrl() {
	return `${env.apiUrl}/snapshots`;
}

function rejectionEnvelope(message: string): ApiErrorBody {
	return {
		status: rejectionStatus,
		error: rejectionError,
		errors: [{ field: rejectedField, message }]
	};
}

function serverAnswer(body?: unknown) {
	return body === undefined
		? new HttpResponse(null, { status: rejectionStatus })
		: HttpResponse.json(body, { status: rejectionStatus });
}

interface MutationCase {
	readonly label: string;
	readonly serverMessage: string;
	readonly fallback: string;
	readonly respond: (body?: unknown) => void;
	readonly mount: () => { trigger: () => void; isError: () => boolean };
}

const MUTATION_CASES: MutationCase[] = [
	{
		label: "save",
		serverMessage: "The snapshot date cannot be in the future",
		fallback: saveFallback,
		respond: body => server.use(http.post(snapshotsUrl(), () => serverAnswer(body))),
		mount: () => {
			const { result } = renderHook(() => useCreateSnapshot(), { wrapper: TestProviders });
			return {
				trigger: () =>
					result.current.mutate({ assetId: mockAssetId, referenceDate: mockReferenceDate }),
				isError: () => result.current.isError
			};
		}
	},
	{
		label: "delete",
		serverMessage: "Keep at least one snapshot for the asset",
		fallback: deleteFallback,
		respond: body => server.use(http.delete(`${snapshotsUrl()}/bulk`, () => serverAnswer(body))),
		mount: () => {
			const { result } = renderHook(() => useDeleteSnapshots(), { wrapper: TestProviders });
			return {
				trigger: () => result.current.mutate({ assetId: mockAssetId, ids: [mockSnapshotId] }),
				isError: () => result.current.isError
			};
		}
	}
];

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
		result.current.mutate({ assetId: mockAssetId, ids: [mockSnapshotId] });

		// THEN
		await waitFor(() => expect(result.current.isSuccess).toBe(true));
		expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: queryKeys.snapshots(mockAssetId) });
		expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: queryKeys.assets });
		expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ["analytics"] });
		expect(toast.success).toHaveBeenCalledWith("Snapshots deleted");
	});
});

describe("snapshot mutation toasts", () => {
	it.each(MUTATION_CASES)(
		"should show the backend message in the $label toast",
		async ({ serverMessage, respond, mount }) => {
			// GIVEN
			respond(rejectionEnvelope(serverMessage));
			const mutation = mount();

			// WHEN
			mutation.trigger();

			// THEN
			await waitFor(() => expect(mutation.isError()).toBe(true));
			expect(toast.error).toHaveBeenCalledWith(serverMessage);
			expect(toast.success).not.toHaveBeenCalled();
		}
	);

	it.each(MUTATION_CASES)(
		"should keep the $label fallback when the response carries no message",
		async ({ fallback, respond, mount }) => {
			// GIVEN
			respond();
			const mutation = mount();

			// WHEN
			mutation.trigger();

			// THEN
			await waitFor(() => expect(mutation.isError()).toBe(true));
			expect(toast.error).toHaveBeenCalledWith(fallback);
		}
	);
});
