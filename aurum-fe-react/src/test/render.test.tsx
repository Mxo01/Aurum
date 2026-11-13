import { screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { renderWithProviders } from "./render";

describe("renderWithProviders", () => {
	it("should render the probe inside the providers", () => {
		// GIVEN
		const testSubject = renderWithProviders;

		// WHEN
		testSubject(<p>hello from the test</p>);

		// THEN
		expect(screen.getByText("hello from the test")).toBeInTheDocument();
	});
});
