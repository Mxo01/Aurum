import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useApi } from "@/lib/api/api-context";
import { unwrap } from "@/lib/api/client";
import { queryKeys } from "@/lib/api/keys";
import type { AssetDTO, CreateAssetDTO, UpdateAssetDTO } from "@/lib/api/types";
import { todayISO } from "@/lib/format/date";

export function useAssets(enabled = true) {
	const api = useApi();

	return useQuery({
		queryKey: queryKeys.assets,
		queryFn: async () => unwrap(await api.GET("/assets")),
		enabled
	});
}

function useInvalidatePortfolio() {
	const queryClient = useQueryClient();
	return () => {
		void queryClient.invalidateQueries({ queryKey: queryKeys.assets });
		void queryClient.invalidateQueries({ queryKey: ["analytics"] });
	};
}

export function useCreateAsset() {
	const api = useApi();
	const invalidate = useInvalidatePortfolio();

	return useMutation({
		mutationFn: async (body: CreateAssetDTO) => unwrap(await api.POST("/assets", { body })),
		onSuccess: () => {
			invalidate();
			toast.success("Asset created");
		},
		onError: () => toast.error("Could not create the asset")
	});
}

export function useUpdateAsset() {
	const api = useApi();
	const invalidate = useInvalidatePortfolio();

	return useMutation({
		mutationFn: async ({ id, body }: { id: string; body: UpdateAssetDTO }) =>
			unwrap(await api.PUT("/assets/{id}", { params: { path: { id } }, body })),
		onSuccess: () => {
			invalidate();
			toast.success("Asset updated");
		},
		onError: () => toast.error("Could not update the asset")
	});
}

export function useToggleFavorite() {
	const api = useApi();
	const queryClient = useQueryClient();

	return useMutation({
		mutationFn: async ({ id, isFavorite }: { id: string; isFavorite: boolean }) =>
			unwrap(await api.PUT("/assets/{id}", { params: { path: { id } }, body: { isFavorite } })),
		onMutate: async ({ id, isFavorite }) => {
			await queryClient.cancelQueries({ queryKey: queryKeys.assets });
			const previous = queryClient.getQueryData<AssetDTO[]>(queryKeys.assets);

			queryClient.setQueryData<AssetDTO[]>(queryKeys.assets, current =>
				current?.map(asset => (asset.id === id ? { ...asset, isFavorite } : asset))
			);

			return { previous };
		},
		onError: (_error, _variables, context) => {
			if (context?.previous) queryClient.setQueryData(queryKeys.assets, context.previous);
			toast.error("Could not update the dashboard tracking");
		},
		onSettled: () => {
			void queryClient.invalidateQueries({ queryKey: queryKeys.assets });
			void queryClient.invalidateQueries({ queryKey: ["analytics"] });
		}
	});
}

export function useRenameAsset() {
	const api = useApi();
	const queryClient = useQueryClient();

	return useMutation({
		mutationFn: async ({ id, name }: { id: string; name: string }) =>
			unwrap(await api.PUT("/assets/{id}", { params: { path: { id } }, body: { name } })),
		onMutate: async ({ id, name }) => {
			await queryClient.cancelQueries({ queryKey: queryKeys.assets });
			const previous = queryClient.getQueryData<AssetDTO[]>(queryKeys.assets);

			queryClient.setQueryData<AssetDTO[]>(queryKeys.assets, current =>
				current?.map(asset => (asset.id === id ? { ...asset, name } : asset))
			);

			return { previous };
		},
		onError: (_error, _variables, context) => {
			if (context?.previous) queryClient.setQueryData(queryKeys.assets, context.previous);
			toast.error("Could not rename the asset");
		},
		onSettled: () => queryClient.invalidateQueries({ queryKey: queryKeys.assets })
	});
}

export function useDeleteAsset() {
	const api = useApi();
	const queryClient = useQueryClient();

	return useMutation({
		mutationFn: async (id: string) =>
			unwrap(await api.DELETE("/assets/{id}", { params: { path: { id } } })),
		onSuccess: () => {
			void queryClient.invalidateQueries({ queryKey: queryKeys.assets });
			void queryClient.invalidateQueries({ queryKey: ["analytics"] });
			void queryClient.invalidateQueries({ queryKey: queryKeys.categories });
			toast.success("Asset deleted");
		},
		onError: () => toast.error("Could not delete the asset")
	});
}

export function useUpdateCurrentValue() {
	const api = useApi();
	const queryClient = useQueryClient();

	return useMutation({
		mutationFn: async ({ assetId, value }: { assetId: string; value: number }) =>
			unwrap(
				await api.POST("/snapshots", {
					body: {
						assetId,
						amountOriginalCurrency: value,
						referenceDate: todayISO()
					}
				})
			),
		onMutate: async ({ assetId, value }) => {
			await queryClient.cancelQueries({ queryKey: queryKeys.assets });
			const previous = queryClient.getQueryData(queryKeys.assets);

			queryClient.setQueryData(queryKeys.assets, (current: unknown) => {
				if (!Array.isArray(current)) return current;
				return current.map(asset => {
					if (typeof asset !== "object" || asset === null || asset.id !== assetId) return asset;
					const record = asset as Record<string, unknown>;
					const previousValue = Number(record.latestValue ?? 0);
					const previousBase = Number(record.latestValueBase ?? previousValue);
					const ratio = previousValue === 0 ? 1 : previousBase / previousValue;
					return { ...record, latestValue: value, latestValueBase: value * ratio };
				});
			});

			return { previous };
		},
		onError: (_error, _variables, context) => {
			if (context?.previous !== undefined) {
				queryClient.setQueryData(queryKeys.assets, context.previous);
			}
			toast.error("Could not save the new value");
		},
		onSettled: (_data, _error, { assetId }) => {
			void queryClient.invalidateQueries({ queryKey: queryKeys.assets });
			void queryClient.invalidateQueries({ queryKey: queryKeys.snapshots(assetId) });
			void queryClient.invalidateQueries({ queryKey: ["analytics"] });
		}
	});
}
