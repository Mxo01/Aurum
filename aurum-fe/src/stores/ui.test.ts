import { beforeEach, describe, expect, it } from "vitest";
import { useUiStore } from "./ui";

describe("useUiStore", () => {
	beforeEach(() => {
		useUiStore.setState({ commandOpen: false });
	});

	it("should open and close the command palette", () => {
		// GIVEN
		const testSubject = useUiStore;

		// WHEN
		testSubject.getState().setCommandOpen(true);

		// THEN
		expect(testSubject.getState().commandOpen).toBe(true);
	});

	it("should toggle the command palette", () => {
		// GIVEN
		const testSubject = useUiStore;

		// WHEN
		testSubject.getState().toggleCommand();

		// THEN
		expect(testSubject.getState().commandOpen).toBe(true);
	});
});
