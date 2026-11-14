import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter, useLocation } from "react-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { usePrivacyStore } from "@/stores/privacy";
import { useThemeStore } from "@/stores/theme";
import { useUiStore } from "@/stores/ui";
import { useShortcuts } from "./use-shortcuts";

const CHORD_TIMEOUT = 1500;

function Harness() {
	useShortcuts();
	const location = useLocation();
	return <span data-testid="location">{`${location.pathname}${location.search}`}</span>;
}

function renderHarness() {
	return render(
		<MemoryRouter initialEntries={["/dashboard"]}>
			<Harness />
		</MemoryRouter>
	);
}

function press(target: Element | Window, init: KeyboardEventInit) {
	fireEvent.keyDown(target, init);
}

describe("useShortcuts", () => {
	beforeEach(() => {
		useUiStore.setState({ commandOpen: false });
		usePrivacyStore.setState({ isPrivacy: false });
		useThemeStore.setState({ theme: "light" });
		vi.spyOn(window, "scrollTo").mockImplementation(() => undefined);
	});

	afterEach(() => {
		vi.restoreAllMocks();
	});

	it.each([
		["KeyD", "/dashboard"],
		["KeyA", "/assets"],
		["KeyC", "/cashflow"],
		["KeyP", "/profile"]
	])("should navigate to %s with the alt key", (code, path) => {
		// GIVEN
		renderHarness();

		// WHEN
		press(window, { altKey: true, code });

		// THEN
		expect(screen.getByTestId("location")).toHaveTextContent(path);
	});

	it("should ignore an alt key that maps to nothing", () => {
		// GIVEN
		renderHarness();

		// WHEN
		press(window, { altKey: true, code: "KeyZ" });

		// THEN
		expect(screen.getByTestId("location")).toHaveTextContent("/dashboard");
	});

	it("should open the new asset route from the new chord", () => {
		// GIVEN
		renderHarness();

		// WHEN
		press(window, { altKey: true, code: "KeyN" });
		press(window, { code: "KeyA" });

		// THEN
		expect(screen.getByTestId("location")).toHaveTextContent("/assets?new=1");
	});

	it("should open the new target route from the new chord", () => {
		// GIVEN
		renderHarness();

		// WHEN
		press(window, { altKey: true, code: "KeyN" });
		press(window, { code: "KeyT" });

		// THEN
		expect(screen.getByTestId("location")).toHaveTextContent("/dashboard?target=new");
	});

	it("should drop the chord when the second key is unrelated", () => {
		// GIVEN
		renderHarness();

		// WHEN
		press(window, { altKey: true, code: "KeyN" });
		press(window, { code: "KeyX" });
		press(window, { code: "KeyA" });

		// THEN
		expect(screen.getByTestId("location")).toHaveTextContent("/dashboard");
	});

	it("should drop the chord once it times out", () => {
		// GIVEN
		renderHarness();
		const now = vi.spyOn(Date, "now").mockReturnValue(1_000);

		// WHEN
		press(window, { altKey: true, code: "KeyN" });
		now.mockReturnValue(1_000 + CHORD_TIMEOUT + 1);
		press(window, { code: "KeyA" });

		// THEN
		expect(screen.getByTestId("location")).toHaveTextContent("/dashboard");
	});

	it("should toggle privacy and theme with alt keys", () => {
		// GIVEN
		renderHarness();

		// WHEN
		press(window, { altKey: true, code: "KeyH" });

		// THEN
		expect(usePrivacyStore.getState().isPrivacy).toBe(true);

		// WHEN
		press(window, { altKey: true, code: "KeyT" });

		// THEN
		expect(useThemeStore.getState().theme).toBe("dark");
	});

	it("should toggle the command palette with the meta key", () => {
		// GIVEN
		renderHarness();

		// WHEN
		press(window, { metaKey: true, key: "k" });

		// THEN
		expect(useUiStore.getState().commandOpen).toBe(true);
	});

	it("should toggle the command palette with the control key", () => {
		// GIVEN
		renderHarness();

		// WHEN
		press(window, { ctrlKey: true, key: "K" });

		// THEN
		expect(useUiStore.getState().commandOpen).toBe(true);
	});

	it("should ignore other modifier combinations", () => {
		// GIVEN
		renderHarness();

		// WHEN
		press(window, { metaKey: true, key: "c" });

		// THEN
		expect(useUiStore.getState().commandOpen).toBe(false);
		expect(screen.getByTestId("location")).toHaveTextContent("/dashboard");
	});

	it("should toggle the command palette with the slash key", () => {
		// GIVEN
		renderHarness();

		// WHEN
		press(window, { key: "/" });

		// THEN
		expect(useUiStore.getState().commandOpen).toBe(true);
	});

	it.each(["input", "textarea", "select", "button"])(
		"should ignore shortcuts while typing in a %s",
		tag => {
			// GIVEN
			renderHarness();
			const field = document.createElement(tag);
			document.body.appendChild(field);

			// WHEN
			press(field, { altKey: true, code: "KeyA" });

			// THEN
			expect(screen.getByTestId("location")).toHaveTextContent("/dashboard");
			field.remove();
		}
	);

	it("should ignore shortcuts inside an editable region", () => {
		// GIVEN
		renderHarness();
		const region = document.createElement("div");
		Object.defineProperty(region, "isContentEditable", { value: true });
		document.body.appendChild(region);

		// WHEN
		press(region, { altKey: true, code: "KeyN" });
		press(region, { code: "KeyA" });

		// THEN
		expect(screen.getByTestId("location")).toHaveTextContent("/dashboard");
		region.remove();
	});

	it("should scroll to the top when the route changes", () => {
		// GIVEN
		renderHarness();

		// WHEN
		press(window, { altKey: true, code: "KeyC" });

		// THEN
		expect(window.scrollTo).toHaveBeenLastCalledWith({ top: 0 });
	});
});
