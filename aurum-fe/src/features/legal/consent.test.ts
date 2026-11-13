import { beforeEach, describe, expect, it } from "vitest";
import { CONSENT_KEY, readConsent, storeConsent } from "./consent";

describe("consent storage", () => {
	beforeEach(() => {
		window.localStorage.clear();
	});

	describe("readConsent", () => {
		it("should be false when nothing is stored", () => {
			// GIVEN
			const testSubject = readConsent;

			// WHEN
			const expected = testSubject();

			// THEN
			expect(expected).toBe(false);
		});

		it("should be true after consent is stored", () => {
			// GIVEN
			storeConsent();

			// WHEN
			const expected = readConsent();

			// THEN
			expect(expected).toBe(true);
			expect(window.localStorage.getItem(CONSENT_KEY)).toBe("true");
		});
	});
});
