import { fireEvent, screen } from "@testing-library/react";
import { createElement } from "react";
import { beforeEach, describe, expect, it } from "vitest";
import { renderWithProviders } from "@/test/render";
import { CONSENT_KEY } from "./consent";
import { ConsentBanner } from "./consent-banner";

describe("ConsentBanner", () => {
	beforeEach(() => {
		window.localStorage.clear();
	});

	it("should ask for consent when it was not given", () => {
		// GIVEN
		const testSubject = ConsentBanner;

		// WHEN
		renderWithProviders(createElement(testSubject));

		// THEN
		expect(screen.getByText(/Aurum stores your financial data/)).toBeInTheDocument();
	});

	it("should store the consent and hide itself", () => {
		// GIVEN
		const testSubject = ConsentBanner;
		renderWithProviders(createElement(testSubject));

		// WHEN
		fireEvent.click(screen.getByRole("button", { name: "Accept" }));

		// THEN
		expect(window.localStorage.getItem(CONSENT_KEY)).toBe("true");
		expect(screen.queryByText(/Aurum stores your financial data/)).not.toBeInTheDocument();
	});

	it("should stay hidden when consent was already given", () => {
		// GIVEN
		window.localStorage.setItem(CONSENT_KEY, "true");
		const testSubject = ConsentBanner;

		// WHEN
		renderWithProviders(createElement(testSubject));

		// THEN
		expect(screen.queryByText(/Aurum stores your financial data/)).not.toBeInTheDocument();
	});
});
