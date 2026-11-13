import type { components } from "./schema";

/**
 * The API wire contract, generated from `aurum-be/openapi.json`.
 *
 * This is the single source of truth for the shapes shared with the backend:
 * field names, primitive types, nullability, and enum values all come from the
 * Java DTOs. After changing a DTO, regenerate with `npm run api:types` — never
 * edit `schema.d.ts` by hand.
 *
 * Frontend-only view models (form values, table rows, chart series) do not
 * belong here; they stay next to the component that owns them.
 */
export type Schemas = components["schemas"];

export type AssetDTO = Schemas["AssetDTO"];
export type AssetCategoryDTO = Schemas["AssetCategoryDTO"];
export type SnapshotDTO = Schemas["SnapshotDTO"];
export type TargetDTO = Schemas["TargetDTO"];
export type AnalyticsSummaryDTO = Schemas["AnalyticsSummaryDTO"];
export type DeltaDTO = Schemas["DeltaDTO"];
export type VariationDTO = Schemas["VariationDTO"];
export type ChartDataDTO = Schemas["ChartDataDTO"];
export type CashFlowEntryDTO = Schemas["CashFlowEntryDTO"];
export type CashFlowYearDTO = Schemas["CashFlowYearDTO"];
export type UpdateCashFlowEntryDTO = Schemas["UpdateCashFlowEntryDTO"];
