import { QueryClient } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { http, HttpResponse } from "msw";
import { toast } from "sonner";
import { describe, expect, it, vi } from "vitest";
import { ApiError } from "@/lib/api/client";
import { queryKeys } from "@/lib/api/keys";
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
	useUploadPicture
} from "./api";

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

	it("should update the currency and invalidate the profile", async () => {
		// GIVEN
		let body: unknown;
		server.use(
			http.put(`${env.apiUrl}/users/currency`, async ({ request }) => {
				body = await request.json();
				return HttpResponse.json({});
			})
		);
		const { queryClient, wrapper } = setup();
		const invalidateSpy = vi.spyOn(queryClient, "invalidateQueries");
		const testSubject = () => useUpdateCurrency();
		const { result } = renderHook(testSubject, { wrapper });

		// WHEN
		result.current.mutate("USD");

		// THEN
		await waitFor(() => expect(result.current.isSuccess).toBe(true));
		expect(body).toEqual({ currency: "USD" });
		expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: queryKeys.profile });
	});

	it("should report a failed currency update", async () => {
		// GIVEN
		const errorSpy = vi.spyOn(toast, "error");
		server.use(
			http.put(`${env.apiUrl}/users/currency`, () => HttpResponse.json({}, { status: 500 }))
		);
		const { wrapper } = setup();
		const testSubject = () => useUpdateCurrency();
		const { result } = renderHook(testSubject, { wrapper });

		// WHEN
		result.current.mutate("USD");

		// THEN
		await waitFor(() => expect(result.current.isError).toBe(true));
		expect(errorSpy).toHaveBeenCalledWith("Could not update the currency");
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
		server.use(http.get(`${env.apiUrl}/users/export`, () => HttpResponse.json(buildProfile())));
		const { wrapper } = setup();
		const testSubject = () => useExportData();
		const { result } = renderHook(testSubject, { wrapper });

		// WHEN
		result.current.mutate();

		// THEN
		await waitFor(() => expect(result.current.isSuccess).toBe(true));
		expect(result.current.data).toBeDefined();
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
