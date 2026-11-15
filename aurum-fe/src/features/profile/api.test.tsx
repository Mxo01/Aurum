import { QueryClient, type QueryKey } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { http, HttpResponse, type RequestHandler } from "msw";
import { toast } from "sonner";
import { beforeEach, describe, expect, expectTypeOf, it, vi } from "vitest";
import { ApiError } from "@/lib/api/client";
import { queryKeys } from "@/lib/api/keys";
import type { UserExportDTO } from "@/lib/api/types";
import { env } from "@/lib/env";
import { buildProfile } from "@/test/fixtures/portfolio";
import { server } from "@/test/msw/server";
import { TestProviders } from "@/test/render";
import {
	useDeletePicture,
	useDeleteProfile,
	useExportData,
	useProfile,
	useUpdateCurrency,
	useUpdateLocale,
	useUpdateName,
	useUploadPicture,
	type ExportPayload
} from "./api";

beforeEach(() => {
	vi.restoreAllMocks();
});

const STORED_PICTURE = "ZmFrZV9qcGVn";
const STORED_PROFILE_CURRENCY = "EUR";
const STORED_PROFILE_LOCALE = "en-US";
const EXPORT_PROFILE_ID = "3f8a1d2c-5b6e-4a7f-9c1d-2e3f4a5b6c7d";
const EXPORT_PROFILE_EMAIL = "mario@example.com";

// Shaped from the generated export schema, so a contract change fails the typecheck.
const EXPORT_PAYLOAD = {
	profile: {
		id: EXPORT_PROFILE_ID,
		email: EXPORT_PROFILE_EMAIL,
		currency: STORED_PROFILE_CURRENCY,
		locale: STORED_PROFILE_LOCALE
	},
	categories: [],
	assets: [],
	targets: [],
	cashFlows: []
} satisfies ExportPayload;

interface ProfilePictureCase {
	label: string;
	picture: string | null;
	expectedPicture: string | null;
}

