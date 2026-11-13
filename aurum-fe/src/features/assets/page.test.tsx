import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { createElement } from "react";
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
		fireEvent.click(screen.getByRole("button", { name: "Liabilities" }));

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
		fireEvent.click(
			screen.getAllByRole("button", { name: "Delete Apartment Berlin" })[0] as HTMLElement
		);
		const dialog = await screen.findByRole("alertdialog");
		fireEvent.click(within(dialog).getByRole("button", { name: "Delete forever" }));

		// THEN
		await waitFor(() => expect(deleteCalls).toBe(1));
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
