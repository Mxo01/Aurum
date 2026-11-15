import { renderHook, waitFor } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { toast } from "sonner";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/lib/api/client";
import { queryKeys } from "@/lib/api/keys";
import type {
	ApiErrorBody,
	ApiErrorDetail,
	CreateTargetDTO,
	UpdateTargetDTO
} from "@/lib/api/types";
import { env } from "@/lib/env";
import { buildTarget } from "@/test/fixtures/portfolio";
import { server } from "@/test/msw/server";
import { createTestQueryClient, TestProviders } from "@/test/render";
import { useCreateTarget, useDeleteTarget, useTargets, useUpdateTarget } from "./api";

const TARGET_ID = "11111111-1111-1111-1111-111111111111";

const REJECTION_STATUS = 400;
const REJECTION_ERROR = "Bad Request";
const SERVER_MESSAGE = "A target named House already exists";
const SILENT_FAILURE_STATUS = 500;

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

function buildEnvelope(detail: ApiErrorDetail): ApiErrorBody {
	return { status: REJECTION_STATUS, error: REJECTION_ERROR, errors: [detail] };
}

interface TargetMutationProbe {
	readonly mutate: () => void;
	readonly isError: boolean;
}

interface TargetMutationCase {
	readonly label: string;
	readonly fallback: string;
	readonly reject: (status: number, body: ApiErrorBody) => void;
	readonly probe: () => TargetMutationProbe;
}

function useCreateTargetProbe(): TargetMutationProbe {
	const mutation = useCreateTarget();
	const body: CreateTargetDTO = { name: "House", targetAmount: 400000, type: "MANUAL" };
	return {
		mutate: () => mutation.mutate(body),
		isError: mutation.isError
	};
}

function useUpdateTargetProbe(): TargetMutationProbe {
	const mutation = useUpdateTarget();
	const body: UpdateTargetDTO = { id: TARGET_ID, name: "House", targetAmount: 420000 };
	return {
		mutate: () => mutation.mutate({ id: TARGET_ID, body }),
		isError: mutation.isError
	};
}

function useDeleteTargetProbe(): TargetMutationProbe {
	const mutation = useDeleteTarget();
	return {
		mutate: () => mutation.mutate(TARGET_ID),
		isError: mutation.isError
	};
}

const TARGET_MUTATION_CASES: readonly TargetMutationCase[] = [
	{
		label: "create",
		fallback: "Could not create the target",
		reject: (status, body) =>
			server.use(http.post(`${env.apiUrl}/targets`, () => HttpResponse.json(body, { status }))),
		probe: useCreateTargetProbe
	},
	{
		label: "update",
		fallback: "Could not update the target",
		reject: (status, body) =>
			server.use(http.put(`${env.apiUrl}/targets/:id`, () => HttpResponse.json(body, { status }))),
		probe: useUpdateTargetProbe
	},
	{
		label: "delete",
		fallback: "Could not delete the target",
		reject: (status, body) =>
			server.use(
				http.delete(`${env.apiUrl}/targets/:id`, () => HttpResponse.json(body, { status }))
			),
		probe: useDeleteTargetProbe
	}
];

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
});

describe("target mutation failures", () => {
	afterEach(() => vi.restoreAllMocks());

	it.each(TARGET_MUTATION_CASES)(
		"should show the server sentence on a failed $label",
		async ({ reject, probe }) => {
			// GIVEN
			const errorSpy = vi.spyOn(toast, "error");
			reject(REJECTION_STATUS, buildEnvelope({ field: "name", message: SERVER_MESSAGE }));
			const testSubject = probe;
			const { result } = renderWithClient(testSubject);

			// WHEN
			result.current.mutate();

			// THEN
			await waitFor(() => expect(result.current.isError).toBe(true));
			expect(errorSpy).toHaveBeenCalledWith(SERVER_MESSAGE);
		}
	);

	it.each(TARGET_MUTATION_CASES)(
		"should fall back to its own text on a $label without an envelope",
		async ({ reject, probe, fallback }) => {
			// GIVEN
			const errorSpy = vi.spyOn(toast, "error");
			reject(SILENT_FAILURE_STATUS, {});
			const testSubject = probe;
			const { result } = renderWithClient(testSubject);

			// WHEN
			result.current.mutate();

			// THEN
			await waitFor(() => expect(result.current.isError).toBe(true));
			expect(errorSpy).toHaveBeenCalledWith(fallback);
		}
	);
});
