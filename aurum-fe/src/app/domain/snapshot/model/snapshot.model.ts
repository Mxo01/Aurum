import type { SnapshotDTO } from "../../../shared/api/contract";

/**
 * A snapshot as the client sends it: `id`, the derived base-currency amount,
 * and the asset name are assigned by the server.
 */
export type Snapshot = Omit<
	SnapshotDTO,
	"id" | "assetName" | "exchangeRateToBase" | "amountBaseCurrency"
> & {
	id?: string;
	assetName?: string | null;
	exchangeRateToBase?: number | null;
	amountBaseCurrency?: number | null;
};