const PROFILE_PICTURE_CASES: ProfilePictureCase[] = [
	{
		label: "should expose the stored picture",
		picture: STORED_PICTURE,
		expectedPicture: STORED_PICTURE
	},
	{ label: "should keep a missing picture null", picture: null, expectedPicture: null }
];

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

	it.each(PROFILE_PICTURE_CASES)("$label", async testCase => {
		// GIVEN
		server.use(
			http.get(`${env.apiUrl}/users`, () =>
				HttpResponse.json(buildProfile({ picture: testCase.picture }))
			)
		);
		const testSubject = () => useProfile();

		// WHEN
		const { result } = renderHook(testSubject, { wrapper: TestProviders });

		// THEN
		await waitFor(() => expect(result.current.isSuccess).toBe(true));
		expect(result.current.data?.picture).toBe(testCase.expectedPicture);
		expect(result.current.data?.currency).toBe(STORED_PROFILE_CURRENCY);
		expect(result.current.data?.locale).toBe(STORED_PROFILE_LOCALE);
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

describe("ExportPayload", () => {
	it("should be the export schema", () => {
		// GIVEN
		const testSubject = EXPORT_PAYLOAD;

		// THEN
		expectTypeOf<ExportPayload>().toEqualTypeOf<UserExportDTO>();
		expect(testSubject.profile.email).toBe(EXPORT_PROFILE_EMAIL);
	});
});

interface NamedQueryKey {
	label: string;
	key: QueryKey;
}

const CHART_YEARS = [2024, 2025] as const;
const SNAPSHOT_ASSET_IDS = ["asset-a", "asset-b"] as const;
const UNTOUCHED_CASHFLOW_YEAR = 2025;

describe("profile mutations", () => {
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

	function seedCurrencyScopedQueries(queryClient: QueryClient) {
		const seeded: NamedQueryKey[] = [
			{ label: "profile", key: queryKeys.profile },
			{ label: "summary", key: queryKeys.summary },
			...CHART_YEARS.map(year => ({ label: `chart ${year}`, key: queryKeys.chart(year) })),
			{ label: "projections", key: queryKeys.projections },
			{ label: "assets", key: queryKeys.assets },
			...SNAPSHOT_ASSET_IDS.map(assetId => ({
				label: `snapshots ${assetId}`,
				key: queryKeys.snapshots(assetId)
			})),
			{ label: "targets", key: queryKeys.targets }
		];
		seeded.forEach(({ key }) => queryClient.setQueryData(key, {}));
		return seeded;
	}

	function seedUnscopedQueries(queryClient: QueryClient) {
		const seeded: NamedQueryKey[] = [
			{ label: "categories", key: queryKeys.categories },
			{
				label: `cashflow ${UNTOUCHED_CASHFLOW_YEAR}`,
				key: queryKeys.cashflow(UNTOUCHED_CASHFLOW_YEAR)
			}
		];
		seeded.forEach(({ key }) => queryClient.setQueryData(key, {}));
		return seeded;
	}

	it("should re-convert every currency-scoped view on a successful update", async () => {
		// GIVEN
		let body: unknown;
		server.use(
			http.put(`${env.apiUrl}/users/currency`, async ({ request }) => {
				body = await request.json();
				return HttpResponse.json({});
			})
		);
		const { queryClient, wrapper } = setup();
		const scoped = seedCurrencyScopedQueries(queryClient);
		const unscoped = seedUnscopedQueries(queryClient);
		const invalidateSpy = vi.spyOn(queryClient, "invalidateQueries");
		const testSubject = () => useUpdateCurrency();
		const { result } = renderHook(testSubject, { wrapper });

		// WHEN
		result.current.mutate("USD");

		// THEN
		await waitFor(() => expect(result.current.isSuccess).toBe(true));
		expect(body).toEqual({ currency: "USD" });
		expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: queryKeys.profile });
		scoped.forEach(({ label, key }) =>
			expect(queryClient.getQueryState(key)?.isInvalidated, `${label} is refetched`).toBe(true)
		);
		unscoped.forEach(({ label, key }) =>
			expect(queryClient.getQueryState(key)?.isInvalidated, `${label} stays fresh`).toBe(false)
		);
	});

	it("should report a failed currency update and invalidate nothing", async () => {
		// GIVEN
		const errorSpy = vi.spyOn(toast, "error");
		server.use(
			http.put(`${env.apiUrl}/users/currency`, () => HttpResponse.json({}, { status: 500 }))
		);
		const { queryClient, wrapper } = setup();
		const scoped = seedCurrencyScopedQueries(queryClient);
		const invalidateSpy = vi.spyOn(queryClient, "invalidateQueries");
		const testSubject = () => useUpdateCurrency();
		const { result } = renderHook(testSubject, { wrapper });

		// WHEN
		result.current.mutate("USD");

		// THEN
		await waitFor(() => expect(result.current.isError).toBe(true));
		expect(errorSpy).toHaveBeenCalledWith("Could not update the currency");
		expect(invalidateSpy).not.toHaveBeenCalled();
		scoped.forEach(({ label, key }) =>
			expect(queryClient.getQueryState(key)?.isInvalidated, `${label} stays fresh`).toBe(false)
		);
	});

	it("should update the locale and invalidate the profile", async () => {
		// GIVEN
		let body: unknown;
		server.use(
			http.put(`${env.apiUrl}/users/locale`, async ({ request }) => {
				body = await request.json();
				return HttpResponse.json({});
			})
		);
		const { queryClient, wrapper } = setup();
		const invalidateSpy = vi.spyOn(queryClient, "invalidateQueries");
		const testSubject = () => useUpdateLocale();
		const { result } = renderHook(testSubject, { wrapper });

		// WHEN
		result.current.mutate("it-IT");

		// THEN
		await waitFor(() => expect(result.current.isSuccess).toBe(true));
		expect(body).toEqual({ locale: "it-IT" });
		expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: queryKeys.profile });
	});

	it("should report a failed locale update", async () => {
		// GIVEN
		const errorSpy = vi.spyOn(toast, "error");
		server.use(
			http.put(`${env.apiUrl}/users/locale`, () => HttpResponse.json({}, { status: 500 }))
		);
		const { wrapper } = setup();
		const testSubject = () => useUpdateLocale();
		const { result } = renderHook(testSubject, { wrapper });

		// WHEN
		result.current.mutate("it-IT");

		// THEN
		await waitFor(() => expect(result.current.isError).toBe(true));
		expect(errorSpy).toHaveBeenCalledWith("Could not update the locale");
	});

	it("should update the name and invalidate the profile", async () => {
		// GIVEN
		let body: unknown;
		server.use(
			http.put(`${env.apiUrl}/users`, async ({ request }) => {
				body = await request.json();
				return HttpResponse.json({});
			})
		);
		const { queryClient, wrapper } = setup();
		const invalidateSpy = vi.spyOn(queryClient, "invalidateQueries");
		const testSubject = () => useUpdateName();
		const { result } = renderHook(testSubject, { wrapper });

		// WHEN
		result.current.mutate("Mario");

		// THEN
		await waitFor(() => expect(result.current.isSuccess).toBe(true));
		expect(body).toEqual({ name: "Mario" });
		expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: queryKeys.profile });
	});

	it("should report a failed name update", async () => {
		// GIVEN
		const errorSpy = vi.spyOn(toast, "error");
		server.use(http.put(`${env.apiUrl}/users`, () => HttpResponse.json({}, { status: 500 })));
		const { wrapper } = setup();
		const testSubject = () => useUpdateName();
		const { result } = renderHook(testSubject, { wrapper });

		// WHEN
		result.current.mutate("Mario");

		// THEN
		await waitFor(() => expect(result.current.isError).toBe(true));
		expect(errorSpy).toHaveBeenCalledWith("Could not update the name");
	});

	it("should upload the picture as form data", async () => {
		// GIVEN
		let uploaded: File | undefined;
		server.use(
			http.post(`${env.apiUrl}/users/picture`, async ({ request }) => {
				const data = await request.formData();
				uploaded = data.get("file") as File;
				return HttpResponse.json({});
			})
		);
		const { queryClient, wrapper } = setup();
		const invalidateSpy = vi.spyOn(queryClient, "invalidateQueries");
		const testSubject = () => useUploadPicture();
		const { result } = renderHook(testSubject, { wrapper });
		const file = new File(["avatar"], "avatar.png", { type: "image/png" });

		// WHEN
		result.current.mutate(file);

		// THEN
		await waitFor(() => expect(result.current.isSuccess).toBe(true));
		expect(uploaded?.name).toBe("avatar.png");
		expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: queryKeys.profile });
	});

	it("should report a failed picture upload", async () => {
		// GIVEN
		const errorSpy = vi.spyOn(toast, "error");
		server.use(
			http.post(`${env.apiUrl}/users/picture`, () => HttpResponse.json({}, { status: 500 }))
		);
		const { wrapper } = setup();
		const testSubject = () => useUploadPicture();
		const { result } = renderHook(testSubject, { wrapper });

		// WHEN
		result.current.mutate(new File(["avatar"], "avatar.png", { type: "image/png" }));

		// THEN
		await waitFor(() => expect(result.current.isError).toBe(true));
		expect(errorSpy).toHaveBeenCalledWith("Could not upload the picture");
	});

	it("should remove the picture and invalidate the profile", async () => {
		// GIVEN
		const { queryClient, wrapper } = setup();
		const invalidateSpy = vi.spyOn(queryClient, "invalidateQueries");
		const testSubject = () => useDeletePicture();
		const { result } = renderHook(testSubject, { wrapper });

		// WHEN
		result.current.mutate();

		// THEN
		await waitFor(() => expect(result.current.isSuccess).toBe(true));
		expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: queryKeys.profile });
	});

	it("should report a failed picture removal", async () => {
		// GIVEN
		const errorSpy = vi.spyOn(toast, "error");
		server.use(
			http.delete(`${env.apiUrl}/users/picture`, () => HttpResponse.json({}, { status: 500 }))
		);
		const { wrapper } = setup();
		const testSubject = () => useDeletePicture();
		const { result } = renderHook(testSubject, { wrapper });

		// WHEN
		result.current.mutate();

		// THEN
		await waitFor(() => expect(result.current.isError).toBe(true));
		expect(errorSpy).toHaveBeenCalledWith("Could not remove the picture");
	});

	it("should delete the profile", async () => {
		// GIVEN
		const { wrapper } = setup();
		const testSubject = () => useDeleteProfile();
		const { result } = renderHook(testSubject, { wrapper });

		// WHEN
		result.current.mutate();

		// THEN
		await waitFor(() => expect(result.current.isSuccess).toBe(true));
	});

	it("should report a failed account deletion", async () => {
		// GIVEN
		const errorSpy = vi.spyOn(toast, "error");
		server.use(http.delete(`${env.apiUrl}/users`, () => HttpResponse.json({}, { status: 500 })));
		const { wrapper } = setup();
		const testSubject = () => useDeleteProfile();
		const { result } = renderHook(testSubject, { wrapper });

		// WHEN
		result.current.mutate();

		// THEN
		await waitFor(() => expect(result.current.isError).toBe(true));
		expect(errorSpy).toHaveBeenCalledWith("Could not delete the account");
	});

	it("should export the data", async () => {
		// GIVEN
		server.use(http.get(`${env.apiUrl}/users/export`, () => HttpResponse.json(EXPORT_PAYLOAD)));
		const { wrapper } = setup();
		const testSubject = () => useExportData();
		const { result } = renderHook(testSubject, { wrapper });

		// WHEN
		result.current.mutate();

		// THEN
		await waitFor(() => expect(result.current.isSuccess).toBe(true));
		expect(result.current.data).toEqual(EXPORT_PAYLOAD);
		expectTypeOf(result.current.data).toEqualTypeOf<ExportPayload | undefined>();
	});

	it("should report a failed export", async () => {
		// GIVEN
		const errorSpy = vi.spyOn(toast, "error");
		server.use(
			http.get(`${env.apiUrl}/users/export`, () => HttpResponse.json({}, { status: 500 }))
		);
		const { wrapper } = setup();
		const testSubject = () => useExportData();
		const { result } = renderHook(testSubject, { wrapper });

		// WHEN
		result.current.mutate();

		// THEN
		await waitFor(() => expect(result.current.isError).toBe(true));
		expect(errorSpy).toHaveBeenCalledWith("Could not export your data");
	});
});

