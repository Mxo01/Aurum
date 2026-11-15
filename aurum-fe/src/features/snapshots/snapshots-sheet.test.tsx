import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { cloneElement, createElement, type ReactElement } from "react";
import type * as rechartsTypes from "recharts";
import { http, HttpResponse } from "msw";
import { describe, expect, it, vi } from "vitest";
import type { SnapshotDTO } from "@/lib/api/types";
import { env } from "@/lib/env";
import { buildAsset, buildSnapshot } from "@/test/fixtures/portfolio";
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

const EUR_SNAPSHOT_DISPLAY = "€430,000.00";
const USD_SNAPSHOT_DISPLAY = "$18,400.00";
const FALLBACK_SNAPSHOT_DISPLAY = "€18,400.00";
const USD_SNAPSHOT_DATE = "Sep 1, 2026";

const SNAPSHOT_VALUE_LABEL = "Snapshot value";
const ADD_BUTTON_NAME = "Add";
const EMPTY_AMOUNT_MESSAGE = "Enter an amount";
const INVALID_AMOUNT_MESSAGE = "Enter a number";
const NEGATIVE_AMOUNT_MESSAGE = "Cannot be negative";
const ZOD_FALLBACK_MESSAGE = /Invalid input/i;
const COMMA_DECIMAL_INPUT = "1234,56";
const COMMA_DECIMAL_AMOUNT = 1234.56;

interface InvalidAmountCase {
	name: string;
	value: string;
	expectedMessage: string;
}

const invalidAmountCases: InvalidAmountCase[] = [
	{ name: "empty", value: "", expectedMessage: EMPTY_AMOUNT_MESSAGE },
	{ name: "unparsable", value: "abc", expectedMessage: INVALID_AMOUNT_MESSAGE },
	{ name: "negative", value: "-5", expectedMessage: NEGATIVE_AMOUNT_MESSAGE }
];

function buildUsdSnapshot(overrides: Partial<SnapshotDTO> = {}): SnapshotDTO {
	return buildSnapshot({
		id: "s3",
		assetId: mockAsset.id,
		assetName: mockAsset.name,
		referenceDate: "2026-09-01",
		amountOriginalCurrency: 18_400,
		currency: "USD",
		exchangeRateToBase: 0.92,
		amountBaseCurrency: 16_928,
		...overrides
	});
}

function buildSnapshotWithoutCurrency(): Omit<SnapshotDTO, "currency"> {
	const { currency: _currency, ...snapshot } = buildUsdSnapshot();
	return snapshot;
}

