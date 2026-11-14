import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { createElement } from "react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { env } from "@/lib/env";
import { buildAsset } from "@/test/fixtures/portfolio";
import { server } from "@/test/msw/server";
import { renderWithProviders } from "@/test/render";
import { AssetsPage } from "./page";

const mockAssets = [
	buildAsset({
		id: "a1",
		name: "Apartment Berlin",
		categoryName: "Real Estate",
		categoryIcon: "pi-home",
		type: "ASSET",
		latestValue: 430000,
		latestValueBase: 430000,
		previousValue: 412000
	}),
	buildAsset({
		id: "a2",
		name: "Global Equity ETF",
		categoryName: "Investments",
		categoryIcon: "pi-chart-line",
		type: "ASSET",
		latestValue: 128450,
		latestValueBase: 128450,
		previousValue: 119300
	}),
	buildAsset({
		id: "a3",
		name: "Mortgage",
		categoryName: "Mortgage",
		categoryIcon: "pi-building",
		type: "LIABILITY",
		latestValue: 268000,
		latestValueBase: 268000,
		previousValue: 273500,
		liabilityType: "MANUAL"
	})
];

function useAssetsFixture() {
	server.use(http.get(`${env.apiUrl}/assets`, () => HttpResponse.json(mockAssets)));
}

