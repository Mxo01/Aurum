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
		expect((await screen.findAllByText("€66,120", {}, { timeout: 3000 })).length).toBeGreaterThan(
			0
		);
		expect(screen.getAllByText("€50,190").length).toBeGreaterThan(0);
		expect(screen.getByText("+€15,930")).toBeInTheDocument();
		expect(screen.getByText("24.1%")).toBeInTheDocument();
		expect(screen.getAllByText("JAN").length).toBeGreaterThan(0);
		expect(screen.queryAllByRole("progressbar")).toHaveLength(0);
		expect(screen.queryByText(/with recorded data/)).not.toBeInTheDocument();
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

it("should navigate to the previous year", async () => {
	// GIVEN
	let requestedYear: string | null = null;
	server.use(
		http.get(`${env.apiUrl}/cashflows/:year`, ({ params }) => {
			requestedYear = String(params.year);
			return HttpResponse.json({ ...mockYear, year: Number(params.year) });
		})
	);
	renderWithProviders(createElement(CashFlowPage), { route: "/cashflow" });
	await screen.findAllByText("JAN");

	// WHEN
	fireEvent.click(screen.getByRole("button", { name: "Previous year" }));

	// THEN
	await waitFor(() => expect(requestedYear).toBe("2025"));
});

it("should retry after a failed load", async () => {
	// GIVEN
	let calls = 0;
	server.use(
		http.get(`${env.apiUrl}/cashflows/:year`, () => {
			calls += 1;
			return calls === 1
				? HttpResponse.json({ message: "boom" }, { status: 500 })
				: HttpResponse.json(mockYear);
		})
	);
	renderWithProviders(createElement(CashFlowPage), { route: "/cashflow" });

	// WHEN
	fireEvent.click(await screen.findByRole("button", { name: "Try again" }));

	// THEN
	await waitFor(() => expect(calls).toBeGreaterThan(1));
	expect((await screen.findAllByText("JAN")).length).toBeGreaterThan(0);
});

it("should edit the spent value in the table", async () => {
	// GIVEN
	let updatedBody: unknown;
	server.use(
		http.put(`${env.apiUrl}/cashflows/:year/:month`, async ({ request }) => {
			updatedBody = await request.json();
			return HttpResponse.json({ id: "entry", month: 1, earned: 5400, spent: 4200 });
		})
	);
	renderWithProviders(createElement(CashFlowPage), { route: "/cashflow" });
	const cells = await screen.findAllByRole("button", {
		name: /Edit JAN spent, currently/
	});

	// WHEN
	fireEvent.click(cells[0] as HTMLElement);
	const input = screen.getByLabelText("Edit JAN spent in EUR");
	fireEvent.change(input, { target: { value: "4200" } });
	fireEvent.blur(input);

	// THEN
	await waitFor(() => expect(updatedBody).toBeDefined());
	expect(updatedBody).toMatchObject({ earned: 5400, spent: 4200 });
});

it("should edit the mobile card values", async () => {
	// GIVEN
	let updatedBody: unknown;
	server.use(
		http.put(`${env.apiUrl}/cashflows/:year/:month`, async ({ request }) => {
			updatedBody = await request.json();
			return HttpResponse.json({ id: "entry", month: 1, earned: 5500, spent: 4600 });
		})
	);
	renderWithProviders(createElement(CashFlowPage), { route: "/cashflow" });
	const earned = await screen.findAllByRole("button", {
		name: /Edit JAN earned, currently €5,400.00/
	});

	// WHEN
	fireEvent.click(earned[earned.length - 1] as HTMLElement);
	const input = screen.getByLabelText("Edit JAN earned in EUR");
	fireEvent.change(input, { target: { value: "5500" } });
	fireEvent.blur(input);

	// THEN
	await waitFor(() => expect(updatedBody).toBeDefined());
	expect(updatedBody).toMatchObject({ earned: 5500 });
});

it("should navigate to the next year when available", async () => {
	// GIVEN
	let requestedYear: string | null = null;
	server.use(
		http.get(`${env.apiUrl}/cashflows/:year`, ({ params }) => {
			requestedYear = String(params.year);
			return HttpResponse.json({
				...mockYear,
				year: Number(params.year),
				availableYears: [2025, 2026]
			});
		})
	);
	renderWithProviders(createElement(CashFlowPage), { route: "/cashflow?year=2025" });
	await screen.findAllByText("JAN");

	// WHEN
	fireEvent.click(screen.getByRole("button", { name: "Next year" }));

	// THEN
	await waitFor(() => expect(requestedYear).toBe("2026"));
});

it("should edit the mobile spent value", async () => {
	// GIVEN
	let updatedBody: unknown;
	server.use(
		http.put(`${env.apiUrl}/cashflows/:year/:month`, async ({ request }) => {
			updatedBody = await request.json();
			return HttpResponse.json({ id: "entry", month: 1, earned: 5400, spent: 4300 });
		})
	);
	renderWithProviders(createElement(CashFlowPage), { route: "/cashflow" });
	const spent = await screen.findAllByRole("button", { name: /Edit JAN spent, currently/ });

	// WHEN
	fireEvent.click(spent[spent.length - 1] as HTMLElement);
	const input = screen.getByLabelText("Edit JAN spent in EUR");
	fireEvent.change(input, { target: { value: "4300" } });
	fireEvent.blur(input);

	// THEN
	await waitFor(() => expect(updatedBody).toBeDefined());
	expect(updatedBody).toMatchObject({ spent: 4300 });
});

it("should fall back to the current year for an invalid year", async () => {
	// GIVEN
	let requestedYear: string | null = null;
	server.use(
		http.get(`${env.apiUrl}/cashflows/:year`, ({ params }) => {
			requestedYear = String(params.year);
			return HttpResponse.json({ ...mockYear, year: Number(params.year) });
		})
	);

	// WHEN
	renderWithProviders(createElement(CashFlowPage), { route: "/cashflow?year=abc" });

	// THEN
	await waitFor(() => expect(requestedYear).toBe("2026"));
});

it("should show the no previous year message", async () => {
	// GIVEN
	server.use(
		http.get(`${env.apiUrl}/cashflows/:year`, () =>
			HttpResponse.json({ ...mockYear, previousYearEntries: null })
		)
	);
	const testSubject = CashFlowPage;

	// WHEN
	renderWithProviders(createElement(testSubject), { route: "/cashflow" });

	// THEN
	expect(await screen.findByText(/No data for 2025/)).toBeInTheDocument();
});

it("should render negative and empty months", async () => {
	// GIVEN
	server.use(
		http.get(`${env.apiUrl}/cashflows/:year`, () =>
			HttpResponse.json({
				year: 2026,
				entries: [
					{ id: "e1", month: 1, earned: 1000, spent: 1500 },
					{ id: "e2", month: 2, earned: 0, spent: 0 }
				],
				previousYearEntries: [{ id: "p1", month: 1, earned: 500, spent: 400 }],
				availableYears: [2025, 2026]
			})
		)
	);
	const testSubject = CashFlowPage;

	// WHEN
	renderWithProviders(createElement(testSubject), { route: "/cashflow" });

	// THEN
	expect((await screen.findAllByText("JAN")).length).toBeGreaterThan(0);
	expect(screen.getAllByText("FEB").length).toBeGreaterThan(0);
});
