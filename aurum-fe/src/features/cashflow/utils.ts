import type { CashFlowYearDTO } from "@/lib/api/types";

export const MONTH_LABELS = [
	"JAN",
	"FEB",
	"MAR",
	"APR",
	"MAY",
	"JUN",
	"JUL",
	"AUG",
	"SEP",
	"OCT",
	"NOV",
	"DEC"
];

export interface CashFlowRow {
	month: number;
	label: string;
	earned: number;
	spent: number;
	balance: number;
	deltaPercent: number | null;
	prevYearEarned: number | null;
	prevYearSpent: number | null;
	prevYearBalance: number | null;
}

export function buildCashflowRows(data: CashFlowYearDTO | undefined): CashFlowRow[] {
	if (!data) return [];

	return data.entries.map((entry, index) => {
		const previous = index > 0 ? data.entries[index - 1] : null;
		const previousBalance = previous ? previous.earned - previous.spent : null;
		const balance = entry.earned - entry.spent;
		const deltaPercent =
			previousBalance !== null && previousBalance !== 0
				? ((balance - previousBalance) / Math.abs(previousBalance)) * 100
				: null;

		const previousYear = data.previousYearEntries?.[index] ?? null;
		const prevYearBalance = previousYear ? previousYear.earned - previousYear.spent : null;

		return {
			month: entry.month,
			label: MONTH_LABELS[entry.month - 1] ?? String(entry.month),
			earned: entry.earned,
			spent: entry.spent,
			balance,
			deltaPercent,
			prevYearEarned: previousYear?.earned ?? null,
			prevYearSpent: previousYear?.spent ?? null,
			prevYearBalance
		};
	});
}

export function cashflowTotals(rows: CashFlowRow[]) {
	const earned = rows.reduce((sum, row) => sum + row.earned, 0);
	const spent = rows.reduce((sum, row) => sum + row.spent, 0);
	const balance = earned - spent;
	const savingsRate = earned > 0 ? (balance / earned) * 100 : null;

	return { earned, spent, balance, savingsRate };
}

export function lastYearWithData(data: CashFlowYearDTO | undefined) {
	return data?.previousYearEntries ? data.year - 1 : null;
}

export function hasAnyValue(rows: CashFlowRow[]) {
	return rows.some(row => row.earned > 0 || row.spent > 0);
}
