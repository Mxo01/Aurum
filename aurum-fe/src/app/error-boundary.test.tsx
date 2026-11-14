import { fireEvent, render, screen } from "@testing-library/react";
import { createElement } from "react";
import { describe, expect, it, vi } from "vitest";
import { ErrorBoundary } from "./error-boundary";

let shouldThrow = true;

function Boom() {
	if (shouldThrow) throw new Error("boom");
	return <p>recovered</p>;
}

describe("ErrorBoundary", () => {
	it("should show the fallback and recover after retry", () => {
		// GIVEN
		const testSubject = ErrorBoundary;
		const consoleSpy = vi.spyOn(console, "error").mockImplementation(() => undefined);

		// WHEN
		render(createElement(testSubject, null, <Boom />));

		// THEN
		expect(screen.getByText("Something went wrong")).toBeInTheDocument();
		expect(screen.getByText("boom")).toBeInTheDocument();

		shouldThrow = false;
		fireEvent.click(screen.getByRole("button", { name: "Try again" }));
		expect(screen.getByText("recovered")).toBeInTheDocument();

		consoleSpy.mockRestore();
	});
});

describe("ErrorBoundary fallback message", () => {
	it("should show the generic message for an empty error", () => {
		// GIVEN
		const consoleSpy = vi.spyOn(console, "error").mockImplementation(() => undefined);
		function EmptyBoom(): never {
			throw new Error("");
		}
		const testSubject = ErrorBoundary;

		// WHEN
		render(createElement(testSubject, null, <EmptyBoom />));

		// THEN
		expect(screen.getByText("An unexpected error interrupted the page.")).toBeInTheDocument();
		consoleSpy.mockRestore();
	});
});
