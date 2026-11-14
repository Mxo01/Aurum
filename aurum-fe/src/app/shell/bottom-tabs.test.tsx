import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, useLocation } from "react-router";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { usePrivacyStore } from "@/stores/privacy";
import { useThemeStore } from "@/stores/theme";
import { BottomTabs } from "./bottom-tabs";

const mockUseAuth0 = vi.fn();

vi.mock("@auth0/auth0-react", () => ({
	useAuth0: () => mockUseAuth0()
}));

function Location() {
	const location = useLocation();
	return <span data-testid="location">{`${location.pathname}${location.search}`}</span>;
}

function renderTabs(route = "/dashboard") {
	return render(
		<MemoryRouter initialEntries={[route]}>
			<BottomTabs />
			<Location />
		</MemoryRouter>
	);
}

describe("BottomTabs", () => {
	beforeEach(() => {
		usePrivacyStore.setState({ isPrivacy: false });
		useThemeStore.setState({ theme: "light" });
		mockUseAuth0.mockReset();
		mockUseAuth0.mockReturnValue({
			user: { name: "Mario Di Modica", email: "mario@example.com" },
			logout: vi.fn()
		});
	});

	it("should render the sections and the profile trigger", () => {
		// WHEN
		renderTabs();

		// THEN
		expect(screen.getByRole("link", { name: "Dashboard" })).toHaveAttribute("href", "/dashboard");
		expect(screen.getByRole("button", { name: "Profile" })).toBeInTheDocument();
	});

	it("should open the account drawer and navigate to the profile", async () => {
		// GIVEN
		const user = userEvent.setup();
		renderTabs();

		// WHEN
		await user.click(screen.getByRole("button", { name: "Profile" }));
		const dialog = await screen.findByRole("dialog");
		expect(within(dialog).getByText("Mario Di Modica")).toBeInTheDocument();
		await user.click(within(dialog).getByRole("button", { name: /Profile/ }));

		// THEN
		expect(screen.getByTestId("location")).toHaveTextContent("/profile");
	});

	it("should toggle the privacy mode from the drawer", async () => {
		// GIVEN
		const user = userEvent.setup();
		renderTabs();

		// WHEN
		await user.click(screen.getByRole("button", { name: "Profile" }));
		const dialog = await screen.findByRole("dialog");
		await user.click(within(dialog).getByRole("button", { name: /Hide amounts/ }));

		// THEN
		expect(usePrivacyStore.getState().isPrivacy).toBe(true);
	});

	it("should mask the account details in privacy mode", async () => {
		// GIVEN
		usePrivacyStore.setState({ isPrivacy: true });
		const user = userEvent.setup();
		renderTabs();

		// WHEN
		await user.click(screen.getByRole("button", { name: "Profile" }));

		// THEN
		const dialog = await screen.findByRole("dialog");
		expect(within(dialog).getByText("••••••••")).toBeInTheDocument();
		expect(within(dialog).getByRole("button", { name: /Show amounts/ })).toBeInTheDocument();
	});

	it("should switch the theme from the drawer", async () => {
		// GIVEN
		const user = userEvent.setup();
		renderTabs();

		// WHEN
		await user.click(screen.getByRole("button", { name: "Profile" }));
		const dialog = await screen.findByRole("dialog");
		await user.click(within(dialog).getByRole("button", { name: /Dark theme/ }));

		// THEN
		expect(useThemeStore.getState().theme).toBe("dark");
	});

	it("should log out from the drawer", async () => {
		// GIVEN
		const logout = vi.fn();
		mockUseAuth0.mockReturnValue({
			user: { name: "Mario Di Modica", email: "mario@example.com" },
			logout
		});
		const user = userEvent.setup();
		renderTabs();

		// WHEN
		await user.click(screen.getByRole("button", { name: "Profile" }));
		const dialog = await screen.findByRole("dialog");
		await user.click(within(dialog).getByRole("button", { name: /Log out/ }));

		// THEN
		expect(logout).toHaveBeenCalledWith({
			logoutParams: { returnTo: window.location.origin }
		});
	});

	it("should fall back to the initials without a user", () => {
		// GIVEN
		mockUseAuth0.mockReturnValue({ user: undefined, logout: vi.fn() });

		// WHEN
		renderTabs();

		// THEN
		expect(screen.getByText("AU")).toBeInTheDocument();
	});
});
