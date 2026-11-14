import { screen } from "@testing-library/react";
import { createElement } from "react";
import { describe, expect, it } from "vitest";
import { renderWithProviders } from "@/test/render";
import { PrivacyPolicyPage } from "./privacy-policy-page";

describe("PrivacyPolicyPage", () => {
	it("should render the policy and the back link", () => {
		// GIVEN
		const testSubject = PrivacyPolicyPage;

		// WHEN
		renderWithProviders(createElement(testSubject), { route: "/privacy" });

		// THEN
		expect(screen.getByRole("heading", { name: "Privacy Policy" })).toBeInTheDocument();
		expect(screen.getByRole("link", { name: "Back" })).toHaveAttribute("href", "/");
	});
});
