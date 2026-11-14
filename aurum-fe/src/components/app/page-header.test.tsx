import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { PageHeader } from "./page-header";

describe("PageHeader", () => {
	it("should render the title, the description and the actions", () => {
		// GIVEN
		const testSubject = PageHeader;
		const Subject = testSubject;

		// WHEN
		render(
			<Subject
				title="Assets"
				description="Everything you own"
				actions={<button type="button">Add</button>}
			/>
		);

		// THEN
		expect(screen.getByRole("heading", { name: "Assets" })).toBeInTheDocument();
		expect(screen.getByText("Everything you own")).toBeInTheDocument();
		expect(screen.getByRole("button", { name: "Add" })).toBeInTheDocument();
	});

	it("should render just the title", () => {
		// GIVEN
		const testSubject = PageHeader;
		const Subject = testSubject;

		// WHEN
		render(<Subject title="Assets" />);

		// THEN
		expect(screen.getByRole("heading", { name: "Assets" })).toBeInTheDocument();
	});
});
