import { fireEvent, screen } from "@testing-library/react";
import { createElement } from "react";
import { beforeEach, describe, expect, it } from "vitest";
import { renderWithProviders } from "@/test/render";
import { useThemeStore } from "@/stores/theme";
import { LandingPreview } from "./landing-preview";

describe("LandingPreview", () => {
	beforeEach(() => {
		useThemeStore.setState({ theme: "light" });
	});

	it("should switch the preview theme", () => {
		// GIVEN
		const testSubject = LandingPreview;
		renderWithProviders(createElement(testSubject));

		// WHEN
		fireEvent.mouseDown(screen.getByRole("tab", { name: "Dark" }));

		// THEN
		expect(useThemeStore.getState().theme).toBe("dark");
	});
});
