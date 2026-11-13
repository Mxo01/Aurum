import type {
	AnalyticsSummaryDTO,
	ChartDataDTO,
	DeltaDTO,
	VariationDTO
} from "../../../shared/api/contract";
import type { Asset } from "../../asset/model/asset.model";

export type Delta = DeltaDTO;
export type Variation = VariationDTO;
/**
 * `topAssets` narrows the contract's currency string union to the frontend
 * `Currency` enum, which is what the charts and formatters accept.
 */
export type AnalyticsSummary = Omit<AnalyticsSummaryDTO, "topAssets"> & {
	topAssets: Asset[];
};
export type ChartData = ChartDataDTO;

/** Year → projected net worth. Computed client-side, never sent. */
export type Projections = { [year: number]: number };
