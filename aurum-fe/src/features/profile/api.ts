import type { QueryKey } from "@tanstack/react-query";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useApi } from "@/lib/api/api-context";
import { unwrap } from "@/lib/api/client";
import { getApiErrorMessage } from "@/lib/api/errors";
import { queryKeys } from "@/lib/api/keys";
import type { Currency, Locale, UserDTO } from "@/lib/api/types";

export const PROFILE_STALE_TIME = 5 * 60 * 1000;

const CHART_KEY_DEPTH = 2;
const SNAPSHOT_KEY_DEPTH = 1;

// A currency switch re-converts every stored amount, so the whole family of
// parameterised analytics and snapshot queries goes stale, not just one entry.
const CURRENCY_SCOPED_KEYS: QueryKey[] = [
	queryKeys.profile,
	queryKeys.summary,
	queryKeys.chart().slice(0, CHART_KEY_DEPTH),
	queryKeys.projections,
	queryKeys.assets,
	queryKeys.snapshots("").slice(0, SNAPSHOT_KEY_DEPTH),
	queryKeys.targets
];

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
		onSuccess: () =>
			Promise.all(
				CURRENCY_SCOPED_KEYS.map(queryKey => queryClient.invalidateQueries({ queryKey }))
			),
		onError: error => toast.error(getApiErrorMessage(error, "Could not update the currency"))
	});
}

export function useUpdateLocale() {
	const api = useApi();
	const queryClient = useQueryClient();

	return useMutation({
		mutationFn: async (locale: Locale) =>
			unwrap(await api.PUT("/users/locale", { body: { locale } })),
		onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.profile }),
		onError: error => toast.error(getApiErrorMessage(error, "Could not update the locale"))
	});
}

export function useUpdateName() {
	const api = useApi();
	const queryClient = useQueryClient();

	return useMutation({
		mutationFn: async (name: string) => unwrap(await api.PUT("/users", { body: { name } })),
		onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.profile }),
		onError: error => toast.error(getApiErrorMessage(error, "Could not update the name"))
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
		onError: error => toast.error(getApiErrorMessage(error, "Could not upload the picture"))
	});
}

export function useDeletePicture() {
	const api = useApi();
	const queryClient = useQueryClient();

	return useMutation({
		mutationFn: async () => unwrap(await api.DELETE("/users/picture")),
		onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.profile }),
		onError: error => toast.error(getApiErrorMessage(error, "Could not remove the picture"))
	});
}

export function useDeleteProfile() {
	const api = useApi();

	return useMutation({
		mutationFn: async () => unwrap(await api.DELETE("/users")),
		onError: error => toast.error(getApiErrorMessage(error, "Could not delete the account"))
	});
}

export function useExportData() {
	const api = useApi();

	return useMutation({
		mutationFn: async () => unwrap(await api.GET("/users/export")),
		onError: error => toast.error(getApiErrorMessage(error, "Could not export your data"))
	});
}

export type ExportPayload = UserDTO;
