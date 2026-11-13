import { fireEvent, screen, waitFor } from "@testing-library/react";
import { createElement } from "react";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { env } from "@/lib/env";
import { buildCashflowYear } from "@/test/fixtures/portfolio";
import { server } from "@/test/msw/server";
import { renderWithProviders } from "@/test/render";
import { CashFlowPage } from "./page";

const mockYear = buildCashflowYear({ year: 2026 });

describe("CashFlowPage", () => {
	it("should render the summary and the table", async () => {
		// GIVEN
		const testSubject = CashFlowPage;

		// WHEN
		renderWithProviders(createElement(testSubject), { route: "/cashflow" });

		// THEN
		expect(screen.getByRole("heading", { name: "Cash flow" })).toBeInTheDocument();
		expect((await screen.findAllByText("€66,120", {}, { timeout: 3000 })).length).toBeGreaterThan(0);
		expect(screen.getAllByText("€50,190").length).toBeGreaterThan(0);
		expect(screen.getByText("+€15,930")).toBeInTheDocument();
		expect(screen.getByText("24.1%")).toBeInTheDocument();
		expect(screen.getAllByText("JAN").length).toBeGreaterThan(0);
	});

	it("should edit a month with an optimistic save", async () => {
		// GIVEN
		let updatedBody: unknown;
		server.use(
			http.put(`${env.apiUrl}/cashflows/:year/:month`, async ({ request }) => {
				updatedBody = await request.json();
				return HttpResponse.json({ id: "entry", month: 1, earned: 6000, spent: 4100 });
			})
		);
		const testSubject = CashFlowPage;
		renderWithProviders(createElement(testSubject), { route: "/cashflow" });
		const cells = await screen.findAllByRole("button", {
			name: /Edit JAN earned, currently €5,400.00/
		});

		// WHEN
		fireEvent.click(cells[0] as HTMLElement);
		const input = screen.getByLabelText("Edit JAN earned in EUR");
		fireEvent.change(input, { target: { value: "6000" } });
		fireEvent.blur(input);

		// THEN
		await waitFor(() => expect(updatedBody).toBeDefined());
		expect(updatedBody).toMatchObject({ earned: 6000, spent: 4100 });
	});

	it("should show the year-over-year columns when previous data exists", async () => {
		// GIVEN
		const testSubject = CashFlowPage;

		// WHEN
		renderWithProviders(createElement(testSubject), { route: "/cashflow" });

		// THEN
		expect(await screen.findByText("2025 earned")).toBeInTheDocument();
		expect(screen.getByText("2025 spent")).toBeInTheDocument();
	});

	it("should request the previous year from the URL", async () => {
		// GIVEN
		let requestedYear: string | null = null;
		server.use(
			http.get(`${env.apiUrl}/cashflows/:year`, ({ params }) => {
				requestedYear = String(params.year);
				return HttpResponse.json({ ...mockYear, year: Number(params.year) });
			})
		);
		const testSubject = CashFlowPage;
		renderWithProviders(createElement(testSubject), { route: "/cashflow" });
		await screen.findAllByText("€66,120", {}, { timeout: 3000 });

		// WHEN
		fireEvent.click(screen.getByRole("button", { name: "Previous year" }));

		// THEN
		await waitFor(() => expect(requestedYear).toBe("2025"));
	});

	it("should show an alert when the year fails to load", async () => {
		// GIVEN
		server.use(
			http.get(`${env.apiUrl}/cashflows/:year`, () =>
				HttpResponse.json({ message: "boom" }, { status: 500 })
			)
		);
		const testSubject = CashFlowPage;

		// WHEN
		renderWithProviders(createElement(testSubject), { route: "/cashflow" });

		// THEN
		expect(await screen.findByRole("alert")).toHaveTextContent("Could not load the cash flow");
	});
});
