import { Plus } from "lucide-react";
import { Link, useSearchParams } from "react-router";
import { Delta } from "@/components/app/delta";
import { PageHeader } from "@/components/app/page-header";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { usePreferences } from "@/lib/format/use-preferences";
import { useTargets } from "@/features/targets/api";
import { useChart, useProjections, useSummary } from "./api";
import { liabilitiesSparkline, projectedGrowth } from "./chart-utils";
import { NetWorthChart } from "./net-worth-chart";
import { Sparkline, StatTile } from "./stat-tile";
import { TargetsCard } from "./targets-card";
import { TopAssetsCard } from "./top-assets-card";

const TILE_DIGITS = 0;
const GROWTH_DIGITS = 1;

function parseYear(value: string | null) {
	if (!value) return undefined;

	const parsed = Number(value);
	return Number.isInteger(parsed) && parsed > 0 ? parsed : undefined;
}

export function DashboardPage() {
	const [searchParams, setSearchParams] = useSearchParams();
	const year = parseYear(searchParams.get("year"));
	const { money, percent, currency } = usePreferences();

	const summary = useSummary();
	const chart = useChart(year);
	const targets = useTargets();
	const projections = useProjections();

	const onYearChange = (next: number | undefined) => {
		const params = new URLSearchParams(searchParams);
		if (next === undefined) params.delete("year");
		else params.set("year", String(next));
		setSearchParams(params, { replace: true });
	};

	const retry = () => {
		void summary.refetch();
		void chart.refetch();
		void targets.refetch();
		void projections.refetch();
	};

	const projected = projections.data?.[1];
	const liabilityValues = liabilitiesSparkline(chart.data);
	const liabilityTrend =
		liabilityValues.length > 1
			? (liabilityValues.at(-1) as number) - (liabilityValues[0] as number)
			: 0;
	const growth = projectedGrowth(projected, summary.data?.totalGrossAssets);
	const assetVariation = summary.data?.assetVariations.oneYear;
	const isLoadingSummary = summary.isLoading;
	const hasError = summary.isError || chart.isError || targets.isError || projections.isError;

	return (
		<div className="flex flex-col gap-5">
			<PageHeader
				title="Overview"
				description="Your net worth at a glance."
				actions={
					<Button asChild>
						<Link to="/assets?new=1">
							<Plus className="size-4" /> Add asset
						</Link>
					</Button>
				}
			/>

			{hasError ? (
				<div
					role="alert"
					className="border-border flex flex-wrap items-center justify-between gap-3 rounded-xl border p-4"
				>
					<p className="text-sm">
						Could not load your dashboard. Check your connection and try again.
					</p>
					<Button
						size="sm"
						variant="outline"
						onClick={retry}
					>
						Try again
					</Button>
				</div>
			) : (
				<>
					<div className="md:order-2">
						<NetWorthChart
							summary={summary.data}
							chart={chart.data}
							year={year}
							onYearChange={onYearChange}
							isLoading={chart.isLoading}
						/>
					</div>

					<div className="md:order-1 grid grid-cols-1 gap-5 sm:grid-cols-2 sm:gap-3 xl:grid-cols-4">
						<StatTile
							label="Projected net worth"
							value={
								projections.isLoading ? (
									<Skeleton className="h-6 w-24" />
								) : (
									money(projected ?? 0, { digits: TILE_DIGITS })
								)
							}
							hint="Based on 1 year growth rate"
							right={
								growth === undefined || growth === 0 ? undefined : (
									<Badge variant="outline">{percent(growth, GROWTH_DIGITS)}</Badge>
								)
							}
						/>
						<StatTile
							label="Asset growth"
							value={
								isLoadingSummary ? (
									<Skeleton className="h-6 w-24" />
								) : (
									money(assetVariation?.absolute ?? 0, {
										digits: TILE_DIGITS,
										signDisplay: "exceptZero"
									})
								)
							}
							hint="Gross asset change vs 1 year ago"
							right={
								assetVariation && assetVariation.percentage !== 0 ? (
									<Delta
										value={assetVariation.percentage}
										hideZero
									/>
								) : undefined
							}
						/>
						<StatTile
							label="Total liabilities"
							value={
								isLoadingSummary ? (
									<Skeleton className="h-6 w-24" />
								) : (
									money(summary.data?.totalLiabilities ?? 0, { digits: TILE_DIGITS })
								)
							}
							hint={
								summary.data
									? `Debt ratio: ${percent(summary.data.debtToAssetRatio, GROWTH_DIGITS)}`
									: undefined
							}
							right={
								<Sparkline
									values={liabilityValues}
									tone={liabilityTrend > 0 ? "danger" : "success"}
								/>
							}
						/>
						<StatTile
							label="FX impact"
							value={
								isLoadingSummary ? (
									<Skeleton className="h-6 w-24" />
								) : (
									money(summary.data?.currencyImpact ?? 0, {
										digits: TILE_DIGITS,
										signDisplay: "exceptZero"
									})
								)
							}
							hint="FX impact (1M)"
						/>
					</div>

					<div className="md:order-3 grid grid-cols-1 gap-4 lg:grid-cols-3">
						<div className="lg:order-2 lg:col-span-2">
							<TopAssetsCard
								summary={summary.data}
								isLoading={isLoadingSummary}
							/>
						</div>
						<div className="lg:order-1">
							<TargetsCard
								targets={targets.data}
								isLoading={targets.isLoading}
								currency={currency}
							/>
						</div>
					</div>
				</>
			)}
		</div>
	);
}
