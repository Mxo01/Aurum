import { LineChart as LineChartIcon, Plus, Trash2, TriangleAlert } from "lucide-react";
import { useState } from "react";
import {
	Area,
	AreaChart,
	CartesianGrid,
	ResponsiveContainer,
	Tooltip,
	XAxis,
	YAxis
} from "recharts";
import { EmptyState } from "@/components/app/empty-state";
import {
	AlertDialog,
	AlertDialogAction,
	AlertDialogCancel,
	AlertDialogContent,
	AlertDialogDescription,
	AlertDialogFooter,
	AlertDialogHeader,
	AlertDialogTitle,
	AlertDialogTrigger
} from "@/components/ui/alert-dialog";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { DatePicker } from "@/components/ui/date-picker";
import { Input } from "@/components/ui/input";
import {
	Sheet,
	SheetContent,
	SheetDescription,
	SheetHeader,
	SheetTitle
} from "@/components/ui/sheet";
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import type { AssetDTO } from "@/lib/api/types";
import { todayISO } from "@/lib/format/date";
import { formatCurrency } from "@/lib/format/money";
import { usePreferences } from "@/lib/format/use-preferences";
import { usePrefersReducedMotion } from "@/lib/use-prefers-reduced-motion";
import { useCreateSnapshot, useDeleteSnapshots, useSnapshots } from "./api";

const CHART_HEIGHT = 240;

interface SnapshotsSheetProps {
	open: boolean;
	onOpenChange: (open: boolean) => void;
	asset: AssetDTO | null;
}

export function SnapshotsSheet({ open, onOpenChange, asset }: SnapshotsSheetProps) {
	return (
		<Sheet
			open={open}
			onOpenChange={onOpenChange}
		>
			<SheetContent className="sm:max-w-lg">
				{asset ? <SnapshotsBody asset={asset} /> : null}
			</SheetContent>
		</Sheet>
	);
}

