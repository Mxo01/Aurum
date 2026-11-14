import { fireEvent, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { cloneElement, createElement, type ReactElement } from "react";
import type * as rechartsTypes from "recharts";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { buildChart, buildSummary } from "@/test/fixtures/portfolio";
import { renderWithProviders } from "@/test/render";
import { useChartTypeStore } from "@/stores/chart-type";
import { NetWorthChart } from "./net-worth-chart";

let formattedTooltip: unknown;

vi.mock("recharts", async importOriginal => {
	const actual = await importOriginal<typeof rechartsTypes>();
	return {
		...actual,
		ResponsiveContainer: ({
			children
		}: {
			children: ReactElement<{ width?: number; height?: number }>;
		}) => cloneElement(children, { width: 600, height: 300 }),
		Tooltip: ({ formatter }: { formatter?: (value: unknown, name: string) => unknown }) => {
			formattedTooltip = formatter?.(1000, "Net worth");
			return null;
		}
	};
});

const mockSummary = buildSummary();
const mockChart = buildChart();

function renderChart(props: Partial<Parameters<typeof NetWorthChart>[0]> = {}) {
	return renderWithProviders(
		createElement(NetWorthChart, {
			summary: mockSummary,
			chart: mockChart,
			year: undefined,
			onYearChange: vi.fn(),
			isLoading: false,
			...props
		})
	);
}

describe("NetWorthChart", () => {
	beforeEach(() => {
		window.localStorage.clear();
		useChartTypeStore.setState({ netWorth: "area" });
	});

	it("should render the header, the chart and the legend", () => {
		// GIVEN
		const testSubject = NetWorthChart;

		// WHEN
		renderChart();

		// THEN
		expect(screen.getAllByText("Net worth").length).toBeGreaterThan(0);
		expect(screen.getByText("Apartment Berlin")).toBeInTheDocument();
		expect(testSubject).toBeDefined();
	});

	it("should switch the period", () => {
		// GIVEN
		renderChart();

		// WHEN
		fireEvent.mouseDown(screen.getByRole("tab", { name: "6M" }));

		// THEN
		expect(screen.getByRole("tab", { name: "6M" })).toHaveAttribute("aria-selected", "true");
	});

	it("should switch to the mixed chart and persist it", () => {
		// GIVEN
		renderChart();

		// WHEN
		fireEvent.mouseDown(screen.getByRole("tab", { name: "Mixed chart" }));

		// THEN
		expect(window.localStorage.getItem("chart-type:net-worth")).toBe("mixed");
		expect(document.querySelector(".recharts-bar")).not.toBeNull();
	});

	it("should start from the stored chart type", () => {
		// GIVEN
		useChartTypeStore.setState({ netWorth: "mixed" });

		// WHEN
		renderChart();

		// THEN
		expect(screen.getByRole("tab", { name: "Mixed chart" })).toHaveAttribute(
			"aria-selected",
			"true"
		);
	});

	it("should change the year", async () => {
		// GIVEN
		const onYearChange = vi.fn();
		const user = userEvent.setup();
		renderChart({ onYearChange });

		// WHEN
		await user.click(screen.getByRole("combobox", { name: "Select year" }));
		await user.click(await screen.findByRole("option", { name: "2026" }));

		// THEN
		expect(onYearChange).toHaveBeenCalledWith(2026);
	});

	it("should go back to all time", async () => {
		// GIVEN
		const onYearChange = vi.fn();
		const user = userEvent.setup();
		renderChart({ onYearChange, year: 2026 });

		// WHEN
		await user.click(screen.getByRole("combobox", { name: "Select year" }));
		await user.click(await screen.findByRole("option", { name: "All time" }));

		// THEN
		expect(onYearChange).toHaveBeenCalledWith(undefined);
	});

	it("should hide the period tabs when a year is selected", () => {
		// GIVEN
		renderChart({ year: 2026 });

		// THEN
		expect(screen.queryByRole("tab", { name: "1M" })).not.toBeInTheDocument();
	});

	it("should show the loading skeleton", () => {
		// GIVEN
		const { container } = renderChart({ isLoading: true });

		// THEN
		expect(container.querySelector(".animate-pulse")).not.toBeNull();
	});

	it("should show the empty state without chart data", () => {
		// GIVEN
		renderChart({ chart: undefined });

		// THEN
		expect(screen.getByText("Not enough data yet")).toBeInTheDocument();
	});

	it("should mark a liability favorite in the legend", () => {
		// GIVEN
		renderChart({
			chart: buildChart({ favoriteAssetsTypes: { "Apartment Berlin": "LIABILITY" } })
		});

		// THEN
		expect(screen.getByText(/Apartment Berlin \(liability\)/)).toBeInTheDocument();
	});

	it("should show a negative variation and a negative average percent", () => {
		// GIVEN
		const summary = buildSummary({
			assetVariations: {
				oneMonth: { absolute: -500, percentage: -0.4 },
				sixMonths: { absolute: -2000, percentage: -2.8 },
				oneYear: { absolute: -21000, percentage: -7.2 }
			}
		});

		// WHEN
		renderChart({ summary });

		// THEN
		expect(screen.getByText("7.20%")).toBeInTheDocument();
		expect(screen.getByText("7.20%").closest("span")).toHaveClass("text-danger");
		expect(screen.getByText(/\(-0\.62%\)/)).toBeInTheDocument();
	});

	it("should hide a zero average percent", () => {
		// GIVEN
		const summary = buildSummary({
			assetVariations: {
				oneMonth: { absolute: 0, percentage: 0 },
				sixMonths: { absolute: 0, percentage: 0 },
				oneYear: { absolute: 0, percentage: 0 }
			}
		});

		// WHEN
		renderChart({ summary });

		// THEN
		expect(screen.queryByText(/\(\+?0\.00%\)/)).not.toBeInTheDocument();
	});
});

it("should format the tooltip", () => {
	// GIVEN
	renderChart();

	// THEN
	expect(formattedTooltip).toEqual(["€1,000", "Net worth"]);
});