const mockSnapshots = [
	buildSnapshot({
		id: "s1",
		assetId: mockAsset.id,
		assetName: mockAsset.name,
		referenceDate: "2026-11-01",
		amountOriginalCurrency: 430_000,
		currency: "EUR",
		exchangeRateToBase: 1,
		amountBaseCurrency: 430_000
	}),
	buildSnapshot({
		id: "s2",
		assetId: mockAsset.id,
		assetName: mockAsset.name,
		referenceDate: "2026-10-01",
		amountOriginalCurrency: 420_000,
		currency: "EUR",
		exchangeRateToBase: 1,
		amountBaseCurrency: 420_000
	})
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
		expect(screen.getAllByText(EUR_SNAPSHOT_DISPLAY).length).toBeGreaterThan(0);
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
		fireEvent.change(screen.getByLabelText(SNAPSHOT_VALUE_LABEL), { target: { value: "450000" } });
		fireEvent.click(screen.getByRole("button", { name: ADD_BUTTON_NAME }));

		// THEN
		await waitFor(() => expect(postedBody).toBeDefined());
		expect(postedBody).toMatchObject({ assetId: "a1", amountOriginalCurrency: 450000 });
	});

	it("should render a free-text decimal value input without native bounds", async () => {
		// GIVEN
		useSnapshotsFixture();
		const testSubject = SnapshotsSheet;
		renderWithProviders(
			createElement(testSubject, { open: true, onOpenChange: () => undefined, asset: mockAsset })
		);

		// WHEN
		const input = await screen.findByLabelText(SNAPSHOT_VALUE_LABEL);

		// THEN
		expect(input).toHaveAttribute("type", "text");
		expect(input).toHaveAttribute("inputMode", "decimal");
		expect(input).not.toHaveAttribute("min");
		expect(input).not.toHaveAttribute("step");
	});

	it("should accept a comma decimal and post it as a number", async () => {
		// GIVEN
		useSnapshotsFixture();
		let postedBody: { amountOriginalCurrency?: unknown } | undefined;
		server.use(
			http.post(`${env.apiUrl}/snapshots`, async ({ request }) => {
				postedBody = (await request.json()) as { amountOriginalCurrency?: unknown };
				return HttpResponse.json({ id: "new" });
			})
		);
		const testSubject = SnapshotsSheet;
		renderWithProviders(
			createElement(testSubject, { open: true, onOpenChange: () => undefined, asset: mockAsset })
		);
		await screen.findByText("History · Apartment Berlin");
		const input = screen.getByLabelText(SNAPSHOT_VALUE_LABEL);

		// WHEN
		fireEvent.change(input, { target: { value: COMMA_DECIMAL_INPUT } });
		fireEvent.click(screen.getByRole("button", { name: ADD_BUTTON_NAME }));

		// THEN
		await waitFor(() => expect(postedBody).toBeDefined());
		expect(postedBody?.amountOriginalCurrency).toBe(COMMA_DECIMAL_AMOUNT);
		expect(typeof postedBody?.amountOriginalCurrency).toBe("number");
		await waitFor(() => expect(input).toHaveValue(""));
		expect(screen.queryByRole("alert")).not.toBeInTheDocument();
	});

	it("should clear the snapshot value error once the user types again", async () => {
		// GIVEN
		useSnapshotsFixture();
		const testSubject = SnapshotsSheet;
		renderWithProviders(
			createElement(testSubject, { open: true, onOpenChange: () => undefined, asset: mockAsset })
		);
		await screen.findByText("History · Apartment Berlin");
		const input = screen.getByLabelText(SNAPSHOT_VALUE_LABEL);
		fireEvent.change(input, { target: { value: "abc" } });
		fireEvent.click(screen.getByRole("button", { name: ADD_BUTTON_NAME }));
		expect(screen.getByRole("alert")).toHaveTextContent(INVALID_AMOUNT_MESSAGE);

		// WHEN
		fireEvent.change(input, { target: { value: "abc1" } });

		// THEN
		expect(screen.queryByRole("alert")).not.toBeInTheDocument();
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
					buildSnapshot({
						id: null,
						assetId: mockAsset.id,
						assetName: mockAsset.name,
						referenceDate: "2026-11-01",
						amountOriginalCurrency: 430_000,
						currency: "EUR",
						exchangeRateToBase: 1,
						amountBaseCurrency: null
					})
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

	it.each(invalidAmountCases)(
		'should reject a $name snapshot value with "$expectedMessage" and never post',
		async ({ value, expectedMessage }) => {
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
			const input = screen.getByLabelText(SNAPSHOT_VALUE_LABEL);
			const addButton = screen.getByRole("button", { name: ADD_BUTTON_NAME });
			expect(addButton).toBeEnabled();

			// WHEN
			fireEvent.change(input, { target: { value } });
			fireEvent.click(addButton);

			// THEN
			expect(screen.getByRole("alert")).toHaveTextContent(expectedMessage);
			expect(screen.queryByText(ZOD_FALLBACK_MESSAGE)).not.toBeInTheDocument();
			expect(posts).toBe(0);
		}
	);

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

	it("should format each history row in the currency it was recorded in", async () => {
		// GIVEN
		server.use(
			http.get(`${env.apiUrl}/snapshots`, () =>
				HttpResponse.json([buildUsdSnapshot(), ...mockSnapshots])
			)
		);
		const testSubject = SnapshotsSheet;
		renderWithProviders(
			createElement(testSubject, { open: true, onOpenChange: () => undefined, asset: mockAsset })
		);

		// WHEN
		const usdRow = await screen.findByText(USD_SNAPSHOT_DISPLAY);

		// THEN
		expect(usdRow.closest("tr")).toHaveTextContent(USD_SNAPSHOT_DATE);
		expect(screen.getAllByText(EUR_SNAPSHOT_DISPLAY).length).toBeGreaterThan(0);
	});

	it("should fall back to the asset currency when a snapshot has no currency", async () => {
		// GIVEN
		server.use(
			http.get(`${env.apiUrl}/snapshots`, () => HttpResponse.json([buildSnapshotWithoutCurrency()]))
		);
		const testSubject = SnapshotsSheet;
		renderWithProviders(
			createElement(testSubject, { open: true, onOpenChange: () => undefined, asset: mockAsset })
		);

		// WHEN
		const fallbackRow = await screen.findByText(FALLBACK_SNAPSHOT_DISPLAY);

		// THEN
		expect(fallbackRow).toBeInTheDocument();
		expect(screen.queryByText(USD_SNAPSHOT_DISPLAY)).not.toBeInTheDocument();
	});
});