describe("AssetsPage", () => {
	it("should render the totals and the grouped list", async () => {
		// GIVEN
		useAssetsFixture();
		const testSubject = AssetsPage;

		// WHEN
		renderWithProviders(createElement(testSubject), { route: "/assets" });

		// THEN
		expect(await screen.findByText("€558,450.00")).toBeInTheDocument();
		expect(screen.getByText("Type")).toBeInTheDocument();
		expect(screen.getAllByText("Liability").length).toBeGreaterThan(0);
		const mortgageHeader = screen
			.getAllByRole("button")
			.find(
				button => button.hasAttribute("aria-expanded") && button.textContent?.includes("Mortgage")
			);
		expect(mortgageHeader).toBeDefined();
		expect(within(mortgageHeader as HTMLElement).queryByText("Liability")).not.toBeInTheDocument();
		expect(screen.getAllByRole("button", { name: /^Mark .+ as favorite$/ }).length).toBeGreaterThan(
			0
		);
		expect(screen.getAllByText("€268,000.00").length).toBeGreaterThan(0);
		expect(screen.getByText("€290,450.00")).toBeInTheDocument();
		expect(screen.getAllByText("Apartment Berlin").length).toBeGreaterThan(0);
		expect(screen.getAllByText("Mortgage").length).toBeGreaterThan(0);
	});

	it("should filter the list by search", async () => {
		// GIVEN
		useAssetsFixture();
		const testSubject = AssetsPage;
		renderWithProviders(createElement(testSubject), { route: "/assets" });
		await screen.findAllByText("Apartment Berlin");

		// WHEN
		fireEvent.change(screen.getByLabelText("Search assets"), {
			target: { value: "mortgage" }
		});

		// THEN
		expect(screen.queryByText("Apartment Berlin")).not.toBeInTheDocument();
		expect(screen.getAllByText("Mortgage").length).toBeGreaterThan(0);
	});

	it("should filter liabilities", async () => {
		// GIVEN
		useAssetsFixture();
		const testSubject = AssetsPage;
		renderWithProviders(createElement(testSubject), { route: "/assets" });
		await screen.findAllByText("Apartment Berlin");

		// WHEN
		fireEvent.mouseDown(screen.getByRole("tab", { name: "Liabilities" }));

		// THEN
		expect(screen.queryByText("Apartment Berlin")).not.toBeInTheDocument();
		expect(screen.getAllByText("Mortgage").length).toBeGreaterThan(0);
	});

	it("should collapse a category", async () => {
		// GIVEN
		useAssetsFixture();
		const testSubject = AssetsPage;
		renderWithProviders(createElement(testSubject), { route: "/assets" });
		await screen.findAllByText("Apartment Berlin");

		// WHEN
		fireEvent.click(screen.getAllByRole("button", { name: /Real Estate/ })[0] as HTMLElement);

		// THEN
		expect(screen.queryByText("Apartment Berlin")).not.toBeInTheDocument();
	});

	it("should save an inline value edit as a snapshot", async () => {
		// GIVEN
		useAssetsFixture();
		let snapshotCalls = 0;
		server.use(
			http.post(`${env.apiUrl}/snapshots`, () => {
				snapshotCalls += 1;
				return HttpResponse.json({ id: "snapshot" });
			})
		);
		const testSubject = AssetsPage;
		renderWithProviders(createElement(testSubject), { route: "/assets" });
		const valueButtons = await screen.findAllByRole("button", {
			name: /Edit Apartment Berlin, currently €430,000.00/
		});

		// WHEN
		fireEvent.click(valueButtons[0] as HTMLElement);
		const input = screen.getByLabelText("Edit Apartment Berlin in EUR");
		fireEvent.change(input, { target: { value: "440000" } });
		fireEvent.blur(input);

		// THEN
		await waitFor(() => expect(snapshotCalls).toBe(1));
	});

	it("should delete an asset after confirmation", async () => {
		// GIVEN
		useAssetsFixture();
		let deleteCalls = 0;
		server.use(
			http.delete(`${env.apiUrl}/assets/:id`, () => {
				deleteCalls += 1;
				return new HttpResponse(null, { status: 200 });
			})
		);
		const testSubject = AssetsPage;
		renderWithProviders(createElement(testSubject), { route: "/assets" });
		await screen.findAllByText("Apartment Berlin");

		// WHEN
		const user = userEvent.setup();
		await user.click(
			screen.getAllByRole("button", { name: "Actions for Apartment Berlin" })[0] as HTMLElement
		);
		await user.click(await screen.findByRole("menuitem", { name: "Delete" }));
		const dialog = await screen.findByRole("alertdialog");
		fireEvent.click(within(dialog).getByRole("button", { name: "Delete forever" }));

		// THEN
		await waitFor(() => expect(deleteCalls).toBe(1));
	});

	it("should hide zero-valued assets by default", async () => {
		// GIVEN
		const mockZeroAsset = buildAsset({
			id: "a4",
			name: "Empty Wallet",
			categoryName: "Cash",
			categoryIcon: "pi-wallet",
			latestValue: 0,
			latestValueBase: 0,
			previousValue: 0
		});
		server.use(
			http.get(`${env.apiUrl}/assets`, () => HttpResponse.json([...mockAssets, mockZeroAsset]))
		);
		const testSubject = AssetsPage;
		renderWithProviders(createElement(testSubject), { route: "/assets" });
		await screen.findAllByText("Apartment Berlin");

		// WHEN
		expect(screen.queryByText("Empty Wallet")).not.toBeInTheDocument();
		fireEvent.click(screen.getByLabelText("Hide zero values"));

		// THEN
		expect((await screen.findAllByText("Empty Wallet")).length).toBeGreaterThan(0);
	});

	it("should send the full asset when toggling the dashboard tracking", async () => {
		// GIVEN
		useAssetsFixture();
		let requestBody: Record<string, unknown> | null = null;
		server.use(
			http.put(`${env.apiUrl}/assets/:id`, async ({ request }) => {
				requestBody = (await request.json()) as Record<string, unknown>;
				return HttpResponse.json(mockAssets[0]);
			})
		);
		const testSubject = AssetsPage;
		renderWithProviders(createElement(testSubject), { route: "/assets" });
		await screen.findAllByText("Apartment Berlin");

		// WHEN
		const [dashboardToggle] = screen.getAllByRole("button", {
			name: "Mark Apartment Berlin as favorite"
		});
		fireEvent.click(dashboardToggle!);

		// THEN
		await waitFor(() => expect(requestBody).not.toBeNull());
		expect(requestBody).toMatchObject({ name: "Apartment Berlin", isFavorite: true });
	});

	it("should open the new asset form from the URL", async () => {
		// GIVEN
		useAssetsFixture();
		const testSubject = AssetsPage;

		// WHEN
		renderWithProviders(createElement(testSubject), { route: "/assets?new=1" });

		// THEN
		expect(await screen.findByRole("heading", { name: "New asset" })).toBeInTheDocument();
	});

	it("should show the empty state without assets", async () => {
		// GIVEN
		server.use(http.get(`${env.apiUrl}/assets`, () => HttpResponse.json([])));
		const testSubject = AssetsPage;

		// WHEN
		renderWithProviders(createElement(testSubject), { route: "/assets" });

		// THEN
		expect(await screen.findByText("No assets yet")).toBeInTheDocument();
	});
});

