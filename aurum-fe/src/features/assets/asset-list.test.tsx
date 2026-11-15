import { fireEvent, screen } from "@testing-library/react";
import { createElement } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import type { AssetDTO } from "@/lib/api/types";
import { usePrivacyStore } from "@/stores/privacy";
import { buildAsset } from "@/test/fixtures/portfolio";
import { renderWithProviders } from "@/test/render";
import { AssetList } from "./asset-list";

const LAYOUTS = ["desktop", "mobile"] as const;
const RENDERS_PER_LAYOUT = LAYOUTS.length;

const BASE_CURRENCY = "EUR";
const GROUP_TOTAL_TITLE = `Group total in ${BASE_CURRENCY}, your base currency. Row values are shown in each asset's own currency.`;
const AUTOMATIC_VALUE_TITLE =
	"Value follows the automatic payment schedule and cannot be edited here.";
const SCHEDULE_HINT = "Follows the automatic payment schedule";
const NO_TREND_LABEL = "No data";
const PRIVACY_MASK = "••••••";

const APARTMENT_DISPLAY = "€430,000.00";
const MORTGAGE_DISPLAY = "€268,000.00";
const CAR_LOAN_DISPLAY = "$18,400.00";
const CAR_LOAN_TOTAL_DISPLAY = "€17,000.00";
const UNPRICED_LOAN_DISPLAY = "$0.00";
const NEXT_APARTMENT_VALUE = 440_000;
const NEXT_MORTGAGE_VALUE = 260_000;

const mockApartment = buildAsset({
	id: "a1",
	name: "Apartment Berlin",
	categoryName: "Real Estate",
	categoryIcon: "pi-home",
	type: "ASSET",
	originalCurrency: "EUR",
	latestValue: 430_000,
	latestValueBase: 430_000,
	previousValue: 412_000
});

const mockMortgage = buildAsset({
	id: "a2",
	name: "Mortgage",
	categoryName: "Mortgage",
	categoryIcon: "pi-building",
	type: "LIABILITY",
	liabilityType: "MANUAL",
	originalCurrency: "EUR",
	latestValue: 268_000,
	latestValueBase: 268_000,
	previousValue: 273_500
});

const mockCarLoan = buildAsset({
	id: "a3",
	name: "Car Loan",
	categoryName: "Loans",
	categoryIcon: "pi-car",
	type: "LIABILITY",
	liabilityType: "AUTOMATIC",
	paymentFrequency: "MONTHLY",
	paymentAmount: 320,
	originalCurrency: "USD",
	latestValue: 18_400,
	latestValueBase: 17_000,
	previousValue: 19_000
});

const mockUnpricedLoan = buildAsset({
	id: "a5",
	name: "Studio Loan",
	categoryName: "Loans",
	categoryIcon: "pi-car",
	type: "LIABILITY",
	liabilityType: "AUTOMATIC",
	paymentFrequency: "MONTHLY",
	paymentAmount: 180,
	originalCurrency: "USD",
	latestValue: null,
	latestValueBase: null,
	previousValue: null
});

const mockStaleAsset = buildAsset({
	id: "a4",
	name: "Old Savings",
	categoryName: "Cash",
	categoryIcon: "pi-wallet",
	type: "ASSET",
	originalCurrency: "EUR",
	latestValue: 5_000,
	latestValueBase: 5_000,
	previousValue: null
});

function nameEditorLabel(asset: AssetDTO) {
	return `Edit ${asset.name}, currently ${asset.name}`;
}

function valueEditorLabel(asset: AssetDTO, display: string) {
	return `Edit ${asset.name}, currently ${display}`;
}

function valueInputLabel(asset: AssetDTO) {
	return `Edit ${asset.name} in ${asset.originalCurrency}`;
}

function renderAssetList(assets: AssetDTO[], collapsed = new Set<string>()) {
	const testSubject = AssetList;
	const onValueSave = vi.fn();

	renderWithProviders(
		createElement(testSubject, {
			assets,
			collapsed,
			onToggle: vi.fn(),
			onEdit: vi.fn(),
			onHistory: vi.fn(),
			onDelete: vi.fn(),
			onRename: vi.fn(),
			onToggleFavorite: vi.fn(),
			onValueSave
		})
	);

	return { onValueSave };
}

