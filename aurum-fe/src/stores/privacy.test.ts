import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { usePrivacyStore } from "./privacy";

describe("privacy store", () => {
	beforeEach(() => {
		window.localStorage.clear();
		usePrivacyStore.setState({ isPrivacy: false });
	});

	describe("initial state", () => {
		afterEach(() => {
			vi.unstubAllGlobals();
			vi.resetModules();
		});

		it("should start disabled without a window", async () => {
			// GIVEN
			vi.resetModules();
			vi.stubGlobal("window", undefined);

			// WHEN
			const { usePrivacyStore: testSubject } = await import("./privacy");

			// THEN
			expect(testSubject.getState().isPrivacy).toBe(false);
		});

		it("should restore a persisted preference", async () => {
			// GIVEN
			window.localStorage.setItem("privacy", "true");
			vi.resetModules();

			// WHEN
			const { usePrivacyStore: testSubject } = await import("./privacy");

			// THEN
			expect(testSubject.getState().isPrivacy).toBe(true);
		});

		it("should ignore a cleared preference", async () => {
			// GIVEN
			window.localStorage.setItem("privacy", "false");
			vi.resetModules();

			// WHEN
			const { usePrivacyStore: testSubject } = await import("./privacy");

			// THEN
			expect(testSubject.getState().isPrivacy).toBe(false);
		});
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
