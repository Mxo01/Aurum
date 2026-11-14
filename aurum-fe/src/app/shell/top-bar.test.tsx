import { screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, useLocation } from "react-router";
import { render } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { usePrivacyStore } from "@/stores/privacy";
import { useThemeStore } from "@/stores/theme";
import { useUiStore } from "@/stores/ui";
import { TopBar } from "./top-bar";

const mockUseAuth0 = vi.fn();

vi.mock("@auth0/auth0-react", () => ({
	useAuth0: () => mockUseAuth0()
}));

function Location() {
	const location = useLocation();
	return <span data-testid="location">{`${location.pathname}${location.search}`}</span>;
}

function renderTopBar(route = "/dashboard") {
	return render(
		<MemoryRouter initialEntries={[route]}>
			<TopBar />
			<Location />
		</MemoryRouter>
	);
}

describe("TopBar", () => {
	beforeEach(() => {
		useUiStore.setState({ commandOpen: false });
		usePrivacyStore.setState({ isPrivacy: false });
		useThemeStore.setState({ theme: "light" });
		mockUseAuth0.mockReset();
		mockUseAuth0.mockReturnValue({
			user: { name: "Mario Di Modica", email: "mario@example.com" },
			logout: vi.fn()
		});
	});

	it("should render the brand, the sections and the search shortcut", () => {
		// WHEN
		renderTopBar();

		// THEN
		expect(screen.getByRole("link", { name: "Aurum home" })).toBeInTheDocument();
		expect(screen.getByRole("link", { name: "Dashboard" })).toHaveAttribute("href", "/dashboard");
		expect(screen.getByRole("button", { name: "Search" })).toBeInTheDocument();
	});

	it("should open the command palette from the search button", async () => {
		// GIVEN
		const user = userEvent.setup();
		renderTopBar();

		// WHEN
		await user.click(screen.getByRole("button", { name: "Search" }));

		// THEN
		expect(useUiStore.getState().commandOpen).toBe(true);
	});

	it("should open the account menu and navigate to the profile", async () => {
		// GIVEN
		const user = userEvent.setup();
		renderTopBar();

		// WHEN
		await user.click(screen.getByRole("button", { name: "Account menu" }));
		const menu = await screen.findByRole("menu");
		expect(within(menu).getByText("Mario Di Modica")).toBeInTheDocument();
		await user.click(within(menu).getByRole("menuitem", { name: /Profile/ }));

		// THEN
		expect(screen.getByTestId("location")).toHaveTextContent("/profile");
	});

	it("should toggle the privacy mode from the account menu", async () => {
		// GIVEN
		const user = userEvent.setup();
		renderTopBar();

		// WHEN
		await user.click(screen.getByRole("button", { name: "Account menu" }));
		await user.click(await screen.findByRole("menuitem", { name: /Hide amounts/ }));

		// THEN
		expect(usePrivacyStore.getState().isPrivacy).toBe(true);
	});

	it("should mask the account details in privacy mode", async () => {
		// GIVEN
		usePrivacyStore.setState({ isPrivacy: true });
		const user = userEvent.setup();
		renderTopBar();

		// WHEN
		await user.click(screen.getByRole("button", { name: "Account menu" }));

		// THEN
		const menu = await screen.findByRole("menu");
		expect(within(menu).getByText("••••••••")).toBeInTheDocument();
		expect(within(menu).getByText("••••••••••••")).toBeInTheDocument();
		expect(within(menu).getByRole("menuitem", { name: /Show amounts/ })).toBeInTheDocument();
	});

	it("should switch the theme from the account menu", async () => {
		// GIVEN
		const user = userEvent.setup();
		renderTopBar();

		// WHEN
		await user.click(screen.getByRole("button", { name: "Account menu" }));
		await user.click(await screen.findByRole("menuitem", { name: /Dark theme/ }));

		// THEN
		expect(useThemeStore.getState().theme).toBe("dark");
	});

	it("should log out from the account menu", async () => {
		// GIVEN
		const logout = vi.fn();
		mockUseAuth0.mockReturnValue({
			user: { name: "Mario Di Modica", email: "mario@example.com" },
			logout
		});
		const user = userEvent.setup();
		renderTopBar();

		// WHEN
		await user.click(screen.getByRole("button", { name: "Account menu" }));
		await user.click(await screen.findByRole("menuitem", { name: /Log out/ }));

		// THEN
		expect(logout).toHaveBeenCalledWith({
			logoutParams: { returnTo: window.location.origin }
		});
	});

	it("should fall back to the initials without a user", () => {
		// GIVEN
		mockUseAuth0.mockReturnValue({ user: undefined, logout: vi.fn() });

		// WHEN
		renderTopBar();

		// THEN
		expect(screen.getByText("AU")).toBeInTheDocument();
	});

	it("should derive the initials from a single name", () => {
		// GIVEN
		mockUseAuth0.mockReturnValue({ user: { name: "Mario" }, logout: vi.fn() });

		// WHEN
		renderTopBar();

		// THEN
		expect(screen.getByText("M")).toBeInTheDocument();
	});
});
