import { renderHook, waitFor } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { toast } from "sonner";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/lib/api/client";
import { queryKeys } from "@/lib/api/keys";
import type { CreateTargetDTO, UpdateTargetDTO } from "@/lib/api/types";
import { env } from "@/lib/env";
import { buildTarget } from "@/test/fixtures/portfolio";
import { server } from "@/test/msw/server";
import { createTestQueryClient, TestProviders } from "@/test/render";
import { useCreateTarget, useDeleteTarget, useTargets, useUpdateTarget } from "./api";

const TARGET_ID = "11111111-1111-1111-1111-111111111111";

function renderWithClient<T>(hook: () => T) {
	const queryClient = createTestQueryClient();
	const invalidateSpy = vi.spyOn(queryClient, "invalidateQueries");

	return {
		invalidateSpy,
		...renderHook(hook, {
			wrapper: ({ children }) => <TestProviders queryClient={queryClient}>{children}</TestProviders>
		})
	};
}

describe("useTargets", () => {
	it("should expose the targets on success", async () => {
		// GIVEN
		const testSubject = () => useTargets();

		// WHEN
		const { result } = renderHook(testSubject, { wrapper: TestProviders });

		// THEN
		await waitFor(() => expect(result.current.isSuccess).toBe(true));
		expect(result.current.data?.length).toBeGreaterThan(0);
	});

	it("should expose an ApiError on failure", async () => {
		// GIVEN
		server.use(
			http.get(`${env.apiUrl}/targets`, () =>
				HttpResponse.json({ message: "boom" }, { status: 500 })
			)
		);
		const testSubject = () => useTargets();

		// WHEN
		const { result } = renderHook(testSubject, { wrapper: TestProviders });

		// THEN
		await waitFor(() => expect(result.current.isError).toBe(true));
		expect(result.current.error).toBeInstanceOf(ApiError);
	});
});

describe("useCreateTarget", () => {
	afterEach(() => vi.restoreAllMocks());

	it("should send the body, invalidate the targets and confirm", async () => {
		// GIVEN
		let requestBody: CreateTargetDTO | undefined;
		server.use(
			http.post(`${env.apiUrl}/targets`, async ({ request }) => {
				requestBody = (await request.json()) as CreateTargetDTO;
				return HttpResponse.json(buildTarget());
			})
		);
		const successSpy = vi.spyOn(toast, "success");
		const body: CreateTargetDTO = { name: "House", targetAmount: 400000, type: "MANUAL" };
		const { result, invalidateSpy } = renderWithClient(() => useCreateTarget());

		// WHEN
		result.current.mutate(body);

		// THEN
		await waitFor(() => expect(result.current.isSuccess).toBe(true));
		expect(requestBody).toEqual(body);
		expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: queryKeys.targets });
		expect(successSpy).toHaveBeenCalledWith("Target created");
	});

	it("should report the failure", async () => {
		// GIVEN
		server.use(
			http.post(`${env.apiUrl}/targets`, () =>
				HttpResponse.json({ message: "boom" }, { status: 500 })
			)
		);
		const errorSpy = vi.spyOn(toast, "error");
		const { result } = renderWithClient(() => useCreateTarget());

		// WHEN
		result.current.mutate({ name: "House" });

		// THEN
		await waitFor(() => expect(result.current.isError).toBe(true));
		expect(errorSpy).toHaveBeenCalledWith("Could not create the target");
	});
});

describe("useUpdateTarget", () => {
	afterEach(() => vi.restoreAllMocks());

	it("should send the path id and the body, invalidate the targets and confirm", async () => {
		// GIVEN
		let requestedId: string | undefined;
		let requestBody: UpdateTargetDTO | undefined;
		server.use(
			http.put(`${env.apiUrl}/targets/:id`, async ({ request, params }) => {
				requestedId = String(params.id);
				requestBody = (await request.json()) as UpdateTargetDTO;
				return HttpResponse.json(buildTarget());
			})
		);
		const successSpy = vi.spyOn(toast, "success");
		const body: UpdateTargetDTO = { id: TARGET_ID, name: "House", targetAmount: 420000 };
		const { result, invalidateSpy } = renderWithClient(() => useUpdateTarget());

		// WHEN
		result.current.mutate({ id: TARGET_ID, body });

		// THEN
		await waitFor(() => expect(result.current.isSuccess).toBe(true));
		expect(requestedId).toBe(TARGET_ID);
		expect(requestBody).toEqual(body);
		expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: queryKeys.targets });
		expect(successSpy).toHaveBeenCalledWith("Target updated");
	});

	it("should report the failure", async () => {
		// GIVEN
		server.use(
			http.put(`${env.apiUrl}/targets/:id`, () =>
				HttpResponse.json({ message: "boom" }, { status: 500 })
			)
		);
		const errorSpy = vi.spyOn(toast, "error");
		const { result } = renderWithClient(() => useUpdateTarget());

		// WHEN
		result.current.mutate({ id: TARGET_ID, body: { name: "House" } });

		// THEN
		await waitFor(() => expect(result.current.isError).toBe(true));
		expect(errorSpy).toHaveBeenCalledWith("Could not update the target");
	});
});

describe("useDeleteTarget", () => {
	afterEach(() => vi.restoreAllMocks());

	it("should delete the target by id, invalidate the targets and confirm", async () => {
		// GIVEN
		let requestedId: string | undefined;
		server.use(
			http.delete(`${env.apiUrl}/targets/:id`, ({ params }) => {
				requestedId = String(params.id);
				return new HttpResponse(null, { status: 200 });
			})
		);
		const successSpy = vi.spyOn(toast, "success");
		const { result, invalidateSpy } = renderWithClient(() => useDeleteTarget());

		// WHEN
		result.current.mutate(TARGET_ID);

		// THEN
		await waitFor(() => expect(result.current.isSuccess).toBe(true));
		expect(requestedId).toBe(TARGET_ID);
		expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: queryKeys.targets });
		expect(successSpy).toHaveBeenCalledWith("Target deleted");
	});

	it("should report the failure", async () => {
		// GIVEN
		server.use(
			http.delete(`${env.apiUrl}/targets/:id`, () =>
				HttpResponse.json({ message: "boom" }, { status: 500 })
			)
		);
		const errorSpy = vi.spyOn(toast, "error");
		const { result } = renderWithClient(() => useDeleteTarget());

		// WHEN
		result.current.mutate(TARGET_ID);

		// THEN
		await waitFor(() => expect(result.current.isError).toBe(true));
		expect(errorSpy).toHaveBeenCalledWith("Could not delete the target");
	});
});
