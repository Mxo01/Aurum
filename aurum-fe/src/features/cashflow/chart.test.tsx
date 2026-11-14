import { fireEvent, screen } from "@testing-library/react";
import { cloneElement, createElement, type ReactElement } from "react";
import type * as rechartsTypes from "recharts";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { CashFlowRow } from "./utils";
import { buildCashflowYear } from "@/test/fixtures/portfolio";
import { renderWithProviders } from "@/test/render";
import { useChartTypeStore } from "@/stores/chart-type";
import { CashflowChart } from "./chart";
import { buildCashflowRows } from "./utils";

vi.mock("recharts", async importOriginal => {
	const actual = await importOriginal<typeof rechartsTypes>();
	return {
		...actual,
		ResponsiveContainer: ({
			children
		}: {
			children: ReactElement<{ width?: number; height?: number }>;
		}) => cloneElement(children, { width: 600, height: 280 })
	};
});

const CHART_STORAGE_KEY = "chart-type:cashflow";
const MOCK_ROWS: CashFlowRow[] = buildCashflowRows(buildCashflowYear());

function renderChart(props: Partial<{ rows: CashFlowRow[]; isLoading: boolean }> = {}) {
	return renderWithProviders(
		createElement(CashflowChart, { rows: MOCK_ROWS, isLoading: false, ...props })
	);
}

describe("CashflowChart", () => {
	beforeEach(() => {
		window.localStorage.clear();
		useChartTypeStore.setState({ cashflow: "bars" });
	});

	it("should render the bars chart by default", () => {
		// GIVEN
		const testSubject = CashflowChart;

		// WHEN
		renderChart();

		// THEN
		expect(screen.getByRole("tab", { name: "Bars chart" })).toHaveAttribute(
			"aria-selected",
			"true"
		);
		expect(screen.getByRole("tab", { name: "Area chart" })).toHaveAttribute(
			"aria-selected",
			"false"
		);
		expect(screen.getByText("Monthly overview")).toBeInTheDocument();
		expect(screen.getByText("Earned")).toBeInTheDocument();
		expect(screen.getByText("Spent")).toBeInTheDocument();
		expect(testSubject).toBeDefined();
	});

	it("should switch to the area chart and persist the choice", () => {
		// GIVEN
		renderChart();

		// WHEN
		fireEvent.mouseDown(screen.getByRole("tab", { name: "Area chart" }));

		// THEN
		expect(screen.getByRole("tab", { name: "Area chart" })).toHaveAttribute(
			"aria-selected",
			"true"
		);
		expect(window.localStorage.getItem(CHART_STORAGE_KEY)).toBe("area");
		expect(useChartTypeStore.getState().cashflow).toBe("area");
		expect(document.querySelector("#cashflow-earned-fill")).not.toBeNull();
		expect(document.querySelector("#cashflow-spent-fill")).not.toBeNull();
	});

	it("should start from the stored chart type", () => {
		// GIVEN
		useChartTypeStore.setState({ cashflow: "area" });

		// WHEN
		renderChart();

		// THEN
		expect(screen.getByRole("tab", { name: "Area chart" })).toHaveAttribute(
			"aria-selected",
			"true"
		);
	});

	it("should show a skeleton while loading", () => {
		// GIVEN
		const { container } = renderChart({ isLoading: true });

		// WHEN / THEN
		expect(container.querySelectorAll(".animate-pulse").length).toBeGreaterThan(0);
		expect(screen.queryByText("No cash flow data yet")).not.toBeInTheDocument();
	});

	it("should show the empty state when every month is empty", () => {
		// GIVEN
		const emptyRows = buildCashflowRows(
			buildCashflowYear({
				entries: [{ id: "1", month: 1, earned: 0, spent: 0 }],
				previousYearEntries: null
			})
		);

		// WHEN
		renderChart({ rows: emptyRows });

		// THEN
		expect(screen.getByText("No cash flow data yet")).toBeInTheDocument();
		expect(screen.getByText("Edit a month in the table to start tracking.")).toBeInTheDocument();
	});
});

it("should format the tooltip values", async () => {
	// GIVEN
	renderChart();

	// WHEN
	const surface = document.querySelector(".recharts-wrapper");
	fireEvent.mouseMove(surface as Element, { clientX: 200, clientY: 100 });

	// THEN
	expect(await screen.findByText("Earned")).toBeInTheDocument();
});
