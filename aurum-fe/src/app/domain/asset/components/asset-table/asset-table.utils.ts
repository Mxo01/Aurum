import { Asset } from "../../model/asset.model";

export function mapAssetsToAssetsWithBalance(assets: Asset[]) {
	return assets.map(asset => {
		const currentValue = asset.latestValue ?? 0;
		const currentValueBase = asset.latestValueBase ?? currentValue;
		const previousValue = asset.previousValue ?? null;

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
			currentValueBase,
			trend,
			trendPercentage
		};
	});
}
