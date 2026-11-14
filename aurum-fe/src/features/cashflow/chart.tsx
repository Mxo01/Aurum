import { ArrowLeftRight } from "lucide-react";
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { EmptyState } from "@/components/app/empty-state";
import { Skeleton } from "@/components/ui/skeleton";
import { usePreferences } from "@/lib/format/use-preferences";
import { usePrefersReducedMotion } from "@/lib/use-prefers-reduced-motion";
import type { CashFlowRow } from "./utils";

const CHART_HEIGHT = 280;

interface CashflowChartProps {
	rows: CashFlowRow[];
	isLoading: boolean;
}

export function CashflowChart({ rows, isLoading }: CashflowChartProps) {
	const { money } = usePreferences();
	const reduceMotion = usePrefersReducedMotion();
	const data = rows.map(row => ({ label: row.label, earned: row.earned, spent: row.spent }));
	const isEmpty = rows.every(row => row.earned === 0 && row.spent === 0);

	return (
		<section className="bg-card border-border rounded-xl border p-4 sm:p-5">
			<div className="mb-4 flex flex-wrap items-center justify-between gap-2">
				<span className="text-muted-foreground text-[11px] font-medium">Monthly overview</span>
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
						<BarChart
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
								cursor={{ fill: "var(--muted)", opacity: 0.5 }}
								formatter={(value, name) => [money(Number(value), { digits: 0 }), name]}
								contentStyle={{
									background: "var(--popover)",
									border: "1px solid var(--border)",
									borderRadius: 10,
									fontSize: 12
								}}
							/>
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
						</BarChart>
					</ResponsiveContainer>
				</div>
			)}
		</section>
	);
}
