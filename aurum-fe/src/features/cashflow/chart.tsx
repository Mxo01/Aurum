import { ArrowLeftRight, ChartColumn, ChartLine } from "lucide-react";
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
import { EmptyState } from "@/components/app/empty-state";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Skeleton } from "@/components/ui/skeleton";
import { usePreferences } from "@/lib/format/use-preferences";
import { usePrefersReducedMotion } from "@/lib/use-prefers-reduced-motion";
import { useChartTypeStore, type CashflowChartType } from "@/stores/chart-type";
import type { CashFlowRow } from "./utils";

const CHART_HEIGHT = 280;

const CHART_TYPES = [
	{ label: "Bars", value: "bars", icon: ChartColumn },
	{ label: "Area", value: "area", icon: ChartLine }
] as const;

interface CashflowChartProps {
	rows: CashFlowRow[];
	isLoading: boolean;
}

export function CashflowChart({ rows, isLoading }: CashflowChartProps) {
	const { money } = usePreferences();
	const reduceMotion = usePrefersReducedMotion();
	const chartType = useChartTypeStore(state => state.cashflow);
	const setChartType = useChartTypeStore(state => state.setCashflow);
	const data = rows.map(row => ({ label: row.label, earned: row.earned, spent: row.spent }));
	const isEmpty = rows.every(row => row.earned === 0 && row.spent === 0);

	return (
		<section className="bg-card border-border rounded-xl border p-4 sm:p-5">
			<div className="mb-4 flex flex-wrap items-center justify-between gap-2">
				<span className="text-muted-foreground text-[11px] font-medium">Monthly overview</span>
				<div className="flex flex-wrap items-center gap-4">
					<Tabs
						value={chartType}
						onValueChange={value => setChartType(value as CashflowChartType)}
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
					<div className="text-muted-foreground flex items-center gap-4 text-[11px]">
						<span className="flex items-center gap-1.5">
							<span
								className="bg-chart-1 inline-block size-2 rounded-full"
								aria-hidden
							/>
							Earned
						</span>
						<span className="flex items-center gap-1.5">
							<span
								className="bg-chart-2 inline-block size-2 rounded-full"
								aria-hidden
							/>
							Spent
						</span>
					</div>
				</div>
			</div>

			{isLoading ? (
				<Skeleton
					style={{ height: CHART_HEIGHT }}
					className="w-full"
				/>
			) : isEmpty ? (
				<EmptyState
					icon={<ArrowLeftRight className="size-7" />}
					title="No cash flow data yet"
					description="Edit a month in the table to start tracking."
				/>
			) : (
				<div
					style={{ height: CHART_HEIGHT }}
					className="w-full"
				>
					<ResponsiveContainer
						width="100%"
						height="100%"
					>
						<ComposedChart
							data={data}
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
								minTickGap={16}
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
									chartType === "bars"
										? { fill: "var(--muted)", opacity: 0.5 }
										: { stroke: "var(--border)" }
								}
								formatter={(value, name) => [money(Number(value), { digits: 0 }), name]}
								contentStyle={{
									background: "var(--popover)",
									border: "1px solid var(--border)",
									borderRadius: 10,
									fontSize: 12
								}}
							/>
							{chartType === "bars" ? (
								<>
									<Bar
										dataKey="earned"
										name="Earned"
										fill="var(--chart-1)"
										radius={[4, 4, 0, 0]}
										maxBarSize={18}
										isAnimationActive={!reduceMotion}
										animationDuration={700}
									/>
									<Bar
										dataKey="spent"
										name="Spent"
										fill="var(--chart-2)"
										radius={[4, 4, 0, 0]}
										maxBarSize={18}
										isAnimationActive={!reduceMotion}
										animationDuration={700}
									/>
								</>
							) : (
								<>
									<defs>
										<linearGradient
											id="cashflow-earned-fill"
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
												offset="100%"
												stopColor="var(--chart-1)"
												stopOpacity={0.02}
											/>
										</linearGradient>
										<linearGradient
											id="cashflow-spent-fill"
											x1="0"
											y1="0"
											x2="0"
											y2="1"
										>
											<stop
												offset="0%"
												stopColor="var(--chart-2)"
												stopOpacity={0.3}
											/>
											<stop
												offset="100%"
												stopColor="var(--chart-2)"
												stopOpacity={0.02}
											/>
										</linearGradient>
									</defs>
									<Area
										type="monotone"
										dataKey="earned"
										name="Earned"
										stroke="var(--chart-1)"
										strokeWidth={2}
										fill="url(#cashflow-earned-fill)"
										dot={false}
										isAnimationActive={!reduceMotion}
										animationDuration={700}
									/>
									<Area
										type="monotone"
										dataKey="spent"
										name="Spent"
										stroke="var(--chart-2)"
										strokeWidth={2}
										fill="url(#cashflow-spent-fill)"
										dot={false}
										isAnimationActive={!reduceMotion}
										animationDuration={700}
									/>
								</>
							)}
						</ComposedChart>
					</ResponsiveContainer>
				</div>
			)}
		</section>
	);
}
