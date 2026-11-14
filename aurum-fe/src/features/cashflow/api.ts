import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useApi } from "@/lib/api/api-context";
import { unwrap } from "@/lib/api/client";
import { queryKeys } from "@/lib/api/keys";
import type { CashFlowYearDTO, UpdateCashFlowEntryDTO } from "@/lib/api/types";

export function useCashflowYear(year: number) {
	const api = useApi();

	return useQuery({
		queryKey: queryKeys.cashflow(year),
		queryFn: async () => unwrap(await api.GET("/cashflows/{year}", { params: { path: { year } } })),
		staleTime: 60_000,
		placeholderData: previous => previous
	});
}

interface UpdateEntryVariables {
	year: number;
	month: number;
	entry: { earned: number; spent: number };
}

export function useUpdateCashflowEntry() {
	const api = useApi();
	const queryClient = useQueryClient();

	return useMutation({
		mutationFn: async ({ year, month, entry }: UpdateEntryVariables) =>
			unwrap(
				await api.PUT("/cashflows/{year}/{month}", {
					params: { path: { year, month } },
					body: entry satisfies UpdateCashFlowEntryDTO
				})
			),
		onMutate: async ({ year, month, entry }) => {
			const key = queryKeys.cashflow(year);
			await queryClient.cancelQueries({ queryKey: key });
			const previous = queryClient.getQueryData<CashFlowYearDTO>(key);

			queryClient.setQueryData<CashFlowYearDTO>(key, current => {
				if (!current) return current;
				return {
					...current,
					entries: current.entries.map(item =>
						item.month === month ? { ...item, ...entry } : item
					)
				};
			});

			return { previous, key };
		},
		onError: (_error, _variables, context) => {
			if (context?.previous) queryClient.setQueryData(context.key, context.previous);
			toast.error("Could not save the entry");
		},
		onSettled: (_data, _error, { year }) => {
			void queryClient.invalidateQueries({ queryKey: queryKeys.cashflow(year) });
		}
	});
}
