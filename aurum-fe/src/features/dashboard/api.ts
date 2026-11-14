import { useQuery } from "@tanstack/react-query";
import { useApi } from "@/lib/api/api-context";
import { unwrap } from "@/lib/api/client";
import { queryKeys } from "@/lib/api/keys";

export const ANALYTICS_STALE_TIME = 60_000;

export function useSummary() {
	const api = useApi();

	return useQuery({
		queryKey: queryKeys.summary,
		queryFn: async () => unwrap(await api.GET("/analytics/summary")),
		staleTime: ANALYTICS_STALE_TIME
	});
}

export function useChart(year?: number) {
	const api = useApi();

	return useQuery({
		queryKey: queryKeys.chart(year),
		queryFn: async () =>
			unwrap(
				await api.GET("/analytics/chart", {
					params: { query: year ? { year } : {} }
				})
			),
		staleTime: ANALYTICS_STALE_TIME,
		placeholderData: previous => previous
	});
}

export function useProjections() {
	const api = useApi();

	return useQuery({
		queryKey: queryKeys.projections,
		queryFn: async () =>
			unwrap(
				await api.GET("/analytics/projections", {
					params: { query: { years: 1, assetsOnly: true } }
				})
			),
		staleTime: ANALYTICS_STALE_TIME
	});
}
