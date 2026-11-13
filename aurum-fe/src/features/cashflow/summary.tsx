import { Activity, ArrowLeftRight, Shield, type LucideIcon } from "lucide-react";
import type { ReactNode } from "react";
import { Delta } from "@/components/app/delta";
import { Skeleton } from "@/components/ui/skeleton";
import { usePreferences } from "@/lib/format/use-preferences";
import { cn } from "@/lib/utils";
import type { cashflowTotals } from "./utils";

interface CashflowSummaryProps {
	year: number;
	totals: ReturnType<typeof cashflowTotals>;
	previousYearEarned?: number;
	previousYearSpent?: number;
	activeMonths: number;
	isLoading: boolean;
}

export function CashflowSummary({
	year,
	totals,
	previousYearEarned,
	previousYearSpent,
	activeMonths,
	isLoading
}: CashflowSummaryProps) {
	const { money, percent } = usePreferences();

	const yoyEarned =
		previousYearEarned && previousYearEarned > 0
			? ((totals.earned - previousYearEarned) / previousYearEarned) * 100
			: undefined;
	const yoySpent =
		previousYearSpent && previousYearSpent > 0
			? ((totals.spent - previousYearSpent) / previousYearSpent) * 100
			: undefined;
	const hasPreviousYear = previousYearEarned !== undefined;

	const averageEarned = activeMonths > 0 ? totals.earned / activeMonths : 0;
	const averageSpent = activeMonths > 0 ? totals.spent / activeMonths : 0;
	const savingsRate =
		totals.savingsRate === null ? null : Math.max(0, Math.min(100, totals.savingsRate));
	const spendRatio = totals.earned > 0 ? (totals.spent / totals.earned) * 100 : null;

	return (
		<section className="grid grid-cols-1 gap-3 lg:grid-cols-3">
			<SummaryCard
				icon={ArrowLeftRight}
				title="Total cashflow"
				hint={hasPreviousYear ? `vs ${year - 1}` : undefined}
				footer={
					<div className="text-muted-foreground flex items-center justify-between text-[11px]">
						<span>Spent / earned</span>
						<span className="num text-foreground">
							{spendRatio === null ? "—" : percent(spendRatio, 1)}
						</span>
					</div>
				}
			>
				<div className="grid grid-cols-2 divide-x divide-border">
					<Metric
						className="pr-4"
						label="Earned"
						value={isLoading ? null : money(totals.earned, { digits: 0 })}
						delta={yoyEarned}
					/>
					<Metric
						className="pl-4"
						label="Spent"
						value={isLoading ? null : money(totals.spent, { digits: 0 })}
						delta={yoySpent}
						invertDelta
					/>
				</div>
			</SummaryCard>

			<SummaryCard
				icon={Activity}
				title="Monthly average"
			>
				<div className="grid grid-cols-2 divide-x divide-border">
					<Metric
						className="pr-4"
						label="Earned"
						value={isLoading ? null : money(averageEarned, { digits: 0 })}
					/>
					<Metric
						className="pl-4"
						label="Spent"
						value={isLoading ? null : money(averageSpent, { digits: 0 })}
					/>
				</div>
			</SummaryCard>

			<SummaryCard
				icon={Shield}
				title="Balance & rate"
			>
				<div className="grid grid-cols-2 divide-x divide-border">
					<Metric
						className="pr-4"
						label="Net balance"
						value={
							isLoading ? null : money(totals.balance, { digits: 0, signDisplay: "exceptZero" })
						}
						tone={totals.balance >= 0 ? "success" : "danger"}
					/>
					<Metric
						className="pl-4"
						label="Savings rate"
						value={isLoading ? null : savingsRate === null ? "—" : percent(savingsRate, 1)}
					/>
				</div>
			</SummaryCard>
		</section>
	);
}

interface SummaryCardProps {
	icon: LucideIcon;
	title: string;
	hint?: string;
	footer?: ReactNode;
	children: ReactNode;
}

function SummaryCard({ icon: Icon, title, hint, footer, children }: SummaryCardProps) {
	return (
		<div className="bg-card border-border flex h-full flex-col rounded-xl border p-4">
			<div className="flex items-center gap-2">
				<span className="bg-muted text-muted-foreground flex size-7 items-center justify-center rounded-lg">
					<Icon
						className="size-3.5"
						aria-hidden
					/>
				</span>
				<span className="text-[11px] font-semibold">{title}</span>
				{hint ? <span className="text-muted-foreground text-[10px]">({hint})</span> : null}
			</div>

			<div className="mt-4 flex-1">{children}</div>
			{footer ? <div className="mt-3">{footer}</div> : null}
		</div>
	);
}

interface MetricProps {
	label: string;
	className?: string;
	value: string | null;
	delta?: number;
	invertDelta?: boolean;
	tone?: "success" | "danger";
}

function Metric({ label, className, value, delta, invertDelta, tone }: MetricProps) {
	return (
		<div className={cn("flex min-w-0 flex-col gap-1", className)}>
			<span className="text-muted-foreground text-[11px]">{label}</span>
			{value === null ? (
				<Skeleton className="h-6 w-20" />
			) : (
				<span
					className={cn(
						"num text-lg font-semibold tracking-tight",
						tone === "success" && "text-success",
						tone === "danger" && "text-danger"
					)}
				>
					{value}
				</span>
			)}
			{delta === undefined ? null : (
				<Delta
					value={delta}
					invert={invertDelta}
				/>
			)}
		</div>
	);
}
