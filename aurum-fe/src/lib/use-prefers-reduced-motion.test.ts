import { act, renderHook } from "@testing-library/react";
import { createElement } from "react";
import { renderToStaticMarkup } from "react-dom/server";
import { afterEach, describe, expect, it, vi } from "vitest";
import { usePrefersReducedMotion } from "./use-prefers-reduced-motion";

const QUERY = "(prefers-reduced-motion: reduce)";
const PROBE_TAG = "span";
const REDUCED_MOTION_OFF = "false";

function ReducedMotionProbe() {
	return createElement(PROBE_TAG, null, String(usePrefersReducedMotion()));
}

function mockMatchMedia(initial: boolean) {
	const listeners = new Set<() => void>();
	const query = {
		matches: initial,
		media: QUERY,
		addEventListener: (_event: string, listener: () => void) => listeners.add(listener),
		removeEventListener: (_event: string, listener: () => void) => listeners.delete(listener)
	};

	vi.stubGlobal(
		"matchMedia",
		vi.fn(() => query)
	);

	return {
		query,
		change(matches: boolean) {
			query.matches = matches;
			for (const listener of listeners) listener();
		}
	};
}

describe("usePrefersReducedMotion", () => {
	afterEach(() => {
		vi.unstubAllGlobals();
	});

	it("should report an enabled preference", () => {
		// GIVEN
		mockMatchMedia(true);

		// WHEN
		const { result } = renderHook(() => usePrefersReducedMotion());

		// THEN
		expect(result.current).toBe(true);
	});

	it("should report a disabled preference", () => {
		// GIVEN
		mockMatchMedia(false);

		// WHEN
		const { result } = renderHook(() => usePrefersReducedMotion());

		// THEN
		expect(result.current).toBe(false);
	});

	it("should fall back to no preference without matchMedia", () => {
		// GIVEN
		vi.stubGlobal("matchMedia", undefined);

		// WHEN
		const { result } = renderHook(() => usePrefersReducedMotion());

		// THEN
		expect(result.current).toBe(false);
	});

	it("should fall back to no preference without a window", () => {
		// GIVEN
		vi.stubGlobal("window", undefined);

		// WHEN
		const rendered = renderToStaticMarkup(createElement(ReducedMotionProbe));

		// THEN
		expect(rendered).toBe(`<${PROBE_TAG}>${REDUCED_MOTION_OFF}</${PROBE_TAG}>`);
	});

	it("should follow preference changes", () => {
		// GIVEN
		const media = mockMatchMedia(false);
		const { result } = renderHook(() => usePrefersReducedMotion());

		// WHEN
		act(() => media.change(true));

		// THEN
		expect(result.current).toBe(true);
	});
});
