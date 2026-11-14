import { act, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { useShortcutHintStore } from "@/stores/shortcut-hint";
import { SHORTCUT_HINT_DURATION, ShortcutHintCard } from "./shortcut-hint";

const mockHint = { keys: ["⌥", "D"], label: "Dashboard" };

function showHint() {
	act(() => {
		useShortcutHintStore.getState().showHint(mockHint);
	});
}

describe("ShortcutHintCard", () => {
	beforeEach(() => {
		vi.useFakeTimers();
		useShortcutHintStore.setState({ hint: null });
	});

	afterEach(() => {
		vi.useRealTimers();
	});

	it("should render nothing without a hint", () => {
		// GIVEN
		const testSubject = ShortcutHintCard;
		const Subject = testSubject;

		// WHEN
		const { container } = render(<Subject />);

		// THEN
		expect(container).toBeEmptyDOMElement();
	});

	it("should show the keys and the action label", () => {
		// GIVEN
		const testSubject = ShortcutHintCard;
		const Subject = testSubject;
		render(<Subject />);

		// WHEN
		showHint();

		// THEN
		const status = screen.getByRole("status");
		expect(status).toHaveTextContent("⌥");
		expect(status).toHaveTextContent("D");
		expect(status).toHaveTextContent("Dashboard");
	});

	it("should hide the hint after the timeout", () => {
		// GIVEN
		const testSubject = ShortcutHintCard;
		const Subject = testSubject;
		render(<Subject />);
		showHint();

		// WHEN
		act(() => {
			vi.advanceTimersByTime(SHORTCUT_HINT_DURATION);
		});

		// THEN
		expect(screen.queryByRole("status")).not.toBeInTheDocument();
	});

	it("should restart the timeout when the same shortcut is pressed again", () => {
		// GIVEN
		const testSubject = ShortcutHintCard;
		const Subject = testSubject;
		render(<Subject />);
		showHint();

		// WHEN
		act(() => {
			vi.advanceTimersByTime(SHORTCUT_HINT_DURATION - 100);
		});
		showHint();
		act(() => {
			vi.advanceTimersByTime(SHORTCUT_HINT_DURATION - 100);
		});

		// THEN
		expect(screen.getByRole("status")).toBeInTheDocument();

		// WHEN
		act(() => {
			vi.advanceTimersByTime(100);
		});

		// THEN
		expect(screen.queryByRole("status")).not.toBeInTheDocument();
	});

	it("should keep the hint when it unmounts before the timeout", () => {
		// GIVEN
		const testSubject = ShortcutHintCard;
		const Subject = testSubject;
		const { unmount } = render(<Subject />);
		showHint();

		// WHEN
		unmount();
		act(() => {
			vi.advanceTimersByTime(SHORTCUT_HINT_DURATION);
		});

		// THEN
		expect(useShortcutHintStore.getState().hint).not.toBeNull();
	});
});
