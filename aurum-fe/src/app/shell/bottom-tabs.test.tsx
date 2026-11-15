import { act, fireEvent, render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, useLocation } from "react-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { usePrivacyStore } from "@/stores/privacy";
import { useThemeStore } from "@/stores/theme";
import { BottomTabs } from "./bottom-tabs";

const mockUseAuth0 = vi.fn();

const CLOSE_BARRIER_FALLBACK_MS = 500;
const ACCOUNT_TRIGGER = "Profile";
const DECORATIVE_ATTRIBUTE = "aria-hidden";
const DECORATIVE_VALUE = "true";
const PRIMARY_POINTER = { isPrimary: true, button: 0, pointerId: 1 } as const;
const POINTER_CLICK_DETAIL = 1;
const FIRED_ONCE = 1;
const ACCOUNT_NAME = "Mario Di Modica";
const ACCOUNT_EMAIL = "mario@example.com";

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

function getCloseBarrier(container: HTMLElement) {
	const decorations = Array.from(container.children).filter(
		child => child.getAttribute(DECORATIVE_ATTRIBUTE) === DECORATIVE_VALUE
	);
	return decorations[0] ?? null;
}

function mockAccount(logout = vi.fn()) {
	mockUseAuth0.mockReturnValue({
		user: { name: ACCOUNT_NAME, email: ACCOUNT_EMAIL },
		logout
	});
	return logout;
}

async function openAccountDrawer(user: ReturnType<typeof userEvent.setup>) {
	await user.click(screen.getByRole("button", { name: ACCOUNT_TRIGGER }));
	return screen.findByRole("dialog");
}

function pressOption(element: HTMLElement) {
	fireEvent.pointerDown(element, PRIMARY_POINTER);
	fireEvent.pointerUp(element, PRIMARY_POINTER);
	fireEvent.click(element, { detail: 1 });
}

describe("BottomTabs", () => {
	beforeEach(() => {
		usePrivacyStore.setState({ isPrivacy: false });
		useThemeStore.setState({ theme: "light" });
		mockUseAuth0.mockReset();
		mockAccount();
	});

	afterEach(() => {
		vi.restoreAllMocks();
		vi.useRealTimers();
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

	describe("close barrier", () => {
		it("should not show the barrier while the drawer is open", async () => {
			// GIVEN
			const user = userEvent.setup();
			const { container } = renderTabs();

			// WHEN
			await openAccountDrawer(user);

			// THEN
			expect(getCloseBarrier(container)).toBeNull();
		});

		it("should show the barrier when the drawer starts closing", async () => {
			// GIVEN
			const user = userEvent.setup();
			const { container } = renderTabs();
			const drawer = await openAccountDrawer(user);

			// WHEN
			await user.click(within(drawer).getByRole("button", { name: new RegExp(ACCOUNT_TRIGGER) }));

			// THEN
			expect(getCloseBarrier(container)).not.toBeNull();
		});

		it("should lift the barrier on the drawer transition end", async () => {
			// GIVEN
			const user = userEvent.setup();
			const { container } = renderTabs();
			const drawer = await openAccountDrawer(user);
			await user.click(within(drawer).getByRole("button", { name: new RegExp(ACCOUNT_TRIGGER) }));
			expect(getCloseBarrier(container)).not.toBeNull();

			// WHEN
			fireEvent.transitionEnd(drawer);

			// THEN
			expect(getCloseBarrier(container)).toBeNull();
		});

		it("should ignore a transition end bubbling from the drawer content", async () => {
			// GIVEN
			const user = userEvent.setup();
			const { container } = renderTabs();
			const drawer = await openAccountDrawer(user);
			const accountName = within(drawer).getByText(ACCOUNT_NAME);
			await user.click(within(drawer).getByRole("button", { name: new RegExp(ACCOUNT_TRIGGER) }));
			expect(getCloseBarrier(container)).not.toBeNull();

			// WHEN
			fireEvent.transitionEnd(accountName);

			// THEN
			expect(getCloseBarrier(container)).not.toBeNull();
		});

		it("should lift the barrier after the fallback delay without a transition", async () => {
			// GIVEN
			const user = userEvent.setup();
			const { container } = renderTabs();
			const drawer = await openAccountDrawer(user);
			vi.useFakeTimers();
			pressOption(within(drawer).getByRole("button", { name: new RegExp(ACCOUNT_TRIGGER) }));
			expect(getCloseBarrier(container)).not.toBeNull();

			// WHEN
			act(() => {
				vi.advanceTimersByTime(CLOSE_BARRIER_FALLBACK_MS);
			});

			// THEN
			expect(getCloseBarrier(container)).toBeNull();
		});

		it("should survive an unmount while the close fallback is pending", async () => {
			// GIVEN
			const user = userEvent.setup();
			const consoleError = vi.spyOn(console, "error").mockImplementation(() => undefined);
			const { container, unmount } = renderTabs();
			await openAccountDrawer(user);
			vi.useFakeTimers();
			fireEvent.keyDown(document, { key: "Escape" });
			expect(getCloseBarrier(container)).not.toBeNull();

			// WHEN
			unmount();
			// The fallback case above already covers "a missing transition end never
			// leaves the page inert"; this one only guards the unmount path.
			act(() => {
				vi.advanceTimersByTime(CLOSE_BARRIER_FALLBACK_MS);
			});

			// THEN
			const actWarnings = consoleError.mock.calls
				.map(([message]) => String(message))
				.filter(message => message.includes("not wrapped in act"));
			expect(actWarnings).toEqual([]);
		});
	});

	describe("press gestures", () => {
		it("should run a drawer action exactly once for a gesture and its trailing click", async () => {
			// GIVEN
			const logout = mockAccount();
			const user = userEvent.setup();
			renderTabs();
			const drawer = await openAccountDrawer(user);
			const logOutOption = within(drawer).getByRole("button", { name: /Log out/ });

			// WHEN the finger lifts
			fireEvent.pointerDown(logOutOption, PRIMARY_POINTER);
			fireEvent.pointerUp(logOutOption, PRIMARY_POINTER);

			// THEN the first tap already acted
			expect(logout).toHaveBeenCalledTimes(FIRED_ONCE);

			// WHEN the click that follows the gesture arrives
			fireEvent.click(logOutOption, { detail: POINTER_CLICK_DETAIL });

			// THEN it does not act again
			expect(logout).toHaveBeenCalledTimes(FIRED_ONCE);
		});
	});
});
