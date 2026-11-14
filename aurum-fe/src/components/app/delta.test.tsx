import { screen } from "@testing-library/react";
import { createElement } from "react";
import { describe, expect, it } from "vitest";
import { renderWithProviders } from "@/test/render";
import { Delta } from "./delta";

describe("Delta", () => {
	it("should render nothing without a value", () => {
		// GIVEN
		const testSubject = Delta;

		// WHEN
		const { container } = renderWithProviders(createElement(testSubject, { value: null }));

		// THEN
		expect(container).toBeEmptyDOMElement();
	});

	it("should hide a zero change when asked", () => {
		// GIVEN
		const testSubject = Delta;

		// WHEN
		const { container } = renderWithProviders(
			createElement(testSubject, { value: 0, hideZero: true })
		);

		// THEN
		expect(container).toBeEmptyDOMElement();
	});

	it("should render a positive change", async () => {
		// GIVEN
		const testSubject = Delta;

		// WHEN
		renderWithProviders(createElement(testSubject, { value: 12.5 }));

		// THEN
		expect(await screen.findByText("12.50%")).toHaveClass("text-success");
	});

	it("should render a negative change", async () => {
		// GIVEN
		const testSubject = Delta;

		// WHEN
		renderWithProviders(createElement(testSubject, { value: -8, digits: 0 }));

		// THEN
		expect(await screen.findByText("8%")).toHaveClass("text-danger");
	});

	it("should flip the tone when inverted", async () => {
		// GIVEN
		const testSubject = Delta;

		// WHEN
		renderWithProviders(createElement(testSubject, { value: -8, invert: true }));

		// THEN
		expect(await screen.findByText("8.00%")).toHaveClass("text-success");
	});

	it("should hide the icon when asked", async () => {
		// GIVEN
		const testSubject = Delta;

		// WHEN
		const { container } = renderWithProviders(
			createElement(testSubject, { value: 5, showIcon: false })
		);

		// THEN
		await screen.findByText("5.00%");
		expect(container.querySelector("svg")).toBeNull();
	});
});
