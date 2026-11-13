import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useApi } from "@/lib/api/api-context";
import { unwrap } from "@/lib/api/client";
import { queryKeys } from "@/lib/api/keys";
import type { CreateAssetCategoryDTO, UpdateAssetCategoryDTO } from "@/lib/api/types";

export function useCategories() {
	const api = useApi();

	return useQuery({
		queryKey: queryKeys.categories,
		queryFn: async () => unwrap(await api.GET("/categories")),
		staleTime: 5 * 60 * 1000
	});
}

function useInvalidateCategories() {
	const queryClient = useQueryClient();
	return () => {
		void queryClient.invalidateQueries({ queryKey: queryKeys.categories });
		void queryClient.invalidateQueries({ queryKey: queryKeys.assets });
		void queryClient.invalidateQueries({ queryKey: ["analytics"] });
	};
}

export function useCreateCategory() {
	const api = useApi();
	const invalidate = useInvalidateCategories();

	return useMutation({
		mutationFn: async (body: CreateAssetCategoryDTO) =>
			unwrap(await api.POST("/categories", { body })),
		onSuccess: () => {
			invalidate();
			toast.success("Category created");
		},
		onError: () => toast.error("Could not create the category")
	});
}

export function useUpdateCategory() {
	const api = useApi();
	const invalidate = useInvalidateCategories();

	return useMutation({
		mutationFn: async ({ id, body }: { id: string; body: UpdateAssetCategoryDTO }) =>
			unwrap(await api.PUT("/categories/{id}", { params: { path: { id } }, body })),
		onSuccess: () => {
			invalidate();
			toast.success("Category updated");
		},
		onError: () => toast.error("Could not update the category")
	});
}

export function useDeleteCategory() {
	const api = useApi();
	const invalidate = useInvalidateCategories();

	return useMutation({
		mutationFn: async (id: string) =>
			unwrap(await api.DELETE("/categories/{id}", { params: { path: { id } } })),
		onSuccess: () => {
			invalidate();
			toast.success("Category deleted");
		},
		onError: () => toast.error("Could not delete the category")
	});
}
