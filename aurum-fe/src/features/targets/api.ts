import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useApi } from "@/lib/api/api-context";
import { unwrap } from "@/lib/api/client";
import { queryKeys } from "@/lib/api/keys";
import type { CreateTargetDTO, UpdateTargetDTO } from "@/lib/api/types";

export function useTargets() {
	const api = useApi();

	return useQuery({
		queryKey: queryKeys.targets,
		queryFn: async () => unwrap(await api.GET("/targets"))
	});
}

export function useCreateTarget() {
	const api = useApi();
	const queryClient = useQueryClient();

	return useMutation({
		mutationFn: async (body: CreateTargetDTO) => unwrap(await api.POST("/targets", { body })),
		onSuccess: () => {
			void queryClient.invalidateQueries({ queryKey: queryKeys.targets });
			toast.success("Target created");
		},
		onError: () => toast.error("Could not create the target")
	});
}

export function useUpdateTarget() {
	const api = useApi();
	const queryClient = useQueryClient();

	return useMutation({
		mutationFn: async ({ id, body }: { id: string; body: UpdateTargetDTO }) =>
			unwrap(await api.PUT("/targets/{id}", { params: { path: { id } }, body })),
		onSuccess: () => {
			void queryClient.invalidateQueries({ queryKey: queryKeys.targets });
			toast.success("Target updated");
		},
		onError: () => toast.error("Could not update the target")
	});
}

export function useDeleteTarget() {
	const api = useApi();
	const queryClient = useQueryClient();

	return useMutation({
		mutationFn: async (id: string) =>
			unwrap(await api.DELETE("/targets/{id}", { params: { path: { id } } })),
		onSuccess: () => {
			void queryClient.invalidateQueries({ queryKey: queryKeys.targets });
			toast.success("Target deleted");
		},
		onError: () => toast.error("Could not delete the target")
	});
}
