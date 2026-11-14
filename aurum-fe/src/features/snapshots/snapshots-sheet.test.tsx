import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { cloneElement, createElement, type ReactElement } from "react";
import type * as rechartsTypes from "recharts";
import { http, HttpResponse } from "msw";
import { describe, expect, it, vi } from "vitest";
import { env } from "@/lib/env";
import { buildAsset } from "@/test/fixtures/portfolio";
import { server } from "@/test/msw/server";
import { renderWithProviders } from "@/test/render";
import { SnapshotsSheet } from "./snapshots-sheet";

const chartFormatters = vi.hoisted(() => ({
	x: undefined as ((value: string) => string) | undefined,
	y: undefined as ((value: number) => string) | undefined,
	tooltip: undefined as ((value: unknown, name: string) => unknown) | undefined,
	label: undefined as ((label: unknown) => string) | undefined
}));

vi.mock("recharts", async importOriginal => {
	const actual = await importOriginal<typeof rechartsTypes>();
	return {
		...actual,
		ResponsiveContainer: ({
			children
		}: {
			children: ReactElement<{ width?: number; height?: number }>;
		}) => cloneElement(children, { width: 600, height: 300 }),
		XAxis: ({ tickFormatter }: { tickFormatter?: (value: string) => string }) => {
			chartFormatters.x = tickFormatter;
			return null;
		},
		YAxis: ({ tickFormatter }: { tickFormatter?: (value: number) => string }) => {
			chartFormatters.y = tickFormatter;
			return null;
		},
		Tooltip: ({
			formatter,
			labelFormatter
		}: {
			formatter?: (value: unknown, name: string) => unknown;
			labelFormatter?: (label: unknown) => string;
		}) => {
			chartFormatters.tooltip = formatter;
			chartFormatters.label = labelFormatter;
			return null;
		}
	};
});

const mockAsset = buildAsset({
	id: "a1",
	name: "Apartment Berlin",
	originalCurrency: "EUR",
	liabilityType: "MANUAL"
});

const mockSnapshots = [
	{
		id: "s1",
		assetId: "a1",
		assetName: "Apartment Berlin",
		referenceDate: "2026-11-01",
		amountOriginalCurrency: 430000,
		exchangeRateToBase: 1,
		amountBaseCurrency: 430000
	},
	{
		id: "s2",
		assetId: "a1",
		assetName: "Apartment Berlin",
		referenceDate: "2026-10-01",
		amountOriginalCurrency: 420000,
		exchangeRateToBase: 1,
		amountBaseCurrency: 420000
	}
];

function useSnapshotsFixture() {
	server.use(http.get(`${env.apiUrl}/snapshots`, () => HttpResponse.json(mockSnapshots)));
}

