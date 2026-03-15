import { Asset } from "../../model/asset.model";

export function mapAssetsToAssetsWithBalance(assets: Asset[]) {
	return assets.map(asset => {
		const assetSnapshots = (asset.snapshots || []).sort(
			(a, b) => new Date(b.referenceDate).getTime() - new Date(a.referenceDate).getTime()
		);

		const currentValue = assetSnapshots.length > 0 ? assetSnapshots[0].amountOriginalCurrency : 0;
		const previousValue =
			assetSnapshots.length > 1 ? assetSnapshots[1].amountOriginalCurrency : null;

		let trend = null;
		let trendPercentage = null;

		if (previousValue !== null && previousValue !== 0) {
			trend = currentValue - previousValue;
			trendPercentage = (trend / previousValue) * 100;
		} else if (previousValue === 0 && currentValue > 0) {
			trend = currentValue;
			trendPercentage = 100;
		}

		return {
			...asset,
			currentValue,
			trend,
			trendPercentage,
			snapshots: assetSnapshots
		};
	});
}
