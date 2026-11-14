import { ChartLine, ChartNoAxesCombined, LineChart as LineChartIcon } from "lucide-react";
import { useState } from "react";
import {
	Area,
	Bar,
	CartesianGrid,
	ComposedChart,
	ResponsiveContainer,
	Tooltip,
	XAxis,
	YAxis
} from "recharts";
import { Delta } from "@/components/app/delta";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { EmptyState } from "@/components/app/empty-state";
import { Skeleton } from "@/components/ui/skeleton";
import {
	Select,
	SelectContent,
	SelectItem,
	SelectTrigger,
	SelectValue
} from "@/components/ui/select";
import { usePreferences } from "@/lib/format/use-preferences";
import { usePrefersReducedMotion } from "@/lib/use-prefers-reduced-motion";
import { useChartTypeStore, type NetWorthChartType } from "@/stores/chart-type";
import type { AnalyticsSummaryDTO, ChartDataDTO, VariationDTO } from "@/lib/api/types";
import { cn } from "@/lib/utils";
import {
	buildChartRows,
	hasChartData,
	monthlyAverage,
	monthlyAveragePercent,
	yearOptions
} from "./chart-utils";

const PERIODS: { label: string; value: keyof VariationDTO }[] = [
	{ label: "1M", value: "oneMonth" },
	{ label: "6M", value: "sixMonths" },
	{ label: "1Y", value: "oneYear" }
];

const FAVORITE_COLORS = ["var(--chart-2)", "var(--chart-3)", "var(--chart-4)", "var(--chart-5)"];

const CHART_TYPES = [
	{ label: "Area", value: "area", icon: ChartLine },
	{ label: "Mixed", value: "mixed", icon: ChartNoAxesCombined }
] as const;

interface NetWorthChartProps {
	summary: AnalyticsSummaryDTO | undefined;
	chart: ChartDataDTO | undefined;
	year: number | undefined;
	onYearChange: (year: number | undefined) => void;
	isLoading: boolean;
}

