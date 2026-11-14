import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { Kbd } from "./kbd";

describe("Kbd", () => {
	it("should render the shortcut and merge the class name", () => {
		// GIVEN
		const testSubject = Kbd;
		const Subject = testSubject;

		// WHEN
		render(<Subject className="extra">⌘K</Subject>);

		// THEN
		const expected = screen.getByText("⌘K");
		expect(expected.tagName).toBe("KBD");
		expect(expected).toHaveClass("extra");
	});
});