interface ValueCase {
	readonly label: string;
	readonly asset: AssetDTO;
	readonly display: string;
	readonly valueMode: "editable" | "scheduled";
}

const VALUE_CASES: ValueCase[] = [
	{ label: "an asset", asset: mockApartment, display: APARTMENT_DISPLAY, valueMode: "editable" },
	{
		label: "a manual liability",
		asset: mockMortgage,
		display: MORTGAGE_DISPLAY,
		valueMode: "editable"
	},
	{
		label: "an automatic liability",
		asset: mockCarLoan,
		display: CAR_LOAN_DISPLAY,
		valueMode: "scheduled"
	},
	{
		label: "an automatic liability without a value",
		asset: mockUnpricedLoan,
		display: UNPRICED_LOAN_DISPLAY,
		valueMode: "scheduled"
	}
];

const SAVE_CASES = [
	{
		label: "an asset",
		asset: mockApartment,
		display: APARTMENT_DISPLAY,
		nextValue: NEXT_APARTMENT_VALUE
	},
	{
		label: "a manual liability",
		asset: mockMortgage,
		display: MORTGAGE_DISPLAY,
		nextValue: NEXT_MORTGAGE_VALUE
	}
];

afterEach(() => {
	usePrivacyStore.setState({ isPrivacy: false });
});

describe("AssetList value column", () => {
	it("should render a missing value as zero", async () => {
		// GIVEN
		const asset = buildAsset({
			id: "a1",
			name: "Empty",
			latestValue: null,
			latestValueBase: null
		});

		// WHEN
		renderAssetList([asset]);

		// THEN
		expect((await screen.findAllByText("€0.00")).length).toBeGreaterThan(0);
	});

	it.each(VALUE_CASES)(
		"should render $label with a $valueMode value",
		({ asset, display, valueMode }) => {
			// GIVEN
			renderAssetList([asset]);
			const isScheduled = valueMode === "scheduled";

			// WHEN
			const valueEditors = screen.queryAllByRole("button", {
				name: valueEditorLabel(asset, display)
			});
			const scheduledValues = screen.queryAllByTitle(AUTOMATIC_VALUE_TITLE);
			const scheduleHints = screen.queryAllByText(SCHEDULE_HINT);

			// THEN
			expect(valueEditors).toHaveLength(isScheduled ? 0 : RENDERS_PER_LAYOUT);
			expect(scheduledValues).toHaveLength(isScheduled ? RENDERS_PER_LAYOUT : 0);
			expect(scheduleHints).toHaveLength(isScheduled ? RENDERS_PER_LAYOUT : 0);
			expect(screen.getAllByRole("button", { name: nameEditorLabel(asset) })).toHaveLength(
				RENDERS_PER_LAYOUT
			);
		}
	);

	it("should keep the automatic liability value read-only", () => {
		// GIVEN
		const { onValueSave } = renderAssetList([mockCarLoan]);
		const scheduledValues = screen.getAllByTitle(AUTOMATIC_VALUE_TITLE);

		// WHEN
		fireEvent.click(scheduledValues[0] as HTMLElement);

		// THEN
		expect(scheduledValues).toHaveLength(RENDERS_PER_LAYOUT);
		for (const scheduledValue of scheduledValues) {
			expect(scheduledValue.tagName).toBe("SPAN");
			expect(scheduledValue).not.toHaveAttribute("tabindex");
			expect(scheduledValue.querySelector("button")).toBeNull();
			expect(scheduledValue.querySelector("svg")).toHaveAttribute("aria-hidden", "true");
			expect(scheduledValue).toHaveTextContent(CAR_LOAN_DISPLAY);
		}
		expect(screen.getAllByText(SCHEDULE_HINT)).toHaveLength(RENDERS_PER_LAYOUT);
		expect(screen.queryByLabelText(valueInputLabel(mockCarLoan))).not.toBeInTheDocument();
		expect(
			screen.queryAllByRole("button", {
				name: valueEditorLabel(mockCarLoan, CAR_LOAN_DISPLAY)
			})
		).toHaveLength(0);
		expect(onValueSave).not.toHaveBeenCalled();
	});

	it.each(SAVE_CASES)("should save an inline edit of $label", ({ asset, display, nextValue }) => {
		// GIVEN
		const { onValueSave } = renderAssetList([asset]);
		const [valueEditor] = screen.getAllByRole("button", {
			name: valueEditorLabel(asset, display)
		});

		// WHEN
		fireEvent.click(valueEditor as HTMLElement);
		const input = screen.getByLabelText(valueInputLabel(asset));
		fireEvent.change(input, { target: { value: String(nextValue) } });
		fireEvent.blur(input);

		// THEN
		expect(onValueSave).toHaveBeenCalledWith(asset, nextValue);
	});

	it("should mask a scheduled value in privacy mode", () => {
		// GIVEN
		usePrivacyStore.setState({ isPrivacy: true });
		renderAssetList([mockCarLoan]);

		// WHEN
		const scheduledValues = screen.getAllByTitle(AUTOMATIC_VALUE_TITLE);

		// THEN
		expect(scheduledValues).toHaveLength(RENDERS_PER_LAYOUT);
		for (const scheduledValue of scheduledValues) {
			expect(scheduledValue).toHaveTextContent(PRIVACY_MASK);
			expect(scheduledValue).not.toHaveTextContent(CAR_LOAN_DISPLAY);
		}
		expect(screen.queryByText(CAR_LOAN_DISPLAY)).not.toBeInTheDocument();
	});

	it("should show no data when the asset has no previous value", () => {
		// GIVEN
		renderAssetList([mockStaleAsset]);

		// WHEN
		const noDataLabels = screen.getAllByText(NO_TREND_LABEL);

		// THEN
		expect(noDataLabels).toHaveLength(RENDERS_PER_LAYOUT);
	});
});