export function NetWorthChart({
	summary,
	chart,
	year,
	onYearChange,
	isLoading
}: NetWorthChartProps) {
	const { money } = usePreferences();
	const reduceMotion = usePrefersReducedMotion();
	const [period, setPeriod] = useState<keyof VariationDTO>("oneYear");
	const chartType = useChartTypeStore(state => state.netWorth);
	const setChartType = useChartTypeStore(state => state.setNetWorth);

	const variation = summary?.assetVariations?.[period];
	const averageAbsolute = monthlyAverage(summary?.assetVariations?.oneYear.absolute);
	const averagePercent = monthlyAveragePercent(summary?.assetVariations?.oneYear.percentage);
	const { rows, favorites } = buildChartRows(chart);
	const currentYear = new Date().getFullYear();
	const years = yearOptions(summary?.oldestSnapshotYear, currentYear);
	const hasData = hasChartData(chart);

	return (
		<section className="bg-card border-border rounded-xl border">
			<div className="flex flex-col gap-4 p-4 sm:p-5">
				<div className="flex flex-wrap items-start justify-between gap-4">
					<div className="flex flex-col gap-1">
						<span className="text-muted-foreground text-[11px] font-medium">Net worth</span>
						<div className="flex flex-wrap items-baseline gap-2">
							<span className="num text-2xl font-semibold tracking-tight sm:text-3xl">
								{money(summary?.totalNetWorth ?? 0)}
							</span>
							{variation && variation.percentage !== 0 ? (
								<span
									className={cn(
										"rounded-md px-1.5 py-0.5",
										variation.percentage >= 0 ? "bg-success/10" : "bg-danger/10"
									)}
								>
									<Delta
										value={variation.percentage}
										hideZero
									/>
								</span>
							) : null}
						</div>
						<div className="text-muted-foreground flex flex-wrap items-center gap-x-4 gap-y-1 text-[11px]">
							<span className="flex items-center gap-1.5">
								avg / month
								<span className="num text-foreground font-medium">
									{money(averageAbsolute ?? 0, { signDisplay: "exceptZero" })}
								</span>
							</span>
							{averagePercent !== undefined && averagePercent !== 0 ? (
								<span className="num">
									({averagePercent >= 0 ? "+" : ""}
									{averagePercent.toFixed(2)}%)
								</span>
							) : null}
						</div>
					</div>

					<div className="flex flex-wrap items-center gap-2">
						{!year ? (
							<Tabs
								value={period}
								onValueChange={value => setPeriod(value as keyof VariationDTO)}
							>
								<TabsList aria-label="Chart period">
									{PERIODS.map(item => (
										<TabsTrigger
											key={item.value}
											value={item.value}
											aria-controls={undefined}
										>
											{item.label}
										</TabsTrigger>
									))}
								</TabsList>
							</Tabs>
						) : null}

						<Tabs
							value={chartType}
							onValueChange={value => setChartType(value as NetWorthChartType)}
						>
							<TabsList aria-label="Chart type">
								{CHART_TYPES.map(item => (
									<TabsTrigger
										key={item.value}
										value={item.value}
										aria-controls={undefined}
										aria-label={`${item.label} chart`}
									>
										<item.icon className="size-3.5" />
										<span className="hidden sm:inline">{item.label}</span>
									</TabsTrigger>
								))}
							</TabsList>
						</Tabs>

						<Select
							value={year ? String(year) : "all"}
							onValueChange={value => onYearChange(value === "all" ? undefined : Number(value))}
						>
							<SelectTrigger
								className="h-8 w-32 text-xs"
								aria-label="Select year"
							>
								<SelectValue />
							</SelectTrigger>
							<SelectContent>
								<SelectItem value="all">All time</SelectItem>
								{years.map(item => (
									<SelectItem
										key={item}
										value={String(item)}
									>
										{item}
									</SelectItem>
								))}
							</SelectContent>
						</Select>
					</div>
				</div>

				{isLoading ? (
					<Skeleton className="h-[300px] w-full" />
				) : hasData ? (
					<div className="h-[300px] w-full">
						<ResponsiveContainer
							width="100%"
							height="100%"
						>
							<ComposedChart
								data={rows}
								margin={{ top: 8, right: 4, left: 0, bottom: 0 }}
							>
								<CartesianGrid
									vertical={false}
									stroke="var(--chart-grid)"
								/>
								<XAxis
									dataKey="label"
									tickLine={false}
									axisLine={false}
									tick={{ fill: "var(--chart-text)", fontSize: 11 }}
									interval="preserveStartEnd"
									minTickGap={28}
								/>
								<YAxis
									width={54}
									tickLine={false}
									axisLine={false}
									tick={{ fill: "var(--chart-text)", fontSize: 11 }}
									tickFormatter={(value: number) => money(value, { compact: true, digits: 0 })}
								/>
								<Tooltip
									cursor={
										chartType === "mixed"
											? { fill: "var(--muted)", opacity: 0.5 }
											: { stroke: "var(--border)" }
									}
									formatter={(value, name) => [money(Number(value), { digits: 0 }), name]}
									labelStyle={{ color: "var(--muted-foreground)", fontSize: 11 }}
									contentStyle={{
										background: "var(--popover)",
										border: "1px solid var(--border)",
										borderRadius: 10,
										fontSize: 12
									}}
								/>
								<defs>
									<linearGradient
										id="net-worth-fill"
										x1="0"
										y1="0"
										x2="0"
										y2="1"
									>
										<stop
											offset="0%"
											stopColor="var(--chart-1)"
											stopOpacity={0.35}
										/>
										<stop
											offset="55%"
											stopColor="var(--chart-1)"
											stopOpacity={0.12}
										/>
										<stop
											offset="100%"
											stopColor="var(--chart-1)"
											stopOpacity={0.02}
										/>
									</linearGradient>
									{favorites.map((favorite, index) => (
										<linearGradient
											key={favorite.key}
											id={`favorite-fill-${index}`}
											x1="0"
											y1="0"
											x2="0"
											y2="1"
										>
											<stop
												offset="0%"
												stopColor={FAVORITE_COLORS[index % FAVORITE_COLORS.length]}
												stopOpacity={0.22}
											/>
											<stop
												offset="55%"
												stopColor={FAVORITE_COLORS[index % FAVORITE_COLORS.length]}
												stopOpacity={0.07}
											/>
											<stop
												offset="100%"
												stopColor={FAVORITE_COLORS[index % FAVORITE_COLORS.length]}
												stopOpacity={0.01}
											/>
										</linearGradient>
									))}
								</defs>
								{chartType === "mixed" ? (
									<Bar
										dataKey="netWorth"
										name="Net worth"
										fill="var(--chart-1)"
										opacity={0.8}
										radius={[4, 4, 0, 0]}
										maxBarSize={28}
										isAnimationActive={!reduceMotion}
										animationDuration={700}
									/>
								) : (
									<Area
										type="monotone"
										dataKey="netWorth"
										name="Net worth"
										stroke="var(--chart-1)"
										strokeWidth={2}
										fill="url(#net-worth-fill)"
										dot={false}
										isAnimationActive={!reduceMotion}
										animationDuration={700}
									/>
								)}
								{favorites.map((favorite, index) => (
									<Area
										key={favorite.key}
										type="monotone"
										dataKey={favorite.key}
										name={favorite.name}
										stroke={FAVORITE_COLORS[index % FAVORITE_COLORS.length]}
										strokeWidth={1.6}
										fill={`url(#favorite-fill-${index})`}
										dot={false}
										isAnimationActive={!reduceMotion}
										animationDuration={700}
									/>
								))}
							</ComposedChart>
						</ResponsiveContainer>
					</div>
				) : (
					<EmptyState
						icon={<LineChartIcon className="size-8" />}
						title="Not enough data yet"
						description="Record a value for an asset to start the chart."
					/>
				)}

				{favorites.length > 0 && hasData ? (
					<div className="text-muted-foreground flex flex-wrap items-center gap-x-4 gap-y-1 text-[11px]">
						<span className="flex items-center gap-1.5">
							<span
								className="bg-chart-1 inline-block size-2 rounded-full"
								aria-hidden
							/>
							Net worth
						</span>
						{favorites.map((favorite, index) => (
							<span
								key={favorite.key}
								className="flex items-center gap-1.5"
							>
								<span
									className="inline-block size-2 rounded-full"
									style={{
										background: FAVORITE_COLORS[index % FAVORITE_COLORS.length]
									}}
									aria-hidden
								/>
								{favorite.name}
								{favorite.isLiability ? " (liability)" : ""}
							</span>
						))}
					</div>
				) : null}
			</div>
		</section>
	);
}
