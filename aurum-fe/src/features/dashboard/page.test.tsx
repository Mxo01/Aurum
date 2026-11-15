import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { createElement } from "react";
import { http, HttpResponse, type RequestHandler } from "msw";
import { describe, expect, it } from "vitest";
import { env } from "@/lib/env";
import {
	buildChart,
	buildLiabilityClassificationIncident,
	buildSummary,
	buildTarget
} from "@/test/fixtures/portfolio";
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

it("should report zero liabilities and the real net worth for the liability classification incident", async () => {
	// GIVEN
	const incident = buildLiabilityClassificationIncident();
	server.use(
		http.get(`${env.apiUrl}/analytics/summary`, () => HttpResponse.json(incident.summary))
	);
	const testSubject = DashboardPage;

	// WHEN
	renderWithProviders(createElement(testSubject), { route: "/dashboard" });

	// THEN
	const liabilities = (await screen.findByText("Total liabilities")).closest(".bg-card");
	expect(await within(liabilities as HTMLElement).findByText("€0")).toBeInTheDocument();
	expect(within(liabilities as HTMLElement).getByText("Debt ratio: 0.0%")).toBeInTheDocument();
	const netWorthCard = screen.getAllByText("Net worth")[0]?.closest(".bg-card");
	expect(await within(netWorthCard as HTMLElement).findByText("€47,979.66")).toBeInTheDocument();
});

const DASHBOARD_FALLBACK = "Could not load your dashboard. Check your connection and try again.";
const BAD_REQUEST = 400;

const DASHBOARD_ENDPOINTS = {
	summary: "/analytics/summary",
	chart: "/analytics/chart",
	targets: "/targets",
	projections: "/analytics/projections"
} as const;

type DashboardEndpoint = keyof typeof DASHBOARD_ENDPOINTS;

const SUMMARY_MESSAGE = "The summary is being recalculated";
const CHART_MESSAGE = "That year has no chart data";
const TARGETS_MESSAGE = "Your targets are temporarily unavailable";
const PROJECTIONS_MESSAGE = "Projections need one more month of history";

interface DashboardFailure {
	endpoint: DashboardEndpoint;
	message?: string;
}

function failingQuery(
	{ endpoint, message }: DashboardFailure,
	onRequest: () => void
): RequestHandler {
	return http.get(`${env.apiUrl}${DASHBOARD_ENDPOINTS[endpoint]}`, () => {
		onRequest();
		if (message === undefined) return new HttpResponse(null, { status: BAD_REQUEST });

		return HttpResponse.json(
			{ status: BAD_REQUEST, error: "Bad Request", errors: [{ message }] },
			{ status: BAD_REQUEST }
		);
	});
}

interface BannerCase {
	label: string;
	failures: DashboardFailure[];
	expected: string;
	absent?: string;
}

const BANNER_CASES: BannerCase[] = [
	{
		label: "failed summary",
		failures: [{ endpoint: "summary", message: SUMMARY_MESSAGE }],
		expected: SUMMARY_MESSAGE
	},
	{
		label: "failed chart",
		failures: [{ endpoint: "chart", message: CHART_MESSAGE }],
		expected: CHART_MESSAGE
	},
	{
		label: "failed targets",
		failures: [{ endpoint: "targets", message: TARGETS_MESSAGE }],
		expected: TARGETS_MESSAGE
	},
	{
		label: "failed projections",
		failures: [{ endpoint: "projections", message: PROJECTIONS_MESSAGE }],
		expected: PROJECTIONS_MESSAGE
	},
	{
		label: "summary and chart both failed",
		failures: [
			{ endpoint: "summary", message: SUMMARY_MESSAGE },
			{ endpoint: "chart", message: CHART_MESSAGE }
		],
		expected: SUMMARY_MESSAGE,
		absent: CHART_MESSAGE
	},
	{
		label: "chart and targets both failed",
		failures: [
			{ endpoint: "chart", message: CHART_MESSAGE },
			{ endpoint: "targets", message: TARGETS_MESSAGE }
		],
		expected: CHART_MESSAGE,
		absent: TARGETS_MESSAGE
	},
	{
		label: "targets and projections both failed",
		failures: [
			{ endpoint: "targets", message: TARGETS_MESSAGE },
			{ endpoint: "projections", message: PROJECTIONS_MESSAGE }
		],
		expected: TARGETS_MESSAGE,
		absent: PROJECTIONS_MESSAGE
	},
	{
		label: "failed summary without a body",
		failures: [{ endpoint: "summary" }],
		expected: DASHBOARD_FALLBACK
	}
];

describe("DashboardPage error banner", () => {
	it.each(BANNER_CASES)("should render the sentence of a $label", async testCase => {
		// GIVEN
		const requested: DashboardEndpoint[] = [];
		server.use(
			...testCase.failures.map(failure =>
				failingQuery(failure, () => requested.push(failure.endpoint))
			)
		);
		const testSubject = DashboardPage;

		// WHEN
		renderWithProviders(createElement(testSubject), { route: "/dashboard" });

		// THEN
		await waitFor(() =>
			expect(testCase.failures.every(failure => requested.includes(failure.endpoint))).toBe(true)
		);
		const alert = await screen.findByRole("alert");
		await waitFor(() => {
			expect(alert).toHaveTextContent(testCase.expected);
			if (testCase.absent !== undefined) expect(alert).not.toHaveTextContent(testCase.absent);
		});
	});
});
