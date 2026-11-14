import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { Splash } from "./splash";

describe("Splash", () => {
	it("should show the loading state", () => {
		// GIVEN
		const testSubject = Splash;
		const Subject = testSubject;

		// WHEN
		render(<Subject />);

		// THEN
		expect(screen.getByText("Loading Aurum")).toBeInTheDocument();
	});
});