const VALIDATION_STATUS = 422;
const STORED_CURRENCY = "USD";
const STORED_LOCALE = "it-IT";
const STORED_NAME = "Mario";
const AVATAR = new File(["avatar"], "avatar.png", { type: "image/png" });

const CURRENCY_MESSAGE = "That currency is no longer supported";
const LOCALE_MESSAGE = "That locale is not available";
const NAME_MESSAGE = "The name must be at least two characters";
const PICTURE_UPLOAD_MESSAGE = "The picture must be smaller than 5 MB";
const PICTURE_REMOVAL_MESSAGE = "There is no picture to remove";
const ACCOUNT_DELETION_MESSAGE = "The account still owns assets";
const EXPORT_MESSAGE = "Your data is not ready to export yet";

interface ProfileErrorDetail {
	field?: string;
	message: string;
}

interface ProfileEndpoint {
	method: "get" | "post" | "put" | "delete";
	path: string;
}

function buildErrorEnvelope(detail: ProfileErrorDetail) {
	return { status: VALIDATION_STATUS, error: "Unprocessable Entity", errors: [detail] };
}

function failingHandler(endpoint: ProfileEndpoint, detail: ProfileErrorDetail): RequestHandler {
	const url = `${env.apiUrl}${endpoint.path}`;
	const respond = () =>
		HttpResponse.json(buildErrorEnvelope(detail), { status: VALIDATION_STATUS });

	switch (endpoint.method) {
		case "get":
			return http.get(url, respond);
		case "post":
			return http.post(url, respond);
		case "put":
			return http.put(url, respond);
		case "delete":
			return http.delete(url, respond);
	}
}

