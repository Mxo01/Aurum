import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { EmptyState } from "./empty-state";

describe("EmptyState", () => {
	it("should render the title and the description", () => {
		// GIVEN
		const testSubject = EmptyState;
		const Subject = testSubject;

		// WHEN
		render(
			<Subject
				icon={<span>icon</span>}
				title="Nothing here"
				description="Add something"
			/>
		);

		// THEN
		expect(screen.getByText("Nothing here")).toBeInTheDocument();
		expect(screen.getByText("Add something")).toBeInTheDocument();
	});

	it("should render an action", () => {
		// GIVEN
		const testSubject = EmptyState;
		const Subject = testSubject;

		// WHEN
		render(
			<Subject
				icon={<span>icon</span>}
				title="Nothing here"
				action={<button type="button">Create</button>}
			/>
		);

		// THEN
		expect(screen.getByRole("button", { name: "Create" })).toBeInTheDocument();
	});
});
