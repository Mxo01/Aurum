import { screen } from "@testing-library/react";
import { createElement } from "react";
import { describe, expect, it } from "vitest";
import type { CashFlowEntryDTO } from "@/lib/api/types";
import { formatCurrency, formatPercent, type MoneyOptions } from "@/lib/format/money";
import { buildCashflowYear } from "@/test/fixtures/portfolio";
import { renderWithProviders } from "@/test/render";
import { CashflowSummary } from "./summary";
import { buildCashflowRows, cashflowTotals } from "./utils";

const CURRENCY = "EUR";
const LOCALE = "en-US";
const YEAR = 2026;

function entry(month: number, earned: number, spent: number): CashFlowEntryDTO {
	return { id: `entry-${month}`, month, earned, spent };
}

function money(value: number, options: Partial<MoneyOptions> = {}) {
	return formatCurrency(value, { currency: CURRENCY, locale: LOCALE, ...options });
}

function percent(value: number, digits = 2) {
	return formatPercent(value, LOCALE, digits);
}

function totalsFor(entries: CashFlowEntryDTO[]) {
	return cashflowTotals(
		buildCashflowRows(buildCashflowYear({ entries, previousYearEntries: null }))
	);
}

describe("CashflowSummary", () => {
	it("should render the totals, the averages and the year-over-year deltas", () => {
		// GIVEN
		const testSubject = CashflowSummary;
		const totals = totalsFor([entry(1, 1000, 400), entry(2, 1000, 600)]);
		const mockProps = {
			year: YEAR,
			totals,
			previousYearEarned: 1600,
			previousYearSpent: 800,
			activeMonths: 2,
			isLoading: false
		};

		// WHEN
		renderWithProviders(createElement(testSubject, mockProps));

		// THEN
		expect(screen.getByText(/vs 2025/)).toBeInTheDocument();
		expect(screen.getAllByText(money(totals.earned, { digits: 0 })).length).toBeGreaterThan(0);
		expect(screen.getAllByText(money(totals.earned / 2, { digits: 0 })).length).toBeGreaterThan(0);
		expect(screen.getAllByText(money(totals.balance, { digits: 0 })).length).toBeGreaterThan(0);
		expect(screen.getAllByText(percent(totals.savingsRate ?? 0, 1)).length).toBeGreaterThan(0);
		expect(screen.getAllByText(percent(25))).toHaveLength(2);
	});

	it("should show a dash when the savings rate is undefined", () => {
		// GIVEN
		const testSubject = CashflowSummary;
		const totals = totalsFor([entry(1, 0, 310)]);
		const mockProps = {
			year: YEAR,
			totals,
			activeMonths: 1,
			isLoading: false
		};

		// WHEN
		renderWithProviders(createElement(testSubject, mockProps));

		// THEN
		expect(screen.getByText("—")).toBeInTheDocument();
		expect(
			screen.getAllByText(money(totals.balance, { digits: 0, signDisplay: "exceptZero" })).length
		).toBeGreaterThan(0);
	});

	it("should clamp a negative savings rate to zero and mark the balance as danger", () => {
		// GIVEN
		const testSubject = CashflowSummary;
		const totals = totalsFor([entry(1, 100, 400)]);
		const mockProps = {
			year: YEAR,
			totals,
			activeMonths: 1,
			isLoading: false
		};

		// WHEN
		renderWithProviders(createElement(testSubject, mockProps));

		// THEN
		expect(screen.getByText(percent(0, 1))).toBeInTheDocument();
		expect(
			screen.getByText(money(totals.balance, { digits: 0, signDisplay: "exceptZero" }))
		).toBeInTheDocument();
	});

	it("should render skeletons while loading", () => {
		// GIVEN
		const testSubject = CashflowSummary;
		const totals = totalsFor([entry(1, 1000, 400)]);
		const mockProps = {
			year: YEAR,
			totals,
			activeMonths: 1,
			isLoading: true
		};

		// WHEN
		const { container } = renderWithProviders(createElement(testSubject, mockProps));

		// THEN
		expect(container.querySelectorAll(".animate-pulse").length).toBeGreaterThan(0);
		expect(screen.queryByText(money(totals.earned, { digits: 0 }))).not.toBeInTheDocument();
	});

	it("should render zero averages without active months", () => {
		// GIVEN
		const testSubject = CashflowSummary;
		const totals = totalsFor([]);
		const mockProps = {
			year: YEAR,
			totals,
			activeMonths: 0,
			isLoading: false
		};

		// WHEN
		renderWithProviders(createElement(testSubject, mockProps));

		// THEN
		expect(screen.getAllByText(money(0, { digits: 0 })).length).toBeGreaterThan(0);
	});
});
