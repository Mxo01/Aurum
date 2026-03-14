export interface Snapshot {
	id?: string;
	assetId: string;
	assetName?: string;
	referenceDate: string;
	amountOriginalCurrency: number;
	exchangeRateToBase?: number;
	amountBaseCurrency?: number;
}
