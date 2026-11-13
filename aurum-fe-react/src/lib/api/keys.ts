export const queryKeys = {
	profile: ["profile"] as const,
	summary: ["analytics", "summary"] as const,
	chart: (year?: number) => ["analytics", "chart", year ?? "all"] as const,
	projections: ["analytics", "projections"] as const,
	assets: ["assets"] as const,
	categories: ["categories"] as const,
	snapshots: (assetId: string) => ["snapshots", assetId] as const,
	targets: ["targets"] as const,
	cashflow: (year: number) => ["cashflow", year] as const
};
