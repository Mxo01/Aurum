import type {
	CashFlowEntryDTO,
	CashFlowYearDTO,
	UpdateCashFlowEntryDTO
} from "../../../shared/api/contract";

export type CashFlowEntry = CashFlowEntryDTO;
export type CashFlowYear = CashFlowYearDTO;
export type UpdateCashFlowEntry = UpdateCashFlowEntryDTO;

/** A month rendered as a table row: derived values the API never returns. */
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
