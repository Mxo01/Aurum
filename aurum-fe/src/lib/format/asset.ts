import type { AssetDTO } from "@/lib/api/types";

export function assetTrend(asset: AssetDTO) {
	const { latestValue, previousValue } = asset;
	if (typeof latestValue !== "number" || typeof previousValue !== "number") return null;
	if (previousValue === 0) return null;

	return ((latestValue - previousValue) / previousValue) * 100;
}

export function assetValue(asset: AssetDTO) {
	return asset.latestValueBase ?? asset.latestValue ?? 0;
}