describe("AssetsPage interactions", () => {
	it("should edit an asset from the row actions", async () => {
		// GIVEN
		useAssetsFixture();
		renderWithProviders(createElement(AssetsPage), { route: "/assets" });
		await screen.findAllByText("Apartment Berlin");

		// WHEN
		const user = userEvent.setup();
		await user.click(screen.getAllByRole("button", { name: "Actions for Apartment Berlin" })[0]!);
		await user.click((await screen.findAllByRole("menuitem", { name: /Edit/ }))[0]!);

		// THEN
		expect(await screen.findByRole("heading", { name: "Edit asset" })).toBeInTheDocument();
	});

	it("should open the history from the row actions", async () => {
		// GIVEN
		useAssetsFixture();
		renderWithProviders(createElement(AssetsPage), { route: "/assets" });
		await screen.findAllByText("Apartment Berlin");

		// WHEN
		const user = userEvent.setup();
		await user.click(screen.getAllByRole("button", { name: "Actions for Apartment Berlin" })[0]!);
		await user.click((await screen.findAllByRole("menuitem", { name: /History/ }))[0]!);

		// THEN
		expect(await screen.findByRole("heading", { name: /History/ })).toBeInTheDocument();
	});

	it("should clear the search", async () => {
		// GIVEN
		useAssetsFixture();
		renderWithProviders(createElement(AssetsPage), { route: "/assets" });
		await screen.findAllByText("Apartment Berlin");
		const search = screen.getByLabelText("Search assets");
		fireEvent.change(search, { target: { value: "zzz" } });

		// WHEN
		fireEvent.change(search, { target: { value: "" } });

		// THEN
		expect(screen.getAllByText("Apartment Berlin").length).toBeGreaterThan(0);
	});

	it("should show the empty search state", async () => {
		// GIVEN
		useAssetsFixture();
		renderWithProviders(createElement(AssetsPage), { route: "/assets" });
		await screen.findAllByText("Apartment Berlin");

		// WHEN
		fireEvent.change(screen.getByLabelText("Search assets"), { target: { value: "zzz" } });

		// THEN
		expect(screen.getByText(/No assets match/)).toBeInTheDocument();
	});

	it("should open the categories sheet", async () => {
		// GIVEN
		useAssetsFixture();
		renderWithProviders(createElement(AssetsPage), { route: "/assets" });
		await screen.findAllByText("Apartment Berlin");

		// WHEN
		fireEvent.click(screen.getByRole("button", { name: /Categories/ }));

		// THEN
		expect(await screen.findByRole("heading", { name: "Categories" })).toBeInTheDocument();
	});
});

