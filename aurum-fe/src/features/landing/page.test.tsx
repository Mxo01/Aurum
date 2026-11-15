import { fireEvent, screen } from "@testing-library/react";
import { createElement, type ComponentType } from "react";
import { createMemoryRouter, RouterProvider } from "react-router";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useThemeStore } from "@/stores/theme";
import { renderWithProviders } from "@/test/render";
import { LandingPage } from "./page";

const mockUseAuth0 = vi.fn();
const mockLoginWithRedirect = vi.fn();

vi.mock("@auth0/auth0-react", () => ({
	useAuth0: () => mockUseAuth0()
}));

const LANDING_PATH = "/";
const DASHBOARD_PATH = "/dashboard";
const LOGIN_APP_STATE = { appState: { returnTo: DASHBOARD_PATH } };
const LANDING_HERO = /finally legible/i;
const DASHBOARD_HEADING = "Dashboard";
const REPLACE_ACTION = "REPLACE";

interface Auth0Gate {
	isLoading: boolean;
	isAuthenticated: boolean;
}

const buildAuth0Stub = (gate: Auth0Gate) => ({
	...gate,
	loginWithRedirect: mockLoginWithRedirect
});

function DashboardSentinel() {
	return <h1>{DASHBOARD_HEADING}</h1>;
}

function renderLanding(testSubject: ComponentType) {
	const router = createMemoryRouter(
		[
			{ path: LANDING_PATH, element: createElement(testSubject) },
			{ path: DASHBOARD_PATH, element: <DashboardSentinel /> }
		],
		{ initialEntries: [LANDING_PATH] }
	);

	return {
		router,
		...renderWithProviders(<RouterProvider router={router} />, { withRouter: false })
	};
}

interface LandingGateCase {
	name: string;
	auth0: Auth0Gate;
	rendersDashboard: boolean;
	rendersLanding: boolean;
	rendersNothing: boolean;
}

const LANDING_GATE_CASES: LandingGateCase[] = [
	{
		name: "waiting for Auth0",
		auth0: { isLoading: true, isAuthenticated: false },
		rendersDashboard: false,
		rendersLanding: false,
		rendersNothing: true
	},
	{
		name: "a signed in visitor",
		auth0: { isLoading: false, isAuthenticated: true },
		rendersDashboard: true,
		rendersLanding: false,
		rendersNothing: false
	},
	{
		name: "an anonymous visitor",
		auth0: { isLoading: false, isAuthenticated: false },
		rendersDashboard: false,
		rendersLanding: true,
		rendersNothing: false
	}
];

interface LoginTrigger {
	trigger: string;
	label: string;
	occurrence: number;
}

const LOGIN_TRIGGERS: LoginTrigger[] = [
	{ trigger: "the header sign in button", label: "Sign in", occurrence: 0 },
	{ trigger: "the header get started button", label: "Get started", occurrence: 0 },
	{ trigger: "the hero get started free button", label: "Get started free", occurrence: 0 },
	{ trigger: "the closing get started free button", label: "Get started free", occurrence: 1 }
];

interface ThemeToggleCase {
	stored: "light" | "dark";
	label: string;
	expectedTheme: "light" | "dark";
	expectedLabel: string;
}

const THEME_TOGGLE_CASES: ThemeToggleCase[] = [
	{
		stored: "light",
		label: "Switch to dark theme",
		expectedTheme: "dark",
		expectedLabel: "Switch to light theme"
	},
	{
		stored: "dark",
		label: "Switch to light theme",
		expectedTheme: "light",
		expectedLabel: "Switch to dark theme"
	}
];

describe("LandingPage", () => {
	beforeEach(() => {
		mockUseAuth0.mockReset();
		mockLoginWithRedirect.mockReset();
		useThemeStore.setState({ theme: "light" });
	});

	it.each(LANDING_GATE_CASES)("should gate the landing on $name", testCase => {
		// GIVEN
		mockUseAuth0.mockReturnValue(buildAuth0Stub(testCase.auth0));
		const testSubject = LandingPage;

		// WHEN
		const { container } = renderLanding(testSubject);

		// THEN
		const dashboard = screen.queryByRole("heading", { name: DASHBOARD_HEADING });
		const landing = screen.queryByRole("heading", { name: LANDING_HERO });

		expect(Boolean(dashboard)).toBe(testCase.rendersDashboard);
		expect(Boolean(landing)).toBe(testCase.rendersLanding);
		expect(container.innerHTML === "").toBe(testCase.rendersNothing);
		expect(mockLoginWithRedirect).not.toHaveBeenCalled();
	});

	it("should replace the landing entry with the dashboard for a signed in visitor", () => {
		// GIVEN
		mockUseAuth0.mockReturnValue(buildAuth0Stub({ isLoading: false, isAuthenticated: true }));
		const testSubject = LandingPage;

		// WHEN
		const { router } = renderLanding(testSubject);

		// THEN
		expect(screen.getByRole("heading", { name: DASHBOARD_HEADING })).toBeInTheDocument();
		expect(screen.queryByRole("heading", { name: LANDING_HERO })).toBeNull();
		expect(router.state.location.pathname).toBe(DASHBOARD_PATH);
		expect(router.state.historyAction).toBe(REPLACE_ACTION);
	});

	it("should render the landing content for an anonymous visitor", () => {
		// GIVEN
		mockUseAuth0.mockReturnValue(buildAuth0Stub({ isLoading: false, isAuthenticated: false }));
		const testSubject = LandingPage;

		// WHEN
		renderLanding(testSubject);

		// THEN
		expect(screen.getByRole("heading", { level: 1, name: LANDING_HERO })).toBeInTheDocument();
		expect(screen.getByText("31 currencies")).toBeInTheDocument();
		expect(screen.getByRole("button", { name: "Sign in" })).toBeInTheDocument();
		expect(screen.getByRole("button", { name: "Get started" })).toBeInTheDocument();
	});

	it.each(LOGIN_TRIGGERS)("should start the login from $trigger", ({ label, occurrence }) => {
		// GIVEN
		mockUseAuth0.mockReturnValue(buildAuth0Stub({ isLoading: false, isAuthenticated: false }));
		const testSubject = LandingPage;
		renderLanding(testSubject);

		// WHEN
		fireEvent.click(screen.getAllByRole("button", { name: label })[occurrence]!);

		// THEN
		expect(mockLoginWithRedirect).toHaveBeenCalledTimes(1);
		expect(mockLoginWithRedirect).toHaveBeenCalledWith(LOGIN_APP_STATE);
	});

	it.each(THEME_TOGGLE_CASES)(
		"should switch the theme from $stored to $expectedTheme",
		({ stored, label, expectedTheme, expectedLabel }) => {
			// GIVEN
			useThemeStore.setState({ theme: stored });
			mockUseAuth0.mockReturnValue(buildAuth0Stub({ isLoading: false, isAuthenticated: false }));
			const testSubject = LandingPage;
			renderLanding(testSubject);

			// WHEN
			fireEvent.click(screen.getByRole("button", { name: label }));

			// THEN
			expect(useThemeStore.getState().theme).toBe(expectedTheme);
			expect(screen.getByRole("button", { name: expectedLabel })).toBeInTheDocument();
		}
	);
});
