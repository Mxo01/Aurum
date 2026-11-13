import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createElement } from "react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { env } from "@/lib/env";
import { buildProfile } from "@/test/fixtures/portfolio";
import { server } from "@/test/msw/server";
import { renderWithProviders } from "@/test/render";
import { useThemeStore } from "@/stores/theme";
import { ProfilePage } from "./page";

const { mockLogout, mockUser } = vi.hoisted(() => ({
	mockLogout: vi.fn(),
	mockUser: {
		sub: "auth0|123",
		name: "Mario Di Modica",
		email: "mario@example.com",
		picture: "https://example.com/avatar.jpg"
	}
}));

vi.mock("@auth0/auth0-react", () => ({
	useAuth0: () => ({
		user: mockUser,
		logout: mockLogout,
		isAuthenticated: true
	})
}));

function useProfileFixture() {
	server.use(http.get(`${env.apiUrl}/users`, () => HttpResponse.json(buildProfile())));
}

describe("ProfilePage", () => {
	beforeEach(() => {
		mockUser.sub = "auth0|123";
		mockUser.name = "Mario Di Modica";
		mockLogout.mockReset();
		localStorage.clear();
		document.documentElement.className = "";
		useThemeStore.setState({ theme: "light" });
		Object.defineProperty(navigator, "clipboard", {
			value: { writeText: vi.fn().mockResolvedValue(undefined) },
			configurable: true
		});
	});

	it("should render the personal details and preferences", async () => {
		// GIVEN
		useProfileFixture();
		const testSubject = ProfilePage;

		// WHEN
		renderWithProviders(createElement(testSubject));

		// THEN
		expect(screen.getByRole("heading", { name: "Profile" })).toBeInTheDocument();
		expect(await screen.findByDisplayValue("Mario Di Modica")).toBeInTheDocument();
		expect(screen.getByLabelText("Currency")).toHaveTextContent("EUR");
		expect(screen.getByLabelText("Locale")).toHaveTextContent("English · 1,234.56");
	});

	it("should update the currency and the locale", async () => {
		// GIVEN
		useProfileFixture();
		let currencyBody: unknown;
		let localeBody: unknown;
		server.use(
			http.put(`${env.apiUrl}/users/currency`, async ({ request }) => {
				currencyBody = await request.json();
				return new HttpResponse(null, { status: 200 });
			}),
			http.put(`${env.apiUrl}/users/locale`, async ({ request }) => {
				localeBody = await request.json();
				return new HttpResponse(null, { status: 200 });
			})
		);
		const testSubject = ProfilePage;
		renderWithProviders(createElement(testSubject));

		// WHEN
		const user = userEvent.setup();
		await user.click(await screen.findByLabelText("Currency"));
		await user.click(await screen.findByRole("option", { name: "USD" }));
		await user.click(screen.getByLabelText("Locale"));
		await user.click(await screen.findByRole("option", { name: /Italian/ }));

		// THEN
		await waitFor(() => expect(currencyBody).toEqual({ currency: "USD" }));
		expect(localeBody).toEqual({ locale: "it-IT" });
	});

	it("should switch the theme", async () => {
		// GIVEN
		useProfileFixture();
		const testSubject = ProfilePage;
		renderWithProviders(createElement(testSubject));

		// WHEN
		fireEvent.click(screen.getByRole("button", { name: "Dark" }));

		// THEN
		expect(document.documentElement.classList.contains("dark")).toBe(true);
	});

	it("should show the MCP setup and copy the config", async () => {
		// GIVEN
		useProfileFixture();
		renderWithProviders(createElement(ProfilePage));
		fireEvent.mouseDown(screen.getByRole("tab", { name: "Integrations" }));

		// WHEN
		const copy = screen.getByRole("button", { name: "Copy MCP config" });
		fireEvent.click(copy);

		// THEN
		expect(screen.getByText(/mcpServers/)).toBeInTheDocument();
		await waitFor(() =>
			expect(navigator.clipboard.writeText).toHaveBeenCalledWith(expect.stringContaining("/mcp"))
		);
	});

	it("should export the data", async () => {
		// GIVEN
		useProfileFixture();
		let exportCalls = 0;
		server.use(
			http.get(`${env.apiUrl}/users/export`, () => {
				exportCalls += 1;
				return HttpResponse.json({ assets: [] });
			})
		);
		const createObjectUrl = vi.spyOn(URL, "createObjectURL").mockReturnValue("blob:test");
		const revokeObjectUrl = vi.spyOn(URL, "revokeObjectURL").mockImplementation(() => undefined);
		vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(() => undefined);
		renderWithProviders(createElement(ProfilePage));
		fireEvent.mouseDown(screen.getByRole("tab", { name: "Data & privacy" }));

		// WHEN
		fireEvent.click(screen.getByRole("button", { name: "Export JSON" }));

		// THEN
		await waitFor(() => expect(exportCalls).toBe(1));
		expect(createObjectUrl).toHaveBeenCalledOnce();
		expect(revokeObjectUrl).toHaveBeenCalledOnce();
	});

	it("should require the typed confirmation before deleting the account", async () => {
		// GIVEN
		useProfileFixture();
		let deleteCalls = 0;
		server.use(
			http.delete(`${env.apiUrl}/users`, () => {
				deleteCalls += 1;
				return new HttpResponse(null, { status: 200 });
			})
		);
		renderWithProviders(createElement(ProfilePage));
		fireEvent.mouseDown(screen.getByRole("tab", { name: "Data & privacy" }));
		fireEvent.click(screen.getByRole("button", { name: "Delete account" }));

		// WHEN
		const dialog = await screen.findByRole("alertdialog");
		const confirm = within(dialog).getByRole("button", { name: "Delete account" });
		expect(confirm).toBeDisabled();
		fireEvent.change(screen.getByLabelText("Type DELETE to confirm"), {
			target: { value: "DELETE" }
		});
		fireEvent.click(confirm);

		// THEN
		await waitFor(() => expect(deleteCalls).toBe(1));
		expect(mockLogout).toHaveBeenCalledOnce();
	});

	it("should lock the name and picture for a Google profile", async () => {
		// GIVEN
		mockUser.sub = "google-oauth2|123";
		useProfileFixture();
		const testSubject = ProfilePage;

		// WHEN
		renderWithProviders(createElement(testSubject));

		// THEN
		expect(await screen.findByLabelText("Name")).toBeDisabled();
		expect(screen.queryByRole("button", { name: /Upload picture/ })).not.toBeInTheDocument();
	});
});