describe("AssetsPage lifecycle", () => {
	it("should show the loading skeletons", () => {
		// GIVEN
		server.use(http.get(`${env.apiUrl}/assets`, () => new Promise(() => undefined)));
		const testSubject = AssetsPage;

		// WHEN
		const { container } = renderWithProviders(createElement(testSubject), { route: "/assets" });

		// THEN
		expect(container.querySelectorAll(".animate-pulse").length).toBeGreaterThan(0);
	});

	it("should rename an asset inline", async () => {
		// GIVEN
		useAssetsFixture();
		let updatedBody: unknown;
		server.use(
			http.put(`${env.apiUrl}/assets/:id`, async ({ request }) => {
				updatedBody = await request.json();
				return HttpResponse.json(mockAssets[0]);
			})
		);
		renderWithProviders(createElement(AssetsPage), { route: "/assets" });
		await screen.findAllByText("Apartment Berlin");

		// WHEN
		fireEvent.click(
			screen.getAllByRole("button", {
				name: /Edit Apartment Berlin, currently Apartment Berlin/
			})[0]!
		);
		const input = screen.getByLabelText("Edit Apartment Berlin");
		fireEvent.change(input, { target: { value: "Berlin Flat" } });
		fireEvent.keyDown(input, { key: "Enter" });

		// THEN
		await waitFor(() => expect(updatedBody).toBeDefined());
		expect(updatedBody).toMatchObject({ name: "Berlin Flat" });
	});

	it("should toggle the favorite from the mobile card", async () => {
		// GIVEN
		useAssetsFixture();
		let updatedBody: unknown;
		server.use(
			http.put(`${env.apiUrl}/assets/:id`, async ({ request }) => {
				updatedBody = await request.json();
				return HttpResponse.json(mockAssets[0]);
			})
		);
		renderWithProviders(createElement(AssetsPage), { route: "/assets" });
		await screen.findAllByText("Apartment Berlin");

		// WHEN
		const toggles = screen.getAllByRole("button", { name: "Mark Apartment Berlin as favorite" });
		fireEvent.click(toggles[toggles.length - 1]!);

		// THEN
		await waitFor(() => expect(updatedBody).toBeDefined());
		expect(updatedBody).toMatchObject({ isFavorite: true });
	});

	it("should ignore a value edit without an id", async () => {
		// GIVEN
		const noIdAsset = buildAsset({ id: "", name: "No id", latestValue: 10, latestValueBase: 10 });
		server.use(http.get(`${env.apiUrl}/assets`, () => HttpResponse.json([noIdAsset])));
		const testSubject = AssetsPage;
		renderWithProviders(createElement(testSubject), { route: "/assets" });
		await screen.findAllByText("No id");

		// WHEN
		fireEvent.click(screen.getAllByRole("button", { name: /Edit No id, currently €10.00/ })[0]!);
		const input = screen.getByLabelText("Edit No id in EUR");
		fireEvent.change(input, { target: { value: "20" } });
		fireEvent.blur(input);

		// THEN
		expect(screen.getAllByText("No id").length).toBeGreaterThan(0);
	});

	it("should close the new asset sheet and clear the URL", async () => {
		// GIVEN
		useAssetsFixture();
		renderWithProviders(createElement(AssetsPage), { route: "/assets?new=1" });
		expect(await screen.findByRole("heading", { name: "New asset" })).toBeInTheDocument();

		// WHEN
		fireEvent.click(screen.getByRole("button", { name: "Close" }));

		// THEN
		await waitFor(() =>
			expect(screen.queryByRole("heading", { name: "New asset" })).not.toBeInTheDocument()
		);
	});

	it("should close the history and the categories sheets", async () => {
		// GIVEN
		useAssetsFixture();
		renderWithProviders(createElement(AssetsPage), { route: "/assets" });
		await screen.findAllByText("Apartment Berlin");

		// WHEN
		const user = userEvent.setup();
		await user.click(screen.getAllByRole("button", { name: "Actions for Apartment Berlin" })[0]!);
		await user.click((await screen.findAllByRole("menuitem", { name: /History/ }))[0]!);
		const history = await screen.findByRole("heading", { name: /History/ });
		expect(history).toBeInTheDocument();

		// THEN
		fireEvent.click(screen.getByRole("button", { name: "Close" }));
		await waitFor(() =>
			expect(screen.queryByRole("heading", { name: /History/ })).not.toBeInTheDocument()
		);

		// WHEN
		fireEvent.click(screen.getByRole("button", { name: /Categories/ }));
		expect(await screen.findByRole("heading", { name: "Categories" })).toBeInTheDocument();
		fireEvent.click(screen.getByRole("button", { name: "Close" }));

		// THEN
		await waitFor(() =>
			expect(screen.queryByRole("heading", { name: "Categories" })).not.toBeInTheDocument()
		);
	});
});

it("should collapse the category from the mobile header", async () => {
	// GIVEN
	useAssetsFixture();
	renderWithProviders(createElement(AssetsPage), { route: "/assets" });
	await screen.findAllByText("Apartment Berlin");
	const headers = screen.getAllByRole("button", { name: /Real Estate/ });

	// WHEN
	fireEvent.click(headers[headers.length - 1]!);

	// THEN
	expect(screen.getAllByRole("button", { name: /Real Estate/ }).length).toBeGreaterThan(0);
});

it("should edit the name and the value from the mobile card", async () => {
	// GIVEN
	useAssetsFixture();
	let updatedBody: unknown;
	server.use(
		http.put(`${env.apiUrl}/assets/:id`, async ({ request }) => {
			updatedBody = await request.json();
			return HttpResponse.json(mockAssets[0]);
		})
	);
	renderWithProviders(createElement(AssetsPage), { route: "/assets" });
	await screen.findAllByText("Apartment Berlin");

	// WHEN
	const names = screen.getAllByRole("button", {
		name: /Edit Apartment Berlin, currently Apartment Berlin/
	});
	fireEvent.click(names[names.length - 1]!);
	const input = screen.getByLabelText("Edit Apartment Berlin");
	fireEvent.change(input, { target: { value: "Berlin Flat" } });
	fireEvent.keyDown(input, { key: "Enter" });

	// THEN
	await waitFor(() => expect(updatedBody).toBeDefined());
	expect(updatedBody).toMatchObject({ name: "Berlin Flat" });
});