interface MutationProbe {
	isError: boolean;
	run: () => void;
}

function probeMutation<TVariables>(
	useMutation: () => { isError: boolean; mutate: (variables: TVariables) => void },
	variables: TVariables
): () => MutationProbe {
	return () => {
		const mutation = useMutation();
		return { isError: mutation.isError, run: () => mutation.mutate(variables) };
	};
}

function renderMutation(probe: () => MutationProbe) {
	const queryClient = new QueryClient({
		defaultOptions: {
			queries: { retry: false, gcTime: Infinity },
			mutations: { retry: false }
		}
	});
	const wrapper = ({ children }: { children: ReactNode }) => (
		<TestProviders queryClient={queryClient}>{children}</TestProviders>
	);
	return renderHook(probe, { wrapper });
}

interface MutationErrorMessageCase {
	label: string;
	detail: ProfileErrorDetail;
	endpoint: ProfileEndpoint;
	probe: () => MutationProbe;
}

const MUTATION_ERROR_CASES: MutationErrorMessageCase[] = [
	{
		label: "currency update",
		detail: { field: "currency", message: CURRENCY_MESSAGE },
		endpoint: { method: "put", path: "/users/currency" },
		probe: probeMutation(useUpdateCurrency, STORED_CURRENCY)
	},
	{
		label: "locale update",
		detail: { message: LOCALE_MESSAGE },
		endpoint: { method: "put", path: "/users/locale" },
		probe: probeMutation(useUpdateLocale, STORED_LOCALE)
	},
	{
		label: "name update",
		detail: { field: "name", message: NAME_MESSAGE },
		endpoint: { method: "put", path: "/users" },
		probe: probeMutation(useUpdateName, STORED_NAME)
	},
	{
		label: "picture upload",
		detail: { message: PICTURE_UPLOAD_MESSAGE },
		endpoint: { method: "post", path: "/users/picture" },
		probe: probeMutation(useUploadPicture, AVATAR)
	},
	{
		label: "picture removal",
		detail: { message: PICTURE_REMOVAL_MESSAGE },
		endpoint: { method: "delete", path: "/users/picture" },
		probe: probeMutation(useDeletePicture, undefined)
	},
	{
		label: "account deletion",
		detail: { message: ACCOUNT_DELETION_MESSAGE },
		endpoint: { method: "delete", path: "/users" },
		probe: probeMutation(useDeleteProfile, undefined)
	},
	{
		label: "data export",
		detail: { message: EXPORT_MESSAGE },
		endpoint: { method: "get", path: "/users/export" },
		probe: probeMutation(useExportData, undefined)
	}
];

describe("profile mutation error messages", () => {
	it.each(MUTATION_ERROR_CASES)(
		"should toast the backend sentence for a failed $label",
		async testCase => {
			// GIVEN
			const errorSpy = vi.spyOn(toast, "error");
			server.use(failingHandler(testCase.endpoint, testCase.detail));
			const { result } = renderMutation(testCase.probe);

			// WHEN
			result.current.run();

			// THEN
			await waitFor(() => expect(result.current.isError).toBe(true));
			expect(errorSpy).toHaveBeenCalledWith(testCase.detail.message);
		}
	);
});
