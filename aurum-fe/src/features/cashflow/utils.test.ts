import { describe, expect, it } from "vitest";
import { buildCashflowYear } from "@/test/fixtures/portfolio";
import { buildCashflowRows, cashflowTotals, hasAnyValue, lastYearWithData } from "./utils";

describe("buildCashflowRows", () => {
	it("should compute balances, deltas and previous-year values", () => {
		// GIVEN
		const testSubject = buildCashflowRows;
		const mockYear = buildCashflowYear({
			year: 2026,
			entries: [
				{ id: "1", month: 1, earned: 5000, spent: 4000 },
				{ id: "2", month: 2, earned: 6000, spent: 4500 }
			],
			previousYearEntries: [
				{ id: "p1", month: 1, earned: 4000, spent: 3500 },
				{ id: "p2", month: 2, earned: 4500, spent: 4000 }
			]
		});

		// WHEN
		const expected = testSubject(mockYear);

		// THEN
		expect(expected).toHaveLength(2);
		expect(expected[0]).toMatchObject({
			label: "JAN",
			balance: 1000,
			deltaPercent: null,
			prevYearEarned: 4000,
			prevYearSpent: 3500,
			prevYearBalance: 500
		});
		expect(expected[1]?.balance).toBe(1500);
		expect(expected[1]?.deltaPercent).toBe(50);
	});

	it("should return no rows without data", () => {
		// GIVEN
		const testSubject = buildCashflowRows;

		// WHEN
		const expected = testSubject(undefined);

		// THEN
		expect(expected).toEqual([]);
	});

	it.each([
		["a month past December", 13, "13"],
		["a month below January", 0, "0"]
	])("should fall back to the raw month for %s", (_description, month, expectedLabel) => {
		// GIVEN
		const testSubject = buildCashflowRows;
		const mockYear = buildCashflowYear({
			entries: [{ id: "1", month, earned: 100, spent: 50 }],
			previousYearEntries: null
		});

		// WHEN
		const expected = testSubject(mockYear);

		// THEN
		expect(expected[0]?.label).toBe(expectedLabel);
	});

	it("should leave previous-year values empty when the year has none", () => {
		// GIVEN
		const testSubject = buildCashflowRows;
		const mockYear = buildCashflowYear({
			entries: [{ id: "1", month: 1, earned: 100, spent: 50 }],
			previousYearEntries: null
		});

		// WHEN
		const expected = testSubject(mockYear);

		// THEN
		expect(expected[0]?.prevYearEarned).toBeNull();
		expect(expected[0]?.prevYearSpent).toBeNull();
	});
});

describe("cashflowTotals", () => {
	it("should sum the rows and compute the savings rate", () => {
		// GIVEN
		const testSubject = cashflowTotals;
		const mockRows = buildCashflowRows(
			buildCashflowYear({
				entries: [
					{ id: "1", month: 1, earned: 1000, spent: 500 },
					{ id: "2", month: 2, earned: 1000, spent: 500 }
				],
				previousYearEntries: null
			})
		);

		// WHEN
		const expected = testSubject(mockRows);

		// THEN
		expect(expected).toEqual({ earned: 2000, spent: 1000, balance: 1000, savingsRate: 50 });
	});

	it("should return a null savings rate without earnings", () => {
		// GIVEN
		const testSubject = cashflowTotals;

		// WHEN
		const expected = testSubject([]);

		// THEN
		expect(expected.savingsRate).toBeNull();
	});
});

describe("lastYearWithData", () => {
	it("should return the previous year when entries exist", () => {
		// GIVEN
		const testSubject = lastYearWithData;

		// WHEN
		const expected = testSubject(buildCashflowYear({ year: 2026 }));

		// THEN
		expect(expected).toBe(2025);
	});

	it("should return null without previous entries", () => {
		// GIVEN
		const testSubject = lastYearWithData;

		// WHEN
		const expected = testSubject(buildCashflowYear({ previousYearEntries: null }));

		// THEN
		expect(expected).toBeNull();
	});
});

describe("hasAnyValue", () => {
	it("should detect at least one non-zero row", () => {
		// GIVEN
		const testSubject = hasAnyValue;
		const mockRows = buildCashflowRows(
			buildCashflowYear({
				entries: [
					{ id: "1", month: 1, earned: 0, spent: 0 },
					{ id: "2", month: 2, earned: 10, spent: 0 }
				]
			})
		);

		// WHEN
		const expected = testSubject(mockRows);

		// THEN
		expect(expected).toBe(true);
	});

	it("should return false for an empty year", () => {
		// GIVEN
		const testSubject = hasAnyValue;
		const mockRows = buildCashflowRows(
			buildCashflowYear({ entries: [{ id: "1", month: 1, earned: 0, spent: 0 }] })
		);

		// WHEN
		const expected = testSubject(mockRows);

		// THEN
		expect(expected).toBe(false);
	});
});