it("should close the categories sheet opened from the URL", async () => {
	// GIVEN
	useAssetsFixture();
	renderWithProviders(createElement(AssetsPage), { route: "/assets?categories=1" });
	expect(await screen.findByRole("heading", { name: "Categories" })).toBeInTheDocument();

	// WHEN
	fireEvent.click(screen.getByRole("button", { name: "Close" }));

	// THEN
	await waitFor(() =>
		expect(screen.queryByRole("heading", { name: "Categories" })).not.toBeInTheDocument()
	);
});

it("should edit the value from the mobile card", async () => {
	// GIVEN
	useAssetsFixture();
	let snapshotBody: unknown;
	server.use(
		http.post(`${env.apiUrl}/snapshots`, async ({ request }) => {
			snapshotBody = await request.json();
			return HttpResponse.json({});
		})
	);
	renderWithProviders(createElement(AssetsPage), { route: "/assets" });
	await screen.findAllByText("Apartment Berlin");

	// WHEN
	const values = screen.getAllByRole("button", {
		name: /Edit Apartment Berlin, currently €430,000.00/
	});
	fireEvent.click(values[values.length - 1]!);
	const input = screen.getByLabelText("Edit Apartment Berlin in EUR");
	fireEvent.change(input, { target: { value: "440000" } });
	fireEvent.blur(input);

	// THEN
	await waitFor(() => expect(snapshotBody).toBeDefined());
	expect(snapshotBody).toMatchObject({ amountOriginalCurrency: 440000 });
});

it("should open the new asset form from the header", async () => {
	// GIVEN
	useAssetsFixture();
	renderWithProviders(createElement(AssetsPage), { route: "/assets" });
	await screen.findAllByText("Apartment Berlin");

	// WHEN
	fireEvent.click(screen.getByRole("button", { name: /Add asset/ }));

	// THEN
	expect(await screen.findByRole("heading", { name: "New asset" })).toBeInTheDocument();
});

it("should render a missing value as zero", async () => {
	// GIVEN
	server.use(
		http.get(`${env.apiUrl}/assets`, () =>
			HttpResponse.json([
				buildAsset({ id: "a1", name: "Empty", latestValue: null, latestValueBase: null })
			])
		)
	);
	const testSubject = AssetsPage;

	// WHEN
	renderWithProviders(createElement(testSubject), { route: "/assets" });

	// THEN
	expect((await screen.findAllByText("€0.00")).length).toBeGreaterThan(0);
});

it("should expand a collapsed category", async () => {
	// GIVEN
	useAssetsFixture();
	renderWithProviders(createElement(AssetsPage), { route: "/assets" });
	await screen.findAllByText("Apartment Berlin");
	const headers = screen.getAllByRole("button", { name: /Real Estate/ });
	fireEvent.click(headers[0]!);

	// WHEN
	fireEvent.click(headers[0]!);

	// THEN
	expect(screen.getAllByText("Apartment Berlin").length).toBeGreaterThan(0);
});

it("should cancel the delete confirmation", async () => {
	// GIVEN
	useAssetsFixture();
	renderWithProviders(createElement(AssetsPage), { route: "/assets" });
	await screen.findAllByText("Apartment Berlin");

	// WHEN
	const user = userEvent.setup();
	await user.click(
		screen.getAllByRole("button", { name: "Actions for Apartment Berlin" })[0] as HTMLElement
	);
	await user.click(await screen.findByRole("menuitem", { name: "Delete" }));
	expect(await screen.findByText("Delete this asset?")).toBeInTheDocument();
	fireEvent.click(screen.getByRole("button", { name: "Cancel" }));

	// THEN
	await waitFor(() => expect(screen.queryByText("Delete this asset?")).not.toBeInTheDocument());
});

it("should ignore a delete without an id", async () => {
	// GIVEN
	const noIdAsset = buildAsset({ id: "", name: "No id" });
	server.use(http.get(`${env.apiUrl}/assets`, () => HttpResponse.json([noIdAsset])));
	renderWithProviders(createElement(AssetsPage), { route: "/assets" });
	await screen.findAllByText("No id");

	// WHEN
	const user = userEvent.setup();
	await user.click(screen.getAllByRole("button", { name: "Actions for No id" })[0] as HTMLElement);
	await user.click(await screen.findByRole("menuitem", { name: "Delete" }));
	fireEvent.click(screen.getByRole("button", { name: "Delete forever" }));

	// THEN
	expect(screen.getAllByText("No id").length).toBeGreaterThan(0);
});
