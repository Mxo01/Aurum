import { beforeEach, describe, expect, it } from "vitest";
import { usePrivacyStore } from "./privacy";

describe("privacy store", () => {
	beforeEach(() => {
		window.localStorage.clear();
		usePrivacyStore.setState({ isPrivacy: false });
	});

	describe("togglePrivacy", () => {
		it("should enable privacy and persist it", () => {
			// GIVEN
			const testSubject = usePrivacyStore.getState();

			// WHEN
			testSubject.togglePrivacy();

			// THEN
			expect(usePrivacyStore.getState().isPrivacy).toBe(true);
			expect(window.localStorage.getItem("privacy")).toBe("true");
		});

		it("should disable privacy and persist it", () => {
			// GIVEN
			usePrivacyStore.setState({ isPrivacy: true });

			// WHEN
			usePrivacyStore.getState().togglePrivacy();

			// THEN
			expect(usePrivacyStore.getState().isPrivacy).toBe(false);
			expect(window.localStorage.getItem("privacy")).toBe("false");
		});
	});
});
