import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useApi } from "@/lib/api/api-context";
import { unwrap } from "@/lib/api/client";
import { queryKeys } from "@/lib/api/keys";
import type { Currency, Locale, UserDTO } from "@/lib/api/types";

export const PROFILE_STALE_TIME = 5 * 60 * 1000;

export function useProfile() {
	const api = useApi();

	return useQuery({
		queryKey: queryKeys.profile,
		queryFn: async () => unwrap(await api.GET("/users")),
		staleTime: PROFILE_STALE_TIME
	});
}

export function useUpdateCurrency() {
	const api = useApi();
	const queryClient = useQueryClient();

	return useMutation({
		mutationFn: async (currency: Currency) =>
			unwrap(await api.PUT("/users/currency", { body: { currency } })),
		onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.profile }),
		onError: () => toast.error("Could not update the currency")
	});
}

export function useUpdateLocale() {
	const api = useApi();
	const queryClient = useQueryClient();

	return useMutation({
		mutationFn: async (locale: Locale) =>
			unwrap(await api.PUT("/users/locale", { body: { locale } })),
		onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.profile }),
		onError: () => toast.error("Could not update the locale")
	});
}

export function useUpdateName() {
	const api = useApi();
	const queryClient = useQueryClient();

	return useMutation({
		mutationFn: async (name: string) => unwrap(await api.PUT("/users", { body: { name } })),
		onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.profile }),
		onError: () => toast.error("Could not update the name")
	});
}

export function useUploadPicture() {
	const api = useApi();
	const queryClient = useQueryClient();

	return useMutation({
		mutationFn: async (file: File) => {
			const body = new FormData();
			body.append("file", file);
			return unwrap(
				await api.POST("/users/picture", {
					body: body as unknown as { file: string },
					bodySerializer: value => value as unknown as BodyInit
				})
			);
		},
		onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.profile }),
		onError: () => toast.error("Could not upload the picture")
	});
}

export function useDeletePicture() {
	const api = useApi();
	const queryClient = useQueryClient();

	return useMutation({
		mutationFn: async () => unwrap(await api.DELETE("/users/picture")),
		onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.profile }),
		onError: () => toast.error("Could not remove the picture")
	});
}

export function useDeleteProfile() {
	const api = useApi();

	return useMutation({
		mutationFn: async () => unwrap(await api.DELETE("/users")),
		onError: () => toast.error("Could not delete the account")
	});
}

export function useExportData() {
	const api = useApi();

	return useMutation({
		mutationFn: async () => unwrap(await api.GET("/users/export")),
		onError: () => toast.error("Could not export your data")
	});
}

export type ExportPayload = UserDTO;
