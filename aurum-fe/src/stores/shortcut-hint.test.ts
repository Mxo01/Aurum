import { beforeEach, describe, expect, it } from "vitest";
import { useShortcutHintStore } from "./shortcut-hint";

const mockHint = { keys: ["⌥", "D"], label: "Dashboard" };

describe("useShortcutHintStore", () => {
	beforeEach(() => {
		useShortcutHintStore.setState({ hint: null });
	});

	it("should store the pressed shortcut", () => {
		// GIVEN
		const testSubject = useShortcutHintStore;

		// WHEN
		testSubject.getState().showHint(mockHint);

		// THEN
		expect(testSubject.getState().hint).toMatchObject(mockHint);
		expect(testSubject.getState().hint?.id).toBeGreaterThan(0);
	});

	it("should give every press a new id", () => {
		// GIVEN
		const testSubject = useShortcutHintStore;
		testSubject.getState().showHint(mockHint);
		const firstId = testSubject.getState().hint?.id;

		// WHEN
		testSubject.getState().showHint(mockHint);

		// THEN
		expect(testSubject.getState().hint?.id).toBeGreaterThan(firstId ?? 0);
	});

	it("should clear the hint", () => {
		// GIVEN
		const testSubject = useShortcutHintStore;
		testSubject.getState().showHint(mockHint);

		// WHEN
		testSubject.getState().clearHint();

		// THEN
		expect(testSubject.getState().hint).toBeNull();
	});
});
