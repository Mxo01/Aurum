import type { AssetDTO, AssetType } from "@/lib/api/types";
import { assetValue } from "@/lib/format/asset";

export interface AssetGroup {
	name: string;
	icon: string | null;
	type: AssetType;
	total: number;
	assets: AssetDTO[];
}

export function groupAssets(assets: AssetDTO[]): AssetGroup[] {
	const groups = new Map<string, AssetGroup>();

	for (const asset of assets) {
		const existing = groups.get(asset.categoryName);

		if (existing) {
			existing.assets.push(asset);
			existing.total += assetValue(asset);
			continue;
		}

		groups.set(asset.categoryName, {
			name: asset.categoryName,
			icon: asset.categoryIcon,
			type: asset.type,
			total: assetValue(asset),
			assets: [asset]
		});
	}

	return Array.from(groups.values())
		.map(group => ({
			...group,
			assets: [...group.assets].sort((a, b) => a.name.localeCompare(b.name))
		}))
		.sort((a, b) => a.name.localeCompare(b.name));
}

export function portfolioTotals(assets: AssetDTO[]) {
	let assetTotal = 0;
	let liabilityTotal = 0;

	for (const asset of assets) {
		if (asset.type === "LIABILITY") liabilityTotal += assetValue(asset);
		else assetTotal += assetValue(asset);
	}

	return { assetTotal, liabilityTotal, netTotal: assetTotal - liabilityTotal };
}
