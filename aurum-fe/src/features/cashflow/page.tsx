import { ChevronLeft, ChevronRight, TriangleAlert } from "lucide-react";
import type { ReactNode } from "react";
import { useSearchParams } from "react-router";
import { Delta } from "@/components/app/delta";
import { EditableValue } from "@/components/app/editable-value";
import { PageHeader } from "@/components/app/page-header";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { Skeleton } from "@/components/ui/skeleton";
import { usePreferences } from "@/lib/format/use-preferences";
import type { CashFlowRow } from "./utils";
import { buildCashflowRows, cashflowTotals } from "./utils";
import { useCashflowYear, useUpdateCashflowEntry } from "./api";
import { CashflowChart } from "./chart";

function parseYear(value: string | null, fallback: number) {
	if (!value) return fallback;
	const parsed = Number(value);
	return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback;
}

export function CashFlowPage() {
	const [searchParams, setSearchParams] = useSearchParams();
	const { money, percent, currency } = usePreferences();
	const currentYear = new Date().getFullYear();
	const year = parseYear(searchParams.get("year"), currentYear);

	const query = useCashflowYear(year);
	const updateEntry = useUpdateCashflowEntry();

	const rows = buildCashflowRows(query.data);
	const totals = cashflowTotals(rows);
	const previousYear = query.data?.previousYearEntries ?? null;

	const prevYearEarned = previousYear?.reduce((sum, entry) => sum + entry.earned, 0);
	const prevYearSpent = previousYear?.reduce((sum, entry) => sum + entry.spent, 0);
	const yoyEarned =
		prevYearEarned && prevYearEarned > 0
			? ((totals.earned - prevYearEarned) / prevYearEarned) * 100
			: undefined;
	const yoySpent =
		prevYearSpent && prevYearSpent > 0
			? ((totals.spent - prevYearSpent) / prevYearSpent) * 100
			: undefined;

	const setYear = (next: number) => {
		const params = new URLSearchParams(searchParams);
		if (next === currentYear) params.delete("year");
		else params.set("year", String(next));
		setSearchParams(params, { replace: true });
	};

	const save = (row: CashFlowRow, field: "earned" | "spent", value: number) => {
		updateEntry.mutate({
			year,
			month: row.month,
			entry: {
				earned: field === "earned" ? value : row.earned,
				spent: field === "spent" ? value : row.spent
			}
		});
	};

	return (
		<div className="flex flex-col gap-5">
			<PageHeader
				title="Cash flow"
				description="What came in, what went out, and what is left."
				actions={
					<div className="flex items-center gap-1">
						<Button
							variant="ghost"
							size="icon-sm"
							aria-label="Previous year"
							disabled={year <= 2000}
							onClick={() => setYear(year - 1)}
						>
							<ChevronLeft className="size-4" />
						</Button>
						<span className="num w-12 text-center text-sm font-semibold">{year}</span>
						<Button
							variant="ghost"
							size="icon-sm"
							aria-label="Next year"
							disabled={year >= currentYear}
							onClick={() => setYear(year + 1)}
						>
							<ChevronRight className="size-4" />
						</Button>
					</div>
				}
			/>

			{query.isError ? (
				<div
					role="alert"
					className="border-border flex flex-wrap items-center justify-between gap-3 rounded-xl border p-4"
				>
					<p className="text-sm">Could not load the cash flow. Try again.</p>
					<Button
						size="sm"
						variant="outline"
						onClick={() => void query.refetch()}
					>
						Try again
					</Button>
				</div>
			) : (
				<>
					<div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
						<SummaryTile
							label="Earned"
							value={
								query.isLoading ? (
									<Skeleton className="h-6 w-24" />
								) : (
									money(totals.earned, { digits: 0 })
								)
							}
							hint={previousYear ? `vs ${year - 1}` : undefined}
							right={yoyEarned === undefined ? undefined : <Delta value={yoyEarned} />}
						/>
						<SummaryTile
							label="Spent"
							value={
								query.isLoading ? (
									<Skeleton className="h-6 w-24" />
								) : (
									money(totals.spent, { digits: 0 })
								)
							}
							hint={previousYear ? `vs ${year - 1}` : undefined}
							right={
								yoySpent === undefined ? undefined : (
									<Delta
										value={yoySpent}
										invert
									/>
								)
							}
						/>
						<SummaryTile
							label="Net balance"
							value={
								query.isLoading ? (
									<Skeleton className="h-6 w-24" />
								) : (
									<span
										className={
											totals.balance > 0
												? "text-success"
												: totals.balance < 0
													? "text-danger"
													: undefined
										}
									>
										{money(totals.balance, { digits: 0, signDisplay: "exceptZero" })}
									</span>
								)
							}
							hint={`${rows.filter(row => row.earned > 0 || row.spent > 0).length} months recorded`}
						/>
						<SummaryTile
							label="Savings rate"
							value={
								query.isLoading ? (
									<Skeleton className="h-6 w-24" />
								) : totals.savingsRate === null ? (
									"—"
								) : (
									percent(totals.savingsRate, 1)
								)
							}
							hint="Net balance / earned"
							footer={
								totals.savingsRate === null ? null : (
									<Progress
										value={Math.max(0, Math.min(100, totals.savingsRate))}
										label="Savings rate"
									/>
								)
							}
						/>
					</div>

					<CashflowChart
						rows={rows}
						isLoading={query.isLoading}
					/>

					<section className="bg-card border-border overflow-hidden rounded-xl border">
						<div className="hidden md:block">
							<table className="w-full text-sm">
								<thead className="text-muted-foreground border-border border-b text-[11px]">
									<tr>
										<th className="px-3 py-2 text-left font-medium">Month</th>
										<th className="px-3 py-2 text-right font-medium">Earned</th>
										<th className="px-3 py-2 text-right font-medium">Spent</th>
										<th className="px-3 py-2 text-right font-medium">Balance</th>
										<th className="px-3 py-2 text-right font-medium">vs prev</th>
										{previousYear ? (
											<>
												<th className="px-3 py-2 text-right font-medium">{year - 1} earned</th>
												<th className="px-3 py-2 text-right font-medium">{year - 1} spent</th>
											</>
										) : null}
									</tr>
								</thead>
								<tbody>
									{rows.map(row => (
										<tr
											key={row.month}
											className="border-border/60 border-b last:border-b-0"
										>
											<td className="text-muted-foreground num px-3 py-2 text-xs font-medium">
												{row.label}
											</td>
											<td className="px-3 py-2 text-right">
												<EditableValue
													value={row.earned}
													currency={currency}
													label={`${row.label} earned`}
													onSave={value => save(row, "earned", value)}
												/>
											</td>
											<td className="px-3 py-2 text-right">
												<EditableValue
													value={row.spent}
													currency={currency}
													label={`${row.label} spent`}
													onSave={value => save(row, "spent", value)}
												/>
											</td>
											<td
												className={`num px-3 py-2 text-right font-medium ${
													row.balance >= 0 ? "text-success" : "text-danger"
												}`}
											>
												{money(row.balance, { digits: 2, signDisplay: "exceptZero" })}
											</td>
											<td className="px-3 py-2 text-right">
												{row.deltaPercent === null ? (
													<span className="text-muted-foreground">—</span>
												) : (
													<Delta value={row.deltaPercent} />
												)}
											</td>
											{previousYear ? (
												<>
													<td className="num text-muted-foreground px-3 py-2 text-right">
														{row.prevYearEarned === null
															? "—"
															: money(row.prevYearEarned, { digits: 0 })}
													</td>
													<td className="num text-muted-foreground px-3 py-2 text-right">
														{row.prevYearSpent === null
															? "—"
															: money(row.prevYearSpent, { digits: 0 })}
													</td>
												</>
											) : null}
										</tr>
									))}
								</tbody>
								<tfoot className="border-border border-t-2">
									<tr className="font-semibold">
										<td className="text-muted-foreground px-3 py-2 text-[11px]">Total</td>
										<td className="num px-3 py-2 text-right">
											{money(totals.earned, { digits: 2 })}
										</td>
										<td className="num px-3 py-2 text-right">
											{money(totals.spent, { digits: 2 })}
										</td>
										<td
											className={`num px-3 py-2 text-right ${
												totals.balance >= 0 ? "text-success" : "text-danger"
											}`}
										>
											{money(totals.balance, { digits: 2, signDisplay: "exceptZero" })}
										</td>
										<td />
										{previousYear ? (
											<>
												<td className="num px-3 py-2 text-right">
													{prevYearEarned === undefined
														? "—"
														: money(prevYearEarned, { digits: 0 })}
												</td>
												<td className="num px-3 py-2 text-right">
													{prevYearSpent === undefined ? "—" : money(prevYearSpent, { digits: 0 })}
												</td>
											</>
										) : null}
									</tr>
								</tfoot>
							</table>
						</div>

						<div className="flex flex-col gap-2 p-3 md:hidden">
							{rows.map(row => (
								<div
									key={row.month}
									className="border-border rounded-lg border p-3"
								>
									<div className="mb-2 flex items-center justify-between">
										<span className="text-muted-foreground num text-[11px] font-medium">
											{row.label}
										</span>
										<div className="flex items-center gap-2">
											<span
												className={`num text-sm font-semibold ${
													row.balance >= 0 ? "text-success" : "text-danger"
												}`}
											>
												{money(row.balance, { digits: 0, signDisplay: "exceptZero" })}
											</span>
											{row.deltaPercent === null ? null : <Delta value={row.deltaPercent} />}
										</div>
									</div>
									<div className="grid grid-cols-2 gap-2">
										<div className="flex flex-col gap-1">
											<span className="text-muted-foreground text-[10px]">Earned</span>
											<EditableValue
												value={row.earned}
												currency={currency}
												label={`${row.label} earned`}
												onSave={value => save(row, "earned", value)}
												className="text-sm"
											/>
										</div>
										<div className="flex flex-col gap-1">
											<span className="text-muted-foreground text-[10px]">Spent</span>
											<EditableValue
												value={row.spent}
												currency={currency}
												label={`${row.label} spent`}
												onSave={value => save(row, "spent", value)}
												className="text-sm"
											/>
										</div>
									</div>
								</div>
							))}
						</div>
					</section>

					{!previousYear && !query.isLoading ? (
						<div className="border-border flex items-start gap-2 rounded-lg border p-3">
							<TriangleAlert className="text-muted-foreground mt-0.5 size-4 shrink-0" />
							<p className="text-muted-foreground text-xs">
								No data for {year - 1}. The year-over-year columns appear once that year has
								entries.
							</p>
						</div>
					) : null}
				</>
			)}
		</div>
	);
}

interface SummaryTileProps {
	label: string;
	value: ReactNode;
	hint?: string;
	right?: ReactNode;
	footer?: ReactNode;
}

function SummaryTile({ label, value, hint, right, footer }: SummaryTileProps) {
	return (
		<div className="bg-card border-border flex min-h-[96px] flex-col justify-between gap-2 rounded-xl border p-4">
			<div className="flex items-start justify-between gap-2">
				<span className="text-muted-foreground text-[11px] font-medium">{label}</span>
				{right}
			</div>
			<div className="flex flex-col gap-1">
				<span className="num text-xl font-semibold tracking-tight">{value}</span>
				{hint ? <span className="text-muted-foreground text-[11px]">{hint}</span> : null}
				{footer}
			</div>
		</div>
	);
}