describe("AssetList group headers", () => {
	it("should show the base currency next to each group total", () => {
		// GIVEN
		renderAssetList([mockApartment, mockMortgage]);

		// WHEN
		const groupTotals = screen.getAllByTitle(GROUP_TOTAL_TITLE);

		// THEN
		expect(groupTotals).toHaveLength(2 * RENDERS_PER_LAYOUT);
		expect(groupTotals.map(total => total.textContent)).toEqual([
			`${MORTGAGE_DISPLAY}${BASE_CURRENCY}`,
			`${APARTMENT_DISPLAY}${BASE_CURRENCY}`,
			`${MORTGAGE_DISPLAY}${BASE_CURRENCY}`,
			`${APARTMENT_DISPLAY}${BASE_CURRENCY}`
		]);
		expect(groupTotals[0]?.closest("button")).toHaveAttribute("aria-expanded", "true");
	});

	it("should keep the group total and hide the rows when collapsed", () => {
		// GIVEN
		renderAssetList([mockApartment, mockMortgage], new Set([mockApartment.categoryName]));

		// WHEN
		const collapsedHeader = screen
			.getAllByTitle(GROUP_TOTAL_TITLE)
			.find(total => total.textContent?.startsWith(APARTMENT_DISPLAY));

		// THEN
		expect(collapsedHeader).toBeDefined();
		expect(collapsedHeader?.closest("button")).toHaveAttribute("aria-expanded", "false");
		expect(screen.queryAllByRole("button", { name: nameEditorLabel(mockApartment) })).toHaveLength(
			0
		);
		expect(screen.getAllByRole("button", { name: nameEditorLabel(mockMortgage) })).toHaveLength(
			RENDERS_PER_LAYOUT
		);
	});

	it("should keep the automatic liability total in the base currency", () => {
		// GIVEN
		renderAssetList([mockCarLoan]);

		// WHEN
		const groupTotals = screen.getAllByTitle(GROUP_TOTAL_TITLE);

		// THEN
		expect(groupTotals.map(total => total.textContent)).toEqual([
			`${CAR_LOAN_TOTAL_DISPLAY}${BASE_CURRENCY}`,
			`${CAR_LOAN_TOTAL_DISPLAY}${BASE_CURRENCY}`
		]);
	});
});
