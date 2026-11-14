import { act, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { Reveal } from "./reveal";

interface ObserverMock {
	callback: IntersectionObserverCallback;
	disconnect: ReturnType<typeof vi.fn>;
}

function stubObserver(): ObserverMock {
	const disconnect = vi.fn();
	const observer: ObserverMock = {
		callback: () => undefined,
		disconnect
	};
	class MockObserver {
		constructor(callback: IntersectionObserverCallback) {
			observer.callback = callback;
		}
		observe() {}
		disconnect() {
			disconnect();
		}
		unobserve() {}
		takeRecords() {
			return [];
		}
	}
	vi.stubGlobal("IntersectionObserver", MockObserver);
	return observer;
}

describe("Reveal", () => {
	afterEach(() => {
		vi.unstubAllGlobals();
	});

	it("should become visible when it intersects the viewport", () => {
		// GIVEN
		const observer = stubObserver();
		const testSubject = Reveal;
		const Subject = testSubject;

		// WHEN
		render(<Subject>content</Subject>);
		act(() => {
			observer.callback([{ isIntersecting: true } as IntersectionObserverEntry], {} as never);
		});

		// THEN
		expect(screen.getByText("content")).toHaveAttribute("data-visible", "true");
	});

	it("should hide again when it leaves the viewport", () => {
		// GIVEN
		const observer = stubObserver();
		const testSubject = Reveal;
		const Subject = testSubject;
		render(<Subject>content</Subject>);

		// WHEN
		act(() => {
			observer.callback([{ isIntersecting: false } as IntersectionObserverEntry], {} as never);
		});

		// THEN
		expect(screen.getByText("content")).toHaveAttribute("data-visible", "false");
	});

	it("should stay hidden for an empty entry list", () => {
		// GIVEN
		const observer = stubObserver();
		const testSubject = Reveal;
		const Subject = testSubject;
		render(<Subject>content</Subject>);

		// WHEN
		act(() => {
			observer.callback([], {} as never);
		});

		// THEN
		expect(screen.getByText("content")).toHaveAttribute("data-visible", "false");
	});

	it("should fall back to visible without an observer", () => {
		// GIVEN
		vi.stubGlobal("IntersectionObserver", undefined);
		const testSubject = Reveal;
		const Subject = testSubject;

		// WHEN
		render(<Subject>content</Subject>);

		// THEN
		expect(screen.getByText("content")).toHaveAttribute("data-visible", "true");
	});

	it("should apply the delay and disconnect on unmount", () => {
		// GIVEN
		const observer = stubObserver();
		const testSubject = Reveal;
		const Subject = testSubject;
		const { unmount } = render(<Subject delay={150}>content</Subject>);

		// THEN
		expect(screen.getByText("content")).toHaveStyle("--reveal-delay: 150ms");

		// WHEN
		unmount();

		// THEN
		expect(observer.disconnect).toHaveBeenCalled();
	});
});
