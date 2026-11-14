import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createElement } from "react";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { env } from "@/lib/env";
import { buildChart, buildSummary, buildTarget } from "@/test/fixtures/portfolio";
import { server } from "@/test/msw/server";
import { renderWithProviders } from "@/test/render";
import { DashboardPage } from "./page";

describe("DashboardPage", () => {
	it("should render the overview with all the tiles", async () => {
		// GIVEN
		const testSubject = DashboardPage;

		// WHEN
		renderWithProviders(createElement(testSubject), { route: "/dashboard" });

		// THEN
		expect(screen.getByRole("heading", { name: "Overview" })).toBeInTheDocument();
		expect(await screen.findAllByText("€570,000.00")).not.toHaveLength(0);
		expect(screen.getByText("Projected net worth")).toBeInTheDocument();
		expect(screen.getByText("Asset growth")).toBeInTheDocument();
		expect(screen.getByText("Total liabilities")).toBeInTheDocument();
		expect(screen.getByText("FX impact")).toBeInTheDocument();
		expect(await screen.findByText("Financial target")).toBeInTheDocument();
		expect(screen.getByText("Top assets")).toBeInTheDocument();
	});

	it("should request the chart for the year in the URL", async () => {
		// GIVEN
		let requestedYear: string | null = null;
		server.use(
			http.get(`${env.apiUrl}/analytics/chart`, ({ request }) => {
				requestedYear = new URL(request.url).searchParams.get("year");
				return HttpResponse.json(buildChart());
			})
		);
		const testSubject = DashboardPage;

		// WHEN
		renderWithProviders(createElement(testSubject), { route: "/dashboard?year=2025" });

		// THEN
		await waitFor(() => expect(requestedYear).toBe("2025"));
	});

	it("should show an alert and retry when the summary fails", async () => {
		// GIVEN
		let calls = 0;
		server.use(
			http.get(`${env.apiUrl}/analytics/summary`, () => {
				calls += 1;
				return HttpResponse.json({ message: "boom" }, { status: 500 });
			})
		);
		const testSubject = DashboardPage;

		// WHEN
		renderWithProviders(createElement(testSubject), { route: "/dashboard" });

		// THEN
		const alert = await screen.findByRole("alert");
		expect(alert).toHaveTextContent("Could not load your dashboard");

		const callsBeforeRetry = calls;
		fireEvent.click(screen.getByRole("button", { name: "Try again" }));
		await waitFor(() => expect(calls).toBeGreaterThan(callsBeforeRetry));
	});
});

it("should clear the target form when the sheet closes", async () => {
	// GIVEN
	const user = userEvent.setup();
	const testSubject = DashboardPage;
	renderWithProviders(createElement(testSubject), { route: "/dashboard" });
	await screen.findByText("Financial target");

	await user.click(screen.getByRole("button", { name: "Target actions" }));
	await user.click(await screen.findByRole("menuitem", { name: "Add target" }));
	const nameInput = await screen.findByLabelText("Name");
	await user.type(nameInput, "Leftover target");
	await user.click(screen.getByRole("button", { name: "Close" }));
	await waitFor(() => expect(screen.queryByLabelText("Name")).not.toBeInTheDocument());

	// WHEN
	await user.click(screen.getByRole("button", { name: "Target actions" }));
	await user.click(await screen.findByRole("menuitem", { name: "Add target" }));

	// THEN
	expect(await screen.findByLabelText("Name")).toHaveValue("");
});

it("should list every target", async () => {
	// GIVEN
	server.use(
		http.get(`${env.apiUrl}/targets`, () =>
			HttpResponse.json([
				buildTarget({ id: "t1", name: "House" }),
				buildTarget({ id: "t2", name: "Car" }),
				buildTarget({ id: "t3", name: "Trip" })
			])
		)
	);
	const testSubject = DashboardPage;

	// WHEN
	renderWithProviders(createElement(testSubject), { route: "/dashboard" });

	// THEN
	expect(await screen.findByText("House")).toBeInTheDocument();
	expect(screen.getByText("Car")).toBeInTheDocument();
	expect(screen.getByText("Trip")).toBeInTheDocument();
});

