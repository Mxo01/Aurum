import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createElement } from "react";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { env } from "@/lib/env";
import { toast } from "sonner";
import { buildProfile } from "@/test/fixtures/portfolio";
import { server } from "@/test/msw/server";
import { renderWithProviders } from "@/test/render";
import { useThemeStore } from "@/stores/theme";
import { ProfilePage } from "./page";

const { mockLogout, mockUser } = vi.hoisted(() => ({
	mockLogout: vi.fn(),
	mockUser: {
		sub: "auth0|123" as string | undefined,
		name: "Mario Di Modica" as string | undefined,
		email: "mario@example.com" as string | undefined,
		picture: "https://example.com/avatar.jpg" as string | undefined
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
		fireEvent.mouseDown(screen.getByRole("tab", { name: "Dark" }));

		// THEN
		expect(document.documentElement.classList.contains("dark")).toBe(true);
	});

	it("should show the MCP setup and copy the config", async () => {
		// GIVEN
		useProfileFixture();
		renderWithProviders(createElement(ProfilePage));
		fireEvent.mouseDown(screen.getByRole("tab", { name: "Integrations" }));

		// WHEN
		const copy = screen.getByRole("button", { name: "Copy" });
		fireEvent.click(copy);

		// THEN
		expect(screen.getByText(/mcpServers/)).toBeInTheDocument();
		await waitFor(() =>
			expect(navigator.clipboard.writeText).toHaveBeenCalledWith(expect.stringContaining("/mcp"))
		);
		expect(await screen.findByRole("button", { name: "Copied" })).toBeInTheDocument();

		// WHEN
		await waitFor(() => expect(screen.getByRole("button", { name: "Copy" })).toBeInTheDocument(), {
			timeout: 2500
		});
	});

	it("should open the picture picker", async () => {
		// GIVEN
		useProfileFixture();
		renderWithProviders(createElement(ProfilePage));
		const input = await screen.findByLabelText("Upload profile picture");
		const clickSpy = vi.spyOn(input as HTMLInputElement, "click");

		// WHEN
		fireEvent.click(screen.getByRole("button", { name: /Upload picture/ }));

		// THEN
		expect(clickSpy).toHaveBeenCalled();
	});

	it("should fall back when the auth user has no profile fields", async () => {
		// GIVEN
		mockUser.sub = undefined;
		mockUser.name = undefined;
		mockUser.picture = undefined;
		mockUser.email = undefined;
		useProfileFixture();

		// WHEN
		renderWithProviders(createElement(ProfilePage));

		// THEN
		expect(await screen.findByLabelText("Name")).toHaveValue("");
		expect(screen.getByRole("button", { name: /Upload picture/ })).toBeInTheDocument();
	});

	it("should report a copy failure", async () => {
		// GIVEN
		useProfileFixture();
		Object.defineProperty(navigator, "clipboard", {
			value: { writeText: vi.fn().mockRejectedValue(new Error("nope")) },
			configurable: true
		});
		const errorSpy = vi.spyOn(toast, "error");
		renderWithProviders(createElement(ProfilePage));
		fireEvent.mouseDown(screen.getByRole("tab", { name: "Integrations" }));

		// WHEN
		fireEvent.click(screen.getByRole("button", { name: "Copy" }));

		// THEN
		await waitFor(() => expect(errorSpy).toHaveBeenCalledWith("Could not copy the config"));
	});

	it("should show the mcp config file and hide it on the mobile tab", async () => {
		// GIVEN
		useProfileFixture();
		renderWithProviders(createElement(ProfilePage));
		fireEvent.mouseDown(screen.getByRole("tab", { name: "Integrations" }));

		// WHEN
		fireEvent.mouseDown(screen.getByRole("tab", { name: "Mobile / Web" }));

		// THEN
		expect(screen.getByText("Read and write Aurum data from any MCP client.")).toBeInTheDocument();
		await waitFor(() => expect(screen.queryByText("mcp.json")).not.toBeInTheDocument());
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

	it("should upload a picture", async () => {
		// GIVEN
		useProfileFixture();
		let uploads = 0;
		server.use(
			http.post(`${env.apiUrl}/users/picture`, () => {
				uploads += 1;
				return new HttpResponse(null, { status: 200 });
			})
		);
		renderWithProviders(createElement(ProfilePage));
		const input = await screen.findByLabelText("Upload profile picture");
		const file = new File(["avatar"], "avatar.png", { type: "image/png" });

		// WHEN
		fireEvent.change(input, { target: { files: [file] } });

		// THEN
		await waitFor(() => expect(uploads).toBe(1));
	});

	it("should reject a picture over five megabytes", async () => {
		// GIVEN
		useProfileFixture();
		let uploads = 0;
		server.use(
			http.post(`${env.apiUrl}/users/picture`, () => {
				uploads += 1;
				return new HttpResponse(null, { status: 200 });
			})
		);
		const errorSpy = vi.spyOn(toast, "error");
		renderWithProviders(createElement(ProfilePage));
		const input = await screen.findByLabelText("Upload profile picture");
		const file = new File(["avatar"], "big.png", { type: "image/png" });
		Object.defineProperty(file, "size", { value: 6 * 1024 * 1024 });

		// WHEN
		fireEvent.change(input, { target: { files: [file] } });

		// THEN
		expect(errorSpy).toHaveBeenCalledWith("The picture must be 5 MB or smaller");
		expect(uploads).toBe(0);
	});

	it("should ignore an empty picture selection", async () => {
		// GIVEN
		useProfileFixture();
		let uploads = 0;
		server.use(
			http.post(`${env.apiUrl}/users/picture`, () => {
				uploads += 1;
				return new HttpResponse(null, { status: 200 });
			})
		);
		renderWithProviders(createElement(ProfilePage));
		const input = await screen.findByLabelText("Upload profile picture");

		// WHEN
		fireEvent.change(input, { target: { files: [] } });

		// THEN
		expect(uploads).toBe(0);
	});

	it("should show and remove a custom picture", async () => {
		// GIVEN
		server.use(
			http.get(`${env.apiUrl}/users`, () =>
				HttpResponse.json(buildProfile({ picture: "ZmFrZQ==" }))
			)
		);
		let removals = 0;
		server.use(
			http.delete(`${env.apiUrl}/users/picture`, () => {
				removals += 1;
				return new HttpResponse(null, { status: 200 });
			})
		);
		renderWithProviders(createElement(ProfilePage));

		// WHEN
		fireEvent.click(await screen.findByRole("button", { name: /Remove/ }));

		// THEN
		await waitFor(() => expect(removals).toBe(1));
	});

	it("should save a changed name on blur", async () => {
		// GIVEN
		useProfileFixture();
		let body: unknown;
		server.use(
			http.put(`${env.apiUrl}/users`, async ({ request }) => {
				body = await request.json();
				return new HttpResponse(null, { status: 200 });
			})
		);
		renderWithProviders(createElement(ProfilePage));
		const name = await screen.findByDisplayValue("Mario Di Modica");

		// WHEN
		fireEvent.change(name, { target: { value: "  Mario Rossi  " } });
		fireEvent.blur(name);

		// THEN
		await waitFor(() => expect(body).toEqual({ name: "Mario Rossi" }));
	});

	it("should not save an unchanged or empty name", async () => {
		// GIVEN
		useProfileFixture();
		let calls = 0;
		server.use(
			http.put(`${env.apiUrl}/users`, () => {
				calls += 1;
				return new HttpResponse(null, { status: 200 });
			})
		);
		renderWithProviders(createElement(ProfilePage));
		const name = await screen.findByDisplayValue("Mario Di Modica");

		// WHEN
		fireEvent.blur(name);
		fireEvent.change(name, { target: { value: "   " } });
		fireEvent.blur(name);

		// THEN
		expect(calls).toBe(0);
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
