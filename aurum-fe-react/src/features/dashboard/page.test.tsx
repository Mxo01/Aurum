import { fireEvent, screen, waitFor } from "@testing-library/react";
import { createElement } from "react";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { env } from "@/lib/env";
import { buildChart } from "@/test/fixtures/portfolio";
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
