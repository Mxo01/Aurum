import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { Sparkline, StatTile } from "./stat-tile";

describe("Sparkline", () => {
	it("should render nothing with fewer than two values", () => {
		// GIVEN
		const testSubject = Sparkline;
		const Subject = testSubject;

		// WHEN
		const { container } = render(<Subject values={[1]} />);

		// THEN
		expect(container).toBeEmptyDOMElement();
	});

	it("should render a flat series", () => {
		// GIVEN
		const testSubject = Sparkline;
		const Subject = testSubject;

		// WHEN
		const { container } = render(<Subject values={[5, 5]} />);

		// THEN
		expect(container.querySelector("svg")).not.toBeNull();
	});

	it("should use the success tone", () => {
		// GIVEN
		const testSubject = Sparkline;
		const Subject = testSubject;

		// WHEN
		const { container } = render(
			<Subject
				values={[1, 2]}
				tone="success"
			/>
		);

		// THEN
		expect(container.querySelector("svg")).toHaveClass("text-success");
	});

	it("should use the danger tone", () => {
		// GIVEN
		const testSubject = Sparkline;
		const Subject = testSubject;

		// WHEN
		const { container } = render(
			<Subject
				values={[2, 1]}
				tone="danger"
			/>
		);

		// THEN
		expect(container.querySelector("svg")).toHaveClass("text-danger");
	});

	it("should stay neutral without a tone", () => {
		// GIVEN
		const testSubject = Sparkline;
		const Subject = testSubject;

		// WHEN
		const { container } = render(<Subject values={[1, 2]} />);

		// THEN
		expect(container.querySelector("svg")).toHaveClass("text-muted-foreground/60");
	});
});

describe("StatTile", () => {
	it("should render the hint, the right slot and the class name", () => {
		// GIVEN
		const testSubject = StatTile;
		const Subject = testSubject;

		// WHEN
		render(
			<Subject
				label="Net worth"
				value="€1"
				hint="a hint"
				right={<span>right</span>}
				className="extra"
			/>
		);

		// THEN
		expect(screen.getByText("Net worth")).toBeInTheDocument();
		expect(screen.getByText("a hint")).toBeInTheDocument();
		expect(screen.getByText("right")).toBeInTheDocument();
	});

	it("should render a full-height chart in half of the card", () => {
		// GIVEN
		const testSubject = StatTile;
		const Subject = testSubject;

		// WHEN
		render(
			<Subject
				label="Total liabilities"
				value="€15"
				chart={<span data-testid="chart">chart</span>}
			/>
		);

		// THEN
		const chart = screen.getByTestId("chart");
		expect(chart).toBeInTheDocument();
		expect(chart.parentElement).toHaveClass("w-1/2", "items-stretch");
	});

	it("should render without the hint and the right slot", () => {
		// GIVEN
		const testSubject = StatTile;
		const Subject = testSubject;

		// WHEN
		render(
			<Subject
				label="Net worth"
				value="€1"
			/>
		);

		// THEN
		expect(screen.getByText("Net worth")).toBeInTheDocument();
		expect(screen.queryByText("a hint")).not.toBeInTheDocument();
	});
});