function SnapshotsBody({ asset }: { asset: AssetDTO }) {
	const { money, date, currency, locale, isPrivacy } = usePreferences();
	const reduceMotion = usePrefersReducedMotion();
	const snapshots = useSnapshots(asset.id);
	const createSnapshot = useCreateSnapshot();
	const deleteSnapshots = useDeleteSnapshots();

	const [selected, setSelected] = useState<Set<string>>(new Set());
	const [newDate, setNewDate] = useState(todayISO());
	const [newValue, setNewValue] = useState("");

	const isAutomatic = asset.liabilityType === "AUTOMATIC";
	const ordered = [...(snapshots.data ?? [])].sort((a, b) =>
		b.referenceDate.localeCompare(a.referenceDate)
	);
	const chartData = [...ordered].reverse().map(snapshot => ({
		date: snapshot.referenceDate,
		value: snapshot.amountBaseCurrency ?? 0
	}));

	const toggle = (id: string) => {
		setSelected(current => {
			const next = new Set(current);
			if (next.has(id)) next.delete(id);
			else next.add(id);
			return next;
		});
	};

	const addSnapshot = () => {
		const parsed = Number(newValue.replace(",", "."));
		if (!Number.isFinite(parsed) || parsed < 0) return;

		createSnapshot.mutate(
			{
				assetId: asset.id,
				referenceDate: newDate,
				amountOriginalCurrency: parsed
			},
			{ onSuccess: () => setNewValue("") }
		);
	};

	return (
		<>
			<SheetHeader>
				<SheetTitle className="truncate">History · {asset.name}</SheetTitle>
				<SheetDescription>
					{asset.originalCurrency} values, converted to {currency} for the chart.
				</SheetDescription>
			</SheetHeader>

			{isAutomatic ? (
				<div className="border-border bg-muted/40 flex gap-2 rounded-lg border p-3">
					<TriangleAlert className="text-primary mt-0.5 size-4 shrink-0" />
					<p className="text-muted-foreground text-xs leading-relaxed">
						This liability uses automatic payments. Snapshots are generated from the payment
						schedule and cannot be added or removed manually.
					</p>
				</div>
			) : null}

			<Tabs
				defaultValue="table"
				className="flex flex-1 flex-col overflow-hidden"
			>
				<TabsList className="self-start">
					<TabsTrigger value="table">Table</TabsTrigger>
					<TabsTrigger value="chart">Chart</TabsTrigger>
				</TabsList>

				<TabsContent
					value="table"
					className="flex flex-1 flex-col overflow-hidden"
				>
					{snapshots.isLoading ? (
						<div className="flex flex-col gap-2">
							<Skeleton className="h-9 w-full" />
							<Skeleton className="h-9 w-full" />
							<Skeleton className="h-9 w-full" />
						</div>
					) : ordered.length === 0 ? (
						<EmptyState
							icon={<LineChartIcon className="size-7" />}
							title="No snapshots yet"
							description="Record the current value to start the history."
						/>
					) : (
						<div className="flex-1 overflow-y-auto overscroll-contain px-2">
							<table className="w-full text-sm">
								<thead className="text-muted-foreground sticky top-0 bg-card text-[11px]">
									<tr>
										{!isAutomatic ? <th className="w-8 py-2" /> : null}
										<th className="py-2 text-left font-medium">Date</th>
										<th className="py-2 text-right font-medium">Amount</th>
										<th className="py-2 text-right font-medium">Value</th>
									</tr>
								</thead>
								<tbody>
									{ordered.map(snapshot => (
										<tr
											key={snapshot.id}
											className="border-border/60 border-t"
										>
											{!isAutomatic ? (
												<td className="py-2">
													<Checkbox
														checked={selected.has(snapshot.id ?? "")}
														onCheckedChange={() => snapshot.id && toggle(snapshot.id)}
														aria-label={`Select ${snapshot.referenceDate}`}
													/>
												</td>
											) : null}
											<td className="py-2">{date(snapshot.referenceDate)}</td>
											<td className="num py-2 text-right">
												{formatCurrency(snapshot.amountOriginalCurrency, {
													currency: asset.originalCurrency,
													locale,
													privacy: isPrivacy,
													digits: 2
												})}
											</td>
											<td className="num py-2 text-right font-medium">
												{money(snapshot.amountBaseCurrency ?? 0, { digits: 2 })}
											</td>
										</tr>
									))}
								</tbody>
							</table>
						</div>
					)}

					{!isAutomatic ? (
						<div className="border-border mt-3 flex flex-col gap-2 border-t pt-3">
							<div className="grid grid-cols-[1fr_1fr_auto] items-end gap-2 px-1">
								<div className="flex flex-col gap-1">
									<span className="text-muted-foreground text-[11px]">Date</span>
									<DatePicker
										id="snapshot-date"
										value={newDate}
										onChange={setNewDate}
										label="Snapshot date"
										toYear={new Date().getFullYear()}
									/>
								</div>
								<div className="flex flex-col gap-1">
									<span className="text-muted-foreground text-[11px]">
										Value ({asset.originalCurrency})
									</span>
									<Input
										type="number"
										step="0.01"
										min={0}
										value={newValue}
										onChange={event => setNewValue(event.target.value)}
										aria-label="Snapshot value"
									/>
								</div>
								<Button
									onClick={addSnapshot}
									disabled={!newValue || createSnapshot.isPending}
								>
									<Plus className="size-4" /> Add
								</Button>
							</div>

							{selected.size > 0 ? (
								<AlertDialog>
									<AlertDialogTrigger asChild>
										<Button
											variant="outline"
											size="sm"
											className="text-danger self-start"
										>
											<Trash2 className="size-4" /> Delete selected ({selected.size})
										</Button>
									</AlertDialogTrigger>
									<AlertDialogContent>
										<AlertDialogHeader>
											<AlertDialogTitle>Delete selected snapshots?</AlertDialogTitle>
											<AlertDialogDescription>
												{selected.size} snapshot(s) will be removed. The net worth history will
												change accordingly.
											</AlertDialogDescription>
										</AlertDialogHeader>
										<AlertDialogFooter>
											<AlertDialogCancel>Cancel</AlertDialogCancel>
											<AlertDialogAction
												onClick={() => {
													deleteSnapshots.mutate(
														{ assetId: asset.id, ids: Array.from(selected) },
														{ onSuccess: () => setSelected(new Set()) }
													);
												}}
											>
												Delete
											</AlertDialogAction>
										</AlertDialogFooter>
									</AlertDialogContent>
								</AlertDialog>
							) : null}
						</div>
					) : null}
				</TabsContent>

				<TabsContent
					value="chart"
					className="flex-1"
				>
					{chartData.length < 2 ? (
						<EmptyState
							icon={<LineChartIcon className="size-7" />}
							title="Not enough data"
							description="Add at least two snapshots to see the chart."
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
								<AreaChart
									data={chartData}
									margin={{ top: 8, right: 4, left: 0, bottom: 0 }}
								>
									<defs>
										<linearGradient
											id="snapshot-area"
											x1="0"
											y1="0"
											x2="0"
											y2="1"
										>
											<stop
												offset="0%"
												stopColor="var(--chart-1)"
												stopOpacity={0.32}
											/>
											<stop
												offset="100%"
												stopColor="var(--chart-1)"
												stopOpacity={0}
											/>
										</linearGradient>
									</defs>
									<CartesianGrid
										vertical={false}
										stroke="var(--chart-grid)"
									/>
									<XAxis
										dataKey="date"
										tickLine={false}
										axisLine={false}
										tick={{ fill: "var(--chart-text)", fontSize: 11 }}
										tickFormatter={(value: string) =>
											new Intl.DateTimeFormat(locale, {
												month: "short",
												year: "2-digit"
											}).format(new Date(`${value}T00:00:00`))
										}
									/>
									<YAxis
										width={54}
										tickLine={false}
										axisLine={false}
										tick={{ fill: "var(--chart-text)", fontSize: 11 }}
										tickFormatter={(value: number) => money(value, { compact: true, digits: 0 })}
									/>
									<Tooltip
										formatter={(value, name) => [money(Number(value), { digits: 0 }), name]}
										labelFormatter={label => date(String(label))}
										contentStyle={{
											background: "var(--popover)",
											border: "1px solid var(--border)",
											borderRadius: 10,
											fontSize: 12
										}}
									/>
									<Area
										type="monotone"
										dataKey="value"
										name="Value"
										stroke="var(--chart-1)"
										strokeWidth={2}
										fill="url(#snapshot-area)"
										dot={false}
										isAnimationActive={!reduceMotion}
										animationDuration={700}
									/>
								</AreaChart>
							</ResponsiveContainer>
						</div>
					)}
				</TabsContent>
			</Tabs>
		</>
	);
}
