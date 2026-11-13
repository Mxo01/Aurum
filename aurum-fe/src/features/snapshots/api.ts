import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useApi } from "@/lib/api/api-context";
import { unwrap } from "@/lib/api/client";
import { queryKeys } from "@/lib/api/keys";
import type { CreateSnapshotDTO } from "@/lib/api/types";

export function useSnapshots(assetId: string | null) {
	const api = useApi();

	return useQuery({
		queryKey: queryKeys.snapshots(assetId ?? ""),
		queryFn: async () =>
			unwrap(await api.GET("/snapshots", { params: { query: { assetId: assetId ?? undefined } } })),
		enabled: Boolean(assetId)
	});
}

export function useCreateSnapshot() {
	const api = useApi();
	const queryClient = useQueryClient();

	return useMutation({
		mutationFn: async (body: CreateSnapshotDTO) => unwrap(await api.POST("/snapshots", { body })),
		onSuccess: (_data, variables) => {
			if (variables.assetId) {
				void queryClient.invalidateQueries({ queryKey: queryKeys.snapshots(variables.assetId) });
			}
			void queryClient.invalidateQueries({ queryKey: queryKeys.assets });
			void queryClient.invalidateQueries({ queryKey: ["analytics"] });
			toast.success("Snapshot saved");
		},
		onError: () => toast.error("Could not save the snapshot")
	});
}

export function useDeleteSnapshots() {
	const api = useApi();
	const queryClient = useQueryClient();

	return useMutation({
		mutationFn: async ({ assetId, ids }: { assetId: string; ids: string[] }) =>
			unwrap(
				await api.DELETE("/snapshots/bulk", {
					params: { query: { assetId } },
					body: ids
				})
			),
		onSuccess: (_data, variables) => {
			void queryClient.invalidateQueries({ queryKey: queryKeys.snapshots(variables.assetId) });
			void queryClient.invalidateQueries({ queryKey: queryKeys.assets });
			void queryClient.invalidateQueries({ queryKey: ["analytics"] });
			toast.success("Snapshots deleted");
		},
		onError: () => toast.error("Could not delete the snapshots")
	});
}
