import { render } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { CategoryIcon } from "./icon-map";

describe("CategoryIcon", () => {
	it("should render a known icon", () => {
		// GIVEN
		const testSubject = CategoryIcon;
		const Subject = testSubject;

		// WHEN
		const { container } = render(<Subject name="pi-home" />);

		// THEN
		expect(container.querySelector("svg")).not.toBeNull();
	});

	it("should fall back for an unknown icon", () => {
		// GIVEN
		const testSubject = CategoryIcon;
		const Subject = testSubject;

		// WHEN
		const { container } = render(<Subject name="pi-unknown" />);

		// THEN
		expect(container.querySelector("svg")).not.toBeNull();
	});
});

it("should fall back without a name", () => {
	// GIVEN
	const testSubject = CategoryIcon;
	const Subject = testSubject;

	// WHEN
	const { container } = render(<Subject />);

	// THEN
	expect(container.querySelector("svg")).not.toBeNull();
});
