import { screen } from "@testing-library/react";
import { createElement } from "react";
import { describe, expect, it } from "vitest";
import { renderWithProviders } from "@/test/render";
import { Brand } from "./brand";

describe("Brand", () => {
	it("should link to the dashboard by default", () => {
		// GIVEN
		const testSubject = Brand;

		// WHEN
		renderWithProviders(createElement(testSubject), { route: "/dashboard" });

		// THEN
		expect(screen.getByRole("link", { name: "Aurum home" })).toHaveAttribute("href", "/dashboard");
	});

	it("should accept a custom destination", () => {
		// GIVEN
		const testSubject = Brand;

		// WHEN
		renderWithProviders(createElement(testSubject, { to: "/" }));

		// THEN
		expect(screen.getByRole("link", { name: "Aurum home" })).toHaveAttribute("href", "/");
	});
});