describe("SnapshotsSheet", () => {
	it("should render the history table", async () => {
		// GIVEN
		useSnapshotsFixture();
		const testSubject = SnapshotsSheet;

		// WHEN
		renderWithProviders(
			createElement(testSubject, { open: true, onOpenChange: () => undefined, asset: mockAsset })
		);

		// THEN
		expect(await screen.findByText("History · Apartment Berlin")).toBeInTheDocument();
		expect(await screen.findByText("Nov 1, 2026")).toBeInTheDocument();
		expect(screen.getAllByText("€430,000.00").length).toBeGreaterThan(0);
	});

	it("should add a snapshot", async () => {
		// GIVEN
		useSnapshotsFixture();
		let postedBody: unknown;
		server.use(
			http.post(`${env.apiUrl}/snapshots`, async ({ request }) => {
				postedBody = await request.json();
				return HttpResponse.json({ id: "new" });
			})
		);
		const testSubject = SnapshotsSheet;
		renderWithProviders(
			createElement(testSubject, { open: true, onOpenChange: () => undefined, asset: mockAsset })
		);
		await screen.findByText("History · Apartment Berlin");

		// WHEN
		fireEvent.change(screen.getByLabelText("Snapshot value"), { target: { value: "450000" } });
		fireEvent.click(screen.getByRole("button", { name: "Add" }));

		// THEN
		await waitFor(() => expect(postedBody).toBeDefined());
		expect(postedBody).toMatchObject({ assetId: "a1", amountOriginalCurrency: 450000 });
	});

	it("should delete the selected snapshots", async () => {
		// GIVEN
		useSnapshotsFixture();
		let deletedIds: string[] = [];
		server.use(
			http.delete(`${env.apiUrl}/snapshots/bulk`, async ({ request }) => {
				deletedIds = (await request.json()) as string[];
				return new HttpResponse(null, { status: 200 });
			})
		);
		const testSubject = SnapshotsSheet;
		renderWithProviders(
			createElement(testSubject, { open: true, onOpenChange: () => undefined, asset: mockAsset })
		);
		await screen.findByText("History · Apartment Berlin");

		// WHEN
		const checkboxes = await screen.findAllByRole("checkbox");
		fireEvent.click(checkboxes[0] as HTMLElement);
		fireEvent.click(screen.getByRole("button", { name: /Delete selected/ }));
		const dialog = await screen.findByRole("alertdialog");
		fireEvent.click(within(dialog).getByRole("button", { name: "Delete" }));

		// THEN
		await waitFor(() => expect(deletedIds).toHaveLength(1));
	});

	it("should hide the add and delete controls for an automatic liability", async () => {
		// GIVEN
		useSnapshotsFixture();
		const testSubject = SnapshotsSheet;

		// WHEN
		renderWithProviders(
			createElement(testSubject, {
				open: true,
				onOpenChange: () => undefined,
				asset: { ...mockAsset, liabilityType: "AUTOMATIC" }
			})
		);

		// THEN
		expect(await screen.findByText(/automatic payments/i)).toBeInTheDocument();
		expect(await screen.findByText("Nov 1, 2026")).toBeInTheDocument();
		expect(screen.queryByLabelText("Snapshot value")).not.toBeInTheDocument();
		expect(screen.queryByRole("checkbox")).not.toBeInTheDocument();
	});

	it("should show the chart tab with enough data", async () => {
		// GIVEN
		useSnapshotsFixture();
		const testSubject = SnapshotsSheet;
		renderWithProviders(
			createElement(testSubject, { open: true, onOpenChange: () => undefined, asset: mockAsset })
		);
		await screen.findByText("History · Apartment Berlin");

		// WHEN
		fireEvent.click(screen.getByRole("tab", { name: "Chart" }));

		// THEN
		expect(screen.queryByText("Not enough data")).not.toBeInTheDocument();
	});

	it("should render no body without an asset", () => {
		// GIVEN
		const testSubject = SnapshotsSheet;

		// WHEN
		renderWithProviders(
			createElement(testSubject, { open: true, onOpenChange: () => undefined, asset: null })
		);

		// THEN
		expect(screen.queryByText(/History ·/)).not.toBeInTheDocument();
	});

	it("should render a snapshot without id or base value as zero", async () => {
		// GIVEN
		server.use(
			http.get(`${env.apiUrl}/snapshots`, () =>
				HttpResponse.json([
					{
						id: null,
						assetId: "a1",
						assetName: "Apartment Berlin",
						referenceDate: "2026-11-01",
						amountOriginalCurrency: 430000,
						exchangeRateToBase: 1,
						amountBaseCurrency: null
					}
				])
			)
		);
		const testSubject = SnapshotsSheet;
		renderWithProviders(
			createElement(testSubject, { open: true, onOpenChange: () => undefined, asset: mockAsset })
		);
		const checkbox = await screen.findByRole("checkbox");
		expect(screen.getAllByText("€0.00").length).toBeGreaterThan(0);

		// WHEN
		fireEvent.click(checkbox);

		// THEN
		expect(screen.queryByRole("button", { name: /Delete selected/ })).not.toBeInTheDocument();
	});

	it("should toggle a snapshot off", async () => {
		// GIVEN
		useSnapshotsFixture();
		const testSubject = SnapshotsSheet;
		renderWithProviders(
			createElement(testSubject, { open: true, onOpenChange: () => undefined, asset: mockAsset })
		);
		const checkboxes = await screen.findAllByRole("checkbox");
		fireEvent.click(checkboxes[0] as HTMLElement);
		expect(screen.getByRole("button", { name: /Delete selected/ })).toBeInTheDocument();

		// WHEN
		fireEvent.click(checkboxes[0] as HTMLElement);

		// THEN
		expect(screen.queryByRole("button", { name: /Delete selected/ })).not.toBeInTheDocument();
	});

	it("should ignore an invalid or negative snapshot value", async () => {
		// GIVEN
		useSnapshotsFixture();
		let posts = 0;
		server.use(
			http.post(`${env.apiUrl}/snapshots`, () => {
				posts += 1;
				return HttpResponse.json({ id: "new" });
			})
		);
		const testSubject = SnapshotsSheet;
		renderWithProviders(
			createElement(testSubject, { open: true, onOpenChange: () => undefined, asset: mockAsset })
		);
		await screen.findByText("History · Apartment Berlin");
		const input = screen.getByLabelText("Snapshot value");

		// WHEN
		fireEvent.change(input, { target: { value: "-5" } });
		fireEvent.click(screen.getByRole("button", { name: "Add" }));

		// THEN
		expect(posts).toBe(0);
	});

	it("should format the chart axis and the tooltip", async () => {
		// GIVEN
		useSnapshotsFixture();
		const testSubject = SnapshotsSheet;
		renderWithProviders(
			createElement(testSubject, { open: true, onOpenChange: () => undefined, asset: mockAsset })
		);
		await screen.findByText("History · Apartment Berlin");

		// WHEN
		fireEvent.mouseDown(screen.getByRole("tab", { name: "Chart" }));

		// THEN
		await waitFor(() => expect(chartFormatters.x).toBeDefined());
		expect(chartFormatters.x?.("2026-11-01")).toBe("Nov 26");
		expect(chartFormatters.y?.(430000)).toMatch(/€/);
		expect(chartFormatters.tooltip?.(430000, "Value")).toEqual(["€430,000", "Value"]);
		expect(chartFormatters.label?.("2026-11-01")).toBe("Nov 1, 2026");
	});

	it("should show the empty state without snapshots", async () => {
		// GIVEN
		server.use(http.get(`${env.apiUrl}/snapshots`, () => HttpResponse.json([])));
		const testSubject = SnapshotsSheet;

		// WHEN
		renderWithProviders(
			createElement(testSubject, { open: true, onOpenChange: () => undefined, asset: mockAsset })
		);

		// THEN
		expect(await screen.findByText("No snapshots yet")).toBeInTheDocument();
	});
});
