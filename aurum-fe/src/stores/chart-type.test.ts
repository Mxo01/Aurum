import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { useChartTypeStore } from "./chart-type";

const NET_WORTH_KEY = "chart-type:net-worth";
const CASHFLOW_KEY = "chart-type:cashflow";
const DEFAULT_NET_WORTH = "area";
const DEFAULT_CASHFLOW = "bars";

const invalidStoredValues = [
	{
		name: "an unknown net worth value",
		netWorth: "bars",
		cashflow: null,
		expectedNetWorth: DEFAULT_NET_WORTH,
		expectedCashflow: DEFAULT_CASHFLOW
	},
	{
		name: "an unknown cashflow value",
		netWorth: null,
		cashflow: "mixed",
		expectedNetWorth: DEFAULT_NET_WORTH,
		expectedCashflow: DEFAULT_CASHFLOW
	},
	{
		name: "an empty net worth value",
		netWorth: "",
		cashflow: "",
		expectedNetWorth: DEFAULT_NET_WORTH,
		expectedCashflow: DEFAULT_CASHFLOW
	}
];

describe("chart type store", () => {
	beforeEach(() => {
		window.localStorage.clear();
		useChartTypeStore.setState({ netWorth: DEFAULT_NET_WORTH, cashflow: DEFAULT_CASHFLOW });
	});

	describe("initial state", () => {
		afterEach(() => {
			vi.unstubAllGlobals();
			vi.resetModules();
		});

		it("should fall back to the defaults without a window", async () => {
			// GIVEN
			vi.resetModules();
			vi.stubGlobal("window", undefined);

			// WHEN
			const { useChartTypeStore: testSubject } = await import("./chart-type");

			// THEN
			expect(testSubject.getState().netWorth).toBe(DEFAULT_NET_WORTH);
			expect(testSubject.getState().cashflow).toBe(DEFAULT_CASHFLOW);
		});

		it("should restore the persisted chart types", async () => {
			// GIVEN
			window.localStorage.setItem(NET_WORTH_KEY, "mixed");
			window.localStorage.setItem(CASHFLOW_KEY, "area");
			vi.resetModules();

			// WHEN
			const { useChartTypeStore: testSubject } = await import("./chart-type");

			// THEN
			expect(testSubject.getState().netWorth).toBe("mixed");
			expect(testSubject.getState().cashflow).toBe("area");
		});

		it.each(invalidStoredValues)(
			"should fall back when the store holds $name",
			async ({ netWorth, cashflow, expectedNetWorth, expectedCashflow }) => {
				// GIVEN
				if (netWorth !== null) window.localStorage.setItem(NET_WORTH_KEY, netWorth);
				if (cashflow !== null) window.localStorage.setItem(CASHFLOW_KEY, cashflow);
				vi.resetModules();

				// WHEN
				const { useChartTypeStore: testSubject } = await import("./chart-type");

				// THEN
				expect(testSubject.getState().netWorth).toBe(expectedNetWorth);
				expect(testSubject.getState().cashflow).toBe(expectedCashflow);
			}
		);
	});

	describe("setNetWorth", () => {
		it("should apply and persist the net worth chart type", () => {
			// GIVEN
			const testSubject = useChartTypeStore.getState();

			// WHEN
			testSubject.setNetWorth("mixed");

			// THEN
			expect(useChartTypeStore.getState().netWorth).toBe("mixed");
			expect(window.localStorage.getItem(NET_WORTH_KEY)).toBe("mixed");
		});
	});

	describe("setCashflow", () => {
		it("should apply and persist the cashflow chart type", () => {
			// GIVEN
			const testSubject = useChartTypeStore.getState();

			// WHEN
			testSubject.setCashflow("area");

			// THEN
			expect(useChartTypeStore.getState().cashflow).toBe("area");
			expect(window.localStorage.getItem(CASHFLOW_KEY)).toBe("area");
		});
	});
});
