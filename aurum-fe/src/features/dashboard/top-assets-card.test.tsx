import { screen, within } from "@testing-library/react";
import { createElement } from "react";
import { describe, expect, it } from "vitest";
import { buildAsset, buildSummary } from "@/test/fixtures/portfolio";
import { renderWithProviders } from "@/test/render";
import { TopAssetsCard } from "./top-assets-card";

const mockAsset = buildAsset({
	id: "asset-1",
	name: "Apartment Berlin",
	type: "ASSET",
	latestValue: 110,
	latestValueBase: 110,
	previousValue: 100
});

describe("TopAssetsCard", () => {
	it("should render the top assets with their trend", () => {
		// GIVEN
		const testSubject = TopAssetsCard;
		const mockSummary = buildSummary({
			topAssets: [mockAsset],
			assetAllocation: { "Real Estate": 56.7 }
		});

		// WHEN
		renderWithProviders(createElement(testSubject, { summary: mockSummary, isLoading: false }));

		// THEN
		expect(screen.getByText("Apartment Berlin")).toBeInTheDocument();
		expect(screen.getByText("€110")).toBeInTheDocument();
		expect(screen.getByText("56.7%")).toBeInTheDocument();

		const row = screen.getByText("Apartment Berlin").closest("tr");
		expect(row).not.toBeNull();
		expect(within(row as HTMLElement).getByText("10.00%")).toHaveClass("text-success");
	});

	it("should treat a falling liability as good", () => {
		// GIVEN
		const testSubject = TopAssetsCard;
		const mockLiability = buildAsset({
			id: "asset-2",
			name: "Mortgage",
			type: "LIABILITY",
			latestValue: 90,
			latestValueBase: 90,
			previousValue: 100
		});
		const mockSummary = buildSummary({ topAssets: [mockLiability] });

		// WHEN
		renderWithProviders(createElement(testSubject, { summary: mockSummary, isLoading: false }));

		// THEN
		const row = screen.getByText("Mortgage").closest("tr");
		expect(row).not.toBeNull();
		expect(within(row as HTMLElement).getByText("10.00%")).toHaveClass("text-success");
	});

	it("should show no data when there is no previous value", () => {
		// GIVEN
		const testSubject = TopAssetsCard;
		const mockSummary = buildSummary({
			topAssets: [buildAsset({ name: "Cash", previousValue: 0 })]
		});

		// WHEN
		renderWithProviders(createElement(testSubject, { summary: mockSummary, isLoading: false }));

		// THEN
		expect(screen.getByText("No data")).toBeInTheDocument();
	});

	it("should limit the list to five assets", () => {
		// GIVEN
		const testSubject = TopAssetsCard;
		const mockSummary = buildSummary({
			topAssets: Array.from({ length: 6 }, (_, index) =>
				buildAsset({ id: `asset-${index}`, name: `Asset ${index}` })
			)
		});

		// WHEN
		renderWithProviders(createElement(testSubject, { summary: mockSummary, isLoading: false }));

		// THEN
		expect(screen.getByText("Asset 4")).toBeInTheDocument();
		expect(screen.queryByText("Asset 5")).not.toBeInTheDocument();
	});

	it("should show the empty state without assets", () => {
		// GIVEN
		const testSubject = TopAssetsCard;

		// WHEN
		renderWithProviders(
			createElement(testSubject, { summary: buildSummary({ topAssets: [] }), isLoading: false })
		);

		// THEN
		expect(screen.getByText("No assets yet")).toBeInTheDocument();
		expect(screen.getByRole("link", { name: "Add asset" })).toBeInTheDocument();
	});

	it("should show skeletons while loading", () => {
		// GIVEN
		const testSubject = TopAssetsCard;

		// WHEN
		const { container } = renderWithProviders(
			createElement(testSubject, { summary: undefined, isLoading: true })
		);

		// THEN
		expect(container.querySelectorAll(".animate-pulse")).toHaveLength(4);
	});
});
