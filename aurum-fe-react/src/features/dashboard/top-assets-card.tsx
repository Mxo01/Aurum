import { Wallet } from "lucide-react";
import { Link } from "react-router";
import { Delta } from "@/components/app/delta";
import { EmptyState } from "@/components/app/empty-state";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { usePreferences } from "@/lib/format/use-preferences";
import { assetTrend } from "./chart-utils";
import type { AnalyticsSummaryDTO } from "@/lib/api/types";
import { ASSET_TYPE } from "@/lib/api/types";

const TOP_ASSETS_LIMIT = 5;
const ALLOCATION_DIGITS = 1;
const NO_DATA_LABEL = "No data";
const ALLOCATION_COLORS = [
	"var(--chart-1)",
	"var(--chart-2)",
	"var(--chart-3)",
	"var(--chart-4)",
	"var(--chart-5)"
];

interface TopAssetsCardProps {
	summary: AnalyticsSummaryDTO | undefined;
	isLoading: boolean;
}

export function TopAssetsCard({ summary, isLoading }: TopAssetsCardProps) {
	const { money, percent } = usePreferences();
	const topAssets = (summary?.topAssets ?? []).slice(0, TOP_ASSETS_LIMIT);
	const allocation = Object.entries(summary?.assetAllocation ?? {});

	return (
		<section className="bg-card border-border flex flex-col rounded-xl border p-4 sm:p-5">
			<div className="flex items-center justify-between gap-2">
				<span className="text-muted-foreground flex items-center gap-2 text-[11px] font-medium">
					<Wallet className="size-3.5" /> Top assets
				</span>
				<Button variant="ghost" size="sm" asChild>
					<Link to="/assets">View all</Link>
				</Button>
			</div>

			{isLoading ? (
				<div className="mt-4 flex flex-col gap-2">
					<Skeleton className="h-4 w-24" />
					<Skeleton className="h-9 w-full" />
					<Skeleton className="h-9 w-full" />
					<Skeleton className="h-9 w-full" />
				</div>
			) : topAssets.length === 0 ? (
				<EmptyState
					className="mt-4"
					icon={<Wallet className="size-7" />}
					title="No assets yet"
					description="Add an asset to see it here."
					action={
						<Button size="sm" variant="outline" asChild>
							<Link to="/assets?new=1">Add asset</Link>
						</Button>
					}
				/>
			) : (
				<>
					<table className="mt-3 w-full text-sm">
						<thead>
							<tr className="text-muted-foreground text-[11px]">
								<th scope="col" className="pb-1.5 text-left font-medium">
									Asset
								</th>
								<th scope="col" className="pb-1.5 text-right font-medium">
									Value
								</th>
								<th scope="col" className="pb-1.5 text-right font-medium">
									1M
								</th>
							</tr>
						</thead>
						<tbody>
							{topAssets.map(asset => {
								const trend = assetTrend(asset);
								const isLiability = asset.type === ASSET_TYPE.LIABILITY;

								return (
									<tr key={asset.id ?? asset.name} className="border-border/60 border-t">
										<td className="py-2 pr-2">
											<span className="block max-w-[180px] truncate font-medium">
												{asset.name}
											</span>
											{isLiability ? (
												<span className="text-muted-foreground text-[11px]">
													Liability
												</span>
											) : null}
										</td>
										<td className="num py-2 text-right">
											{money(asset.latestValueBase ?? asset.latestValue ?? 0, { digits: 0 })}
										</td>
										<td className="py-2 pl-2 text-right">
											{trend === null ? (
												<span className="text-muted-foreground text-[11px]">
													{NO_DATA_LABEL}
												</span>
											) : (
												<Delta value={trend} invert={isLiability} />
											)}
										</td>
									</tr>
								);
							})}
						</tbody>
					</table>

					{allocation.length > 0 ? (
						<div className="mt-5 flex flex-col gap-2">
							<span className="text-muted-foreground text-[11px] font-medium">Allocation</span>
							<div
								className="flex h-2 overflow-hidden rounded-full"
								role="img"
								aria-label="Asset allocation"
							>
								{allocation.map(([name, percentage], index) => (
									<span
										key={name}
										className="h-full"
										style={{
											width: `${percentage}%`,
											background: ALLOCATION_COLORS[index % ALLOCATION_COLORS.length]
										}}
									/>
								))}
							</div>
							<dl className="flex flex-col gap-1">
								{allocation.map(([name, percentage], index) => (
									<div
										key={name}
										className="flex items-center justify-between gap-2 text-[11px]"
									>
										<dt className="text-muted-foreground flex items-center gap-1.5">
											<span
												className="inline-block size-2 rounded-full"
												style={{
													background: ALLOCATION_COLORS[index % ALLOCATION_COLORS.length]
												}}
												aria-hidden
											/>
											{name}
										</dt>
										<dd className="num">{percent(percentage, ALLOCATION_DIGITS)}</dd>
									</div>
								))}
							</dl>
						</div>
					) : null}
				</>
			)}
		</section>
	);
}
