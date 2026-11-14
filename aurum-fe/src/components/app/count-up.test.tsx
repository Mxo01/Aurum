import { act, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { CountUp } from "./count-up";

const format = (value: number) => `${Math.round(value)}`;

interface ObserverMock {
	callback: IntersectionObserverCallback;
}

function stubObserver(): ObserverMock {
	const observer: ObserverMock = { callback: () => undefined };
	class MockObserver {
		constructor(callback: IntersectionObserverCallback) {
			observer.callback = callback;
		}
		observe() {}
		disconnect() {}
		unobserve() {}
		takeRecords() {
			return [];
		}
	}
	vi.stubGlobal("IntersectionObserver", MockObserver);
	return observer;
}

function stubFrames() {
	let callbacks: FrameRequestCallback[] = [];
	const request = vi.fn((callback: FrameRequestCallback) => {
		callbacks.push(callback);
		return callbacks.length;
	});
	const cancel = vi.fn();
	vi.stubGlobal("requestAnimationFrame", request);
	vi.stubGlobal("cancelAnimationFrame", cancel);
	return {
		request,
		cancel,
		runFrame(time: number) {
			const pending = callbacks;
			callbacks = [];
			act(() => pending.forEach(callback => callback(time)));
		}
	};
}

describe("CountUp", () => {
	afterEach(() => {
		vi.restoreAllMocks();
		vi.unstubAllGlobals();
	});

	it("should show the final value with reduced motion", () => {
		// GIVEN
		vi.stubGlobal("matchMedia", () => ({ matches: true }));
		const testSubject = CountUp;
		const Subject = testSubject;

		// WHEN
		render(
			<Subject
				value={100}
				format={format}
			/>
		);

		// THEN
		expect(screen.getByText("100")).toBeInTheDocument();
	});

	it("should show the final value without an observer", () => {
		// GIVEN
		vi.stubGlobal("IntersectionObserver", undefined);
		const testSubject = CountUp;
		const Subject = testSubject;

		// WHEN
		render(
			<Subject
				value={100}
				format={format}
			/>
		);

		// THEN
		expect(screen.getByText("100")).toBeInTheDocument();
	});

	it("should animate from zero when it becomes visible", () => {
		// GIVEN
		vi.stubGlobal("matchMedia", () => ({ matches: false }));
		const observer = stubObserver();
		const frames = stubFrames();
		vi.spyOn(performance, "now").mockReturnValue(0);
		const testSubject = CountUp;
		const Subject = testSubject;
		render(
			<Subject
				value={100}
				format={format}
			/>
		);
		expect(screen.getByText("0")).toBeInTheDocument();

		// WHEN
		act(() => {
			observer.callback([{ isIntersecting: true } as IntersectionObserverEntry], {} as never);
		});
		frames.runFrame(600);

		// THEN
		expect(screen.getByText("88")).toBeInTheDocument();
	});

	it("should stop at the final value", () => {
		// GIVEN
		vi.stubGlobal("matchMedia", () => ({ matches: false }));
		const observer = stubObserver();
		const frames = stubFrames();
		vi.spyOn(performance, "now").mockReturnValue(0);
		const testSubject = CountUp;
		const Subject = testSubject;
		render(
			<Subject
				value={100}
				format={format}
			/>
		);

		// WHEN
		act(() => {
			observer.callback([{ isIntersecting: true } as IntersectionObserverEntry], {} as never);
		});
		frames.runFrame(1200);

		// THEN
		expect(screen.getByText("100")).toBeInTheDocument();
	});

	it("should cancel the animation on unmount", () => {
		// GIVEN
		vi.stubGlobal("matchMedia", () => ({ matches: false }));
		const observer = stubObserver();
		const frames = stubFrames();
		const testSubject = CountUp;
		const Subject = testSubject;
		const { unmount } = render(
			<Subject
				value={100}
				format={format}
			/>
		);
		act(() => {
			observer.callback([{ isIntersecting: true } as IntersectionObserverEntry], {} as never);
		});

		// WHEN
		unmount();

		// THEN
		expect(frames.cancel).toHaveBeenCalled();
	});

	it("should not cancel a frame that was never scheduled", () => {
		// GIVEN
		vi.stubGlobal("matchMedia", () => ({ matches: false }));
		stubObserver();
		const frames = stubFrames();
		const testSubject = CountUp;
		const Subject = testSubject;
		const { unmount } = render(
			<Subject
				value={100}
				format={format}
			/>
		);

		// WHEN
		unmount();

		// THEN
		expect(frames.cancel).not.toHaveBeenCalled();
	});

	it("should ignore a second intersection", () => {
		// GIVEN
		vi.stubGlobal("matchMedia", () => ({ matches: false }));
		const observer = stubObserver();
		const frames = stubFrames();
		const testSubject = CountUp;
		const Subject = testSubject;
		render(
			<Subject
				value={100}
				format={format}
			/>
		);
		const entry = { isIntersecting: true } as IntersectionObserverEntry;

		// WHEN
		act(() => {
			observer.callback([entry], {} as never);
			observer.callback([entry], {} as never);
		});

		// THEN
		expect(frames.request).toHaveBeenCalledTimes(1);
	});
});
