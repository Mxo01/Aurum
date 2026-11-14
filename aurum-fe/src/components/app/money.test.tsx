import { screen } from "@testing-library/react";
import { createElement } from "react";
import { describe, expect, it } from "vitest";
import { renderWithProviders } from "@/test/render";
import { Money } from "./money";

describe("Money", () => {
	it("should render the formatted amount", async () => {
		// GIVEN
		const testSubject = Money;

		// WHEN
		renderWithProviders(createElement(testSubject, { value: 1234.5 }));

		// THEN
		expect((await screen.findAllByText("€1,234.50")).length).toBeGreaterThan(0);
	});

	it("should pass the formatting options through", async () => {
		// GIVEN
		const testSubject = Money;

		// WHEN
		renderWithProviders(createElement(testSubject, { value: 1234.5, digits: 0, compact: true }));

		// THEN
		expect((await screen.findAllByText("€1.2K")).length).toBeGreaterThan(0);
	});
});
