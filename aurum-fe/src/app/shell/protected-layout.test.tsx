import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { beforeEach, describe, expect, it, vi } from "vitest";

const mockUseAuth0 = vi.fn();
const mockLoginWithRedirect = vi.fn();

vi.mock("@auth0/auth0-react", () => ({
	useAuth0: () => mockUseAuth0()
}));

vi.mock("./app-shell", () => ({
	AppShell: () => <div>app shell</div>
}));

import { ProtectedLayout } from "./protected-layout";

describe("ProtectedLayout", () => {
	beforeEach(() => {
		mockUseAuth0.mockReset();
		mockLoginWithRedirect.mockReset();
	});

	it("should show the splash while auth is loading", () => {
		// GIVEN
		mockUseAuth0.mockReturnValue({
			isLoading: true,
			isAuthenticated: false,
			loginWithRedirect: mockLoginWithRedirect
		});
		const testSubject = ProtectedLayout;
		const Subject = testSubject;

		// WHEN
		render(
			<MemoryRouter initialEntries={["/dashboard?year=2026"]}>
				<Subject />
			</MemoryRouter>
		);

		// THEN
		expect(screen.getByText("Loading Aurum")).toBeInTheDocument();
		expect(mockLoginWithRedirect).not.toHaveBeenCalled();
	});

	it("should redirect an anonymous visitor back to the current route", () => {
		// GIVEN
		mockUseAuth0.mockReturnValue({
			isLoading: false,
			isAuthenticated: false,
			loginWithRedirect: mockLoginWithRedirect
		});
		const testSubject = ProtectedLayout;
		const Subject = testSubject;

		// WHEN
		render(
			<MemoryRouter initialEntries={["/assets?categories=1"]}>
				<Subject />
			</MemoryRouter>
		);

		// THEN
		expect(screen.getByText("Loading Aurum")).toBeInTheDocument();
		expect(mockLoginWithRedirect).toHaveBeenCalledWith({
			appState: { returnTo: "/assets?categories=1" }
		});
	});

	it("should render the shell for an authenticated user", () => {
		// GIVEN
		mockUseAuth0.mockReturnValue({
			isLoading: false,
			isAuthenticated: true,
			loginWithRedirect: mockLoginWithRedirect
		});
		const testSubject = ProtectedLayout;
		const Subject = testSubject;

		// WHEN
		render(
			<MemoryRouter initialEntries={["/dashboard"]}>
				<Subject />
			</MemoryRouter>
		);

		// THEN
		expect(screen.getByText("app shell")).toBeInTheDocument();
		expect(mockLoginWithRedirect).not.toHaveBeenCalled();
	});
});
