import { act, render, screen } from "@testing-library/react";
import { Download, EyeOff, Lock, Trash2 } from "lucide-react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { PrivacyCarousel } from "./privacy-carousel";

const ITEMS = [
	{ icon: Lock, title: "Auth0", body: "Login" },
	{ icon: EyeOff, title: "No trackers", body: "No analytics" },
	{ icon: Download, title: "Export", body: "Download" },
	{ icon: Trash2, title: "Delete", body: "Remove" }
];

const HEADER = <h2>Private by default.</h2>;

describe("PrivacyCarousel", () => {
	let frames: FrameRequestCallback[];
	let requestSpy: ReturnType<typeof vi.fn>;
	let cancelSpy: ReturnType<typeof vi.fn>;
	let top = 0;
	let offsetSpy: ReturnType<typeof vi.spyOn>;

	beforeEach(() => {
		frames = [];
		top = 0;
		requestSpy = vi.fn((callback: FrameRequestCallback) => {
			frames.push(callback);
			return frames.length;
		});
		cancelSpy = vi.fn();
		vi.stubGlobal("requestAnimationFrame", requestSpy);
		vi.stubGlobal("cancelAnimationFrame", cancelSpy);
		Object.defineProperty(window, "innerHeight", { configurable: true, value: 1000 });
		offsetSpy = vi.spyOn(HTMLElement.prototype, "offsetHeight", "get").mockReturnValue(3000);
		vi.spyOn(Element.prototype, "getBoundingClientRect").mockImplementation(
			() =>
				({
					top,
					bottom: 0,
					height: 0,
					left: 0,
					right: 0,
					width: 0,
					x: 0,
					y: 0,
					toJSON: () => undefined
				}) as DOMRect
		);
	});

	afterEach(() => {
		vi.restoreAllMocks();
		vi.unstubAllGlobals();
	});

	function renderCarousel() {
		return render(
			<PrivacyCarousel
				items={ITEMS}
				header={HEADER}
			/>
		);
	}

	function scroll() {
		act(() => {
			window.dispatchEvent(new Event("scroll"));
		});
	}

	function runFrame() {
		act(() => {
			const [callback] = frames.splice(0, 1);
			callback?.(0);
		});
	}

	it("should render the header and the active card", () => {
		// GIVEN
		const testSubject = PrivacyCarousel;

		// WHEN
		renderCarousel();

		// THEN
		expect(screen.getByRole("heading", { name: "Private by default." })).toBeInTheDocument();
		expect(screen.getByText("Auth0").closest("article")).toHaveAttribute("aria-hidden", "false");
		expect(screen.getByText("No trackers").closest("article")).toHaveAttribute(
			"aria-hidden",
			"true"
		);
		expect(testSubject).toBeDefined();
	});

	it("should advance to the last card when the runway is scrolled", () => {
		// GIVEN
		renderCarousel();
		top = -2000;

		// WHEN
		scroll();
		runFrame();

		// THEN
		expect(screen.getByText("Delete").closest("article")).toHaveAttribute("aria-hidden", "false");
	});

	it("should coalesce scroll events into one frame", () => {
		// GIVEN
		renderCarousel();
		requestSpy.mockClear();

		// WHEN
		scroll();
		scroll();

		// THEN
		expect(requestSpy).toHaveBeenCalledTimes(1);
	});

	it("should keep the first card when the runway does not scroll", () => {
		// GIVEN
		offsetSpy.mockReturnValue(500);

		// WHEN
		renderCarousel();
		top = -100;
		scroll();
		runFrame();

		// THEN
		expect(screen.getByText("Auth0").closest("article")).toHaveAttribute("aria-hidden", "false");
	});

	it("should cancel a pending frame on unmount", () => {
		// GIVEN
		const { unmount } = renderCarousel();
		scroll();

		// WHEN
		unmount();

		// THEN
		expect(cancelSpy).toHaveBeenCalled();
	});
});
