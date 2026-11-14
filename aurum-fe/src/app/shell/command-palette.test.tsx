import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { MemoryRouter, useLocation } from "react-router";
import { beforeEach, describe, expect, it } from "vitest";
import { env } from "@/lib/env";
import { buildAsset } from "@/test/fixtures/portfolio";
import { server } from "@/test/msw/server";
import { renderWithProviders } from "@/test/render";
import { usePrivacyStore } from "@/stores/privacy";
import { useThemeStore } from "@/stores/theme";
import { useUiStore } from "@/stores/ui";
import { CommandPalette } from "./command-palette";

const mockAssets = [
	buildAsset({ id: "a1", name: "Apartment Berlin", categoryName: "Real Estate" })
];

function Location() {
	const location = useLocation();
	return <span data-testid="location">{`${location.pathname}${location.search}`}</span>;
}

function renderPalette() {
	return renderWithProviders(
		<MemoryRouter initialEntries={["/dashboard"]}>
			<CommandPalette />
			<Location />
		</MemoryRouter>,
		{ withRouter: false }
	);
}

describe("CommandPalette", () => {
	beforeEach(() => {
		useUiStore.setState({ commandOpen: true });
		usePrivacyStore.setState({ isPrivacy: false });
		useThemeStore.setState({ theme: "light" });
		server.use(http.get(`${env.apiUrl}/assets`, () => HttpResponse.json(mockAssets)));
	});

	it("should list the navigation actions", async () => {
		// GIVEN
		renderPalette();

		// THEN
		expect(await screen.findByRole("option", { name: /Dashboard/ })).toBeInTheDocument();
		expect(screen.getByRole("option", { name: /Assets/ })).toBeInTheDocument();
	});

	it("should navigate from an action", async () => {
		// GIVEN
		const user = userEvent.setup();
		renderPalette();

		// WHEN
		await user.click(await screen.findByRole("option", { name: /^Assets/ }));

		// THEN
		expect(screen.getByTestId("location")).toHaveTextContent("/assets");
		expect(useUiStore.getState().commandOpen).toBe(false);
	});

	it.each([
		["Dashboard", "/dashboard"],
		["Cash flow", "/cashflow"],
		["Profile", "/profile"],
		["New asset", "/assets?new=1"],
		["New target", "/dashboard?target=new"]
	])("should run the %s action", async (name, path) => {
		// GIVEN
		const user = userEvent.setup();
		renderPalette();

		// WHEN
		await user.click(await screen.findByRole("option", { name: new RegExp(`^${name}`) }));

		// THEN
		expect(screen.getByTestId("location")).toHaveTextContent(path);
	});

	it("should search the assets and open the match", async () => {
		// GIVEN
		const user = userEvent.setup();
		renderPalette();
		const input = await screen.findByPlaceholderText("Type a command or search…");

		// WHEN
		await user.type(input, "apartment");
		const match = await screen.findByRole("option", { name: /Apartment Berlin/ });
		await user.click(match);

		// THEN
		expect(screen.getByTestId("location")).toHaveTextContent("/assets?search=Apartment%20Berlin");
	});

	it("should match an asset by its category", async () => {
		// GIVEN
		const user = userEvent.setup();
		renderPalette();
		const input = await screen.findByPlaceholderText("Type a command or search…");

		// WHEN
		await user.type(input, "real estate");

		// THEN
		expect(await screen.findByRole("option", { name: /Apartment Berlin/ })).toBeInTheDocument();
	});

	it("should offer the fallback search while the assets are loading", async () => {
		// GIVEN
		server.use(http.get(`${env.apiUrl}/assets`, () => new Promise(() => undefined)));
		const user = userEvent.setup();
		renderPalette();
		const input = await screen.findByPlaceholderText("Type a command or search…");

		// WHEN
		await user.type(input, "apartment");

		// THEN
		expect(await screen.findByRole("option", { name: /Search assets for/ })).toBeInTheDocument();
	});

	it("should offer a fallback search when nothing matches", async () => {
		// GIVEN
		const user = userEvent.setup();
		renderPalette();
		const input = await screen.findByPlaceholderText("Type a command or search…");

		// WHEN
		await user.type(input, "zzz");
		const fallback = await screen.findByRole("option", { name: /Search assets for/ });
		await user.click(fallback);

		// THEN
		expect(screen.getByTestId("location")).toHaveTextContent("/assets?search=zzz");
	});

	it("should toggle the privacy mode", async () => {
		// GIVEN
		const user = userEvent.setup();
		renderPalette();

		// WHEN
		await user.click(await screen.findByRole("option", { name: /Hide amounts/ }));

		// THEN
		expect(usePrivacyStore.getState().isPrivacy).toBe(true);
	});

	it("should toggle the theme", async () => {
		// GIVEN
		const user = userEvent.setup();
		renderPalette();

		// WHEN
		await user.click(await screen.findByRole("option", { name: /Switch to dark theme/ }));

		// THEN
		expect(useThemeStore.getState().theme).toBe("dark");
	});

	it("should reset the search when it closes", async () => {
		// GIVEN
		const user = userEvent.setup();
		renderPalette();
		const input = await screen.findByPlaceholderText("Type a command or search…");
		await user.type(input, "apartment");

		// WHEN
		await user.keyboard("{Escape}");

		// THEN
		expect(useUiStore.getState().commandOpen).toBe(false);
	});
});
