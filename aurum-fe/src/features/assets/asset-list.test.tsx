import { screen } from "@testing-library/react";
import { createElement } from "react";
import { describe, expect, it, vi } from "vitest";
import { buildAsset } from "@/test/fixtures/portfolio";
import { renderWithProviders } from "@/test/render";
import { AssetList } from "./asset-list";

describe("AssetList", () => {
	it("should render a missing value as zero", async () => {
		// GIVEN
		const testSubject = AssetList;
		const asset = buildAsset({
			id: "a1",
			name: "Empty",
			latestValue: null,
			latestValueBase: null
		});

		// WHEN
		renderWithProviders(
			createElement(testSubject, {
				assets: [asset],
				collapsed: new Set<string>(),
				onToggle: vi.fn(),
				onEdit: vi.fn(),
				onHistory: vi.fn(),
				onDelete: vi.fn(),
				onRename: vi.fn(),
				onToggleFavorite: vi.fn(),
				onValueSave: vi.fn()
			})
		);

		// THEN
		expect((await screen.findAllByText("€0.00")).length).toBeGreaterThan(0);
	});
});
