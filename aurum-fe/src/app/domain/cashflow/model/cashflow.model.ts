export interface CashFlowEntry {
	id: string | null;
	month: number;
	earned: number;
	spent: number;
}

export interface CashFlowYear {
	year: number;
	entries: CashFlowEntry[];
	previousYearEntries: CashFlowEntry[] | null;
	availableYears: number[];
}

export interface UpdateCashFlowEntry {
	earned?: number;
	spent?: number;
}

export interface CashFlowRow {
	month: number;
	monthLabel: string;
	earned: number;
	spent: number;
	balance: number;
	prevMonthBalance: number | null;
	deltaAbsolute: number | null;
	deltaPercent: number | null;
	prevYearEarned: number | null;
	prevYearSpent: number | null;
	prevYearBalance: number | null;
	prevYearDeltaAbsolute: number | null;
	prevYearDeltaPercent: number | null;
}

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
