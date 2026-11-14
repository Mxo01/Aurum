import { create } from "zustand";

export type NetWorthChartType = "area" | "mixed";
export type CashflowChartType = "area" | "bars";

const NET_WORTH_KEY = "chart-type:net-worth";
const CASHFLOW_KEY = "chart-type:cashflow";

function readType<T extends string>(key: string, allowed: readonly T[], fallback: T): T {
	if (typeof window === "undefined") return fallback;
	const stored = window.localStorage.getItem(key);
	return allowed.includes(stored as T) ? (stored as T) : fallback;
}

interface ChartTypeState {
	netWorth: NetWorthChartType;
	cashflow: CashflowChartType;
	setNetWorth: (type: NetWorthChartType) => void;
	setCashflow: (type: CashflowChartType) => void;
}

export const useChartTypeStore = create<ChartTypeState>(set => ({
	netWorth: readType<NetWorthChartType>(NET_WORTH_KEY, ["area", "mixed"], "area"),
	cashflow: readType<CashflowChartType>(CASHFLOW_KEY, ["area", "bars"], "bars"),
	setNetWorth: type => {
		window.localStorage.setItem(NET_WORTH_KEY, type);
		set({ netWorth: type });
	},
	setCashflow: type => {
		window.localStorage.setItem(CASHFLOW_KEY, type);
		set({ cashflow: type });
	}
}));
