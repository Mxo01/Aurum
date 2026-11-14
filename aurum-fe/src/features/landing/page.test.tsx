import { fireEvent, screen } from "@testing-library/react";
import { createElement } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { renderWithProviders } from "@/test/render";
import { useThemeStore } from "@/stores/theme";
import { LandingPage } from "./page";

const mockUseAuth0 = vi.fn();
const mockLoginWithRedirect = vi.fn();

vi.mock("@auth0/auth0-react", () => ({
	useAuth0: () => mockUseAuth0()
}));

describe("LandingPage", () => {
	beforeEach(() => {
		mockUseAuth0.mockReset();
		mockLoginWithRedirect.mockReset();
		useThemeStore.setState({ theme: "light" });
	});

	it("should send an anonymous visitor to login", () => {
		// GIVEN
		mockUseAuth0.mockReturnValue({
			isAuthenticated: false,
			loginWithRedirect: mockLoginWithRedirect
		});
		const testSubject = LandingPage;
		renderWithProviders(createElement(testSubject), { route: "/" });

		// WHEN
		fireEvent.click(screen.getAllByRole("button", { name: /Get started free/ })[0]!);

		// THEN
		expect(mockLoginWithRedirect).toHaveBeenCalledWith({ appState: { returnTo: "/dashboard" } });
	});

	it("should link to the dashboard for an authenticated visitor", () => {
		// GIVEN
		mockUseAuth0.mockReturnValue({
			isAuthenticated: true,
			loginWithRedirect: mockLoginWithRedirect
		});
		const testSubject = LandingPage;

		// WHEN
		renderWithProviders(createElement(testSubject), { route: "/" });

		// THEN
		expect(screen.getByRole("link", { name: "Open dashboard" })).toHaveAttribute(
			"href",
			"/dashboard"
		);
	});

	it("should toggle the theme", () => {
		// GIVEN
		mockUseAuth0.mockReturnValue({
			isAuthenticated: false,
			loginWithRedirect: mockLoginWithRedirect
		});
		const testSubject = LandingPage;
		renderWithProviders(createElement(testSubject), { route: "/" });

		// WHEN
		fireEvent.click(screen.getByRole("button", { name: "Switch to dark theme" }));

		// THEN
		expect(useThemeStore.getState().theme).toBe("dark");
	});
});

it("should start the login from the header and the final call to action", () => {
	// GIVEN
	mockUseAuth0.mockReturnValue({
		isAuthenticated: false,
		loginWithRedirect: mockLoginWithRedirect
	});
	const testSubject = LandingPage;
	renderWithProviders(createElement(testSubject), { route: "/" });

	// WHEN
	fireEvent.click(screen.getByRole("button", { name: "Sign in" }));
	fireEvent.click(screen.getByRole("button", { name: "Get started" }));
	const ctas = screen.getAllByRole("button", { name: /Get started free/ });
	fireEvent.click(ctas[ctas.length - 1]!);

	// THEN
	expect(mockLoginWithRedirect).toHaveBeenCalledTimes(3);
});