it("should retry the dashboard after a failure", async () => {
	// GIVEN
	let summaryCalls = 0;
	server.use(
		http.get(`${env.apiUrl}/analytics/summary`, () => {
			summaryCalls += 1;
			return summaryCalls === 1
				? HttpResponse.json({ message: "boom" }, { status: 500 })
				: HttpResponse.json(buildSummary());
		})
	);
	const testSubject = DashboardPage;
	renderWithProviders(createElement(testSubject), { route: "/dashboard" });

	// WHEN
	fireEvent.click(await screen.findByRole("button", { name: "Try again" }));

	// THEN
	await waitFor(() => expect(summaryCalls).toBeGreaterThan(1));
});

it("should ignore an invalid year in the URL", async () => {
	// GIVEN
	let requestedYear: string | null = null;
	server.use(
		http.get(`${env.apiUrl}/analytics/chart`, ({ request }) => {
			requestedYear = new URL(request.url).searchParams.get("year");
			return HttpResponse.json(buildChart());
		})
	);
	const testSubject = DashboardPage;

	// WHEN
	renderWithProviders(createElement(testSubject), { route: "/dashboard?year=abc" });

	// THEN
	await waitFor(() => expect(requestedYear).toBeNull());
});

it("should put the selected year in the URL", async () => {
	// GIVEN
	let requestedYear: string | null = null;
	server.use(
		http.get(`${env.apiUrl}/analytics/chart`, ({ request }) => {
			requestedYear = new URL(request.url).searchParams.get("year");
			return HttpResponse.json(buildChart());
		})
	);
	const user = userEvent.setup();
	renderWithProviders(createElement(DashboardPage), { route: "/dashboard" });

	// WHEN
	await user.click(screen.getByRole("combobox", { name: "Select year" }));
	await user.click(await screen.findByRole("option", { name: "2025" }));

	// THEN
	await waitFor(() => expect(requestedYear).toBe("2025"));
});

it("should clear the year from the URL", async () => {
	// GIVEN
	let requestedYear: string | null | undefined;
	server.use(
		http.get(`${env.apiUrl}/analytics/chart`, ({ request }) => {
			requestedYear = new URL(request.url).searchParams.get("year");
			return HttpResponse.json(buildChart());
		})
	);
	const user = userEvent.setup();
	renderWithProviders(createElement(DashboardPage), { route: "/dashboard?year=2025" });
	await waitFor(() => expect(requestedYear).toBe("2025"));

	// WHEN
	await user.click(screen.getByRole("combobox", { name: "Select year" }));
	await user.click(await screen.findByRole("option", { name: "All time" }));

	// THEN
	await waitFor(() => expect(requestedYear).toBeNull());
});

it("should tolerate an empty chart and empty projections", async () => {
	// GIVEN
	server.use(
		http.get(`${env.apiUrl}/analytics/chart`, () =>
			HttpResponse.json({
				labels: [],
				totalNetWorth: [],
				totalAssetsOnly: [],
				favoriteAssetsValues: {},
				favoriteAssetsTypes: {}
			})
		),
		http.get(`${env.apiUrl}/analytics/projections`, () => HttpResponse.json({}))
	);

	// WHEN
	renderWithProviders(createElement(DashboardPage), { route: "/dashboard" });

	// THEN
	expect(await screen.findByText("Not enough data yet")).toBeInTheDocument();
	const projected = screen.getByText("Projected net worth").closest(".bg-card");
	expect(within(projected as HTMLElement).getByText("€0")).toBeInTheDocument();
});

it("should tolerate a null summary", async () => {
	// GIVEN
	server.use(http.get(`${env.apiUrl}/analytics/summary`, () => HttpResponse.json(null)));

	// WHEN
	renderWithProviders(createElement(DashboardPage), { route: "/dashboard" });

	// THEN
	expect(await screen.findByText("Total liabilities")).toBeInTheDocument();
	const liabilities = screen.getByText("Total liabilities").closest(".bg-card");
	const fx = screen.getByText("FX impact").closest(".bg-card");
	await waitFor(() =>
		expect(within(liabilities as HTMLElement).getByText("€0")).toBeInTheDocument()
	);
	expect(within(fx as HTMLElement).getByText("€0")).toBeInTheDocument();
});
