import { describe, expect, it } from "vitest";
import {
	ASSET_TYPE,
	CURRENCIES,
	LIABILITY_TYPE,
	LOCALES,
	PAYMENT_FREQUENCY,
	TARGET_STATUS,
	TARGET_TYPE
} from "./types";

describe("domain constants", () => {
	it("should expose the asset types", () => {
		// GIVEN
		const testSubject = ASSET_TYPE;

		// THEN
		expect(testSubject).toEqual({ ASSET: "ASSET", LIABILITY: "LIABILITY" });
	});

	it("should expose the liability types", () => {
		// GIVEN
		const testSubject = LIABILITY_TYPE;

		// THEN
		expect(testSubject).toEqual({ MANUAL: "MANUAL", AUTOMATIC: "AUTOMATIC" });
	});

	it("should expose the payment frequencies", () => {
		// GIVEN
		const testSubject = PAYMENT_FREQUENCY;

		// THEN
		expect(testSubject).toEqual({ WEEKLY: "WEEKLY", MONTHLY: "MONTHLY", YEARLY: "YEARLY" });
	});

	it("should expose the target types", () => {
		// GIVEN
		const testSubject = TARGET_TYPE;

		// THEN
		expect(testSubject).toEqual({ NET_WORTH: "NET_WORTH", MANUAL: "MANUAL" });
	});

	it("should expose the target statuses", () => {
		// GIVEN
		const testSubject = TARGET_STATUS;

		// THEN
		expect(testSubject).toEqual({
			COMPLETED: "COMPLETED",
			IN_PROGRESS: "IN_PROGRESS",
			NOT_REACHED: "NOT_REACHED"
		});
	});

	it("should list every supported currency once", () => {
		// GIVEN
		const testSubject = CURRENCIES;

		// THEN
		expect(testSubject).toContain("EUR");
		expect(testSubject).toContain("USD");
		expect(new Set(testSubject).size).toBe(testSubject.length);
	});

	it("should list the supported locales with their sample format", () => {
		// GIVEN
		const testSubject = LOCALES;

		// THEN
		expect(testSubject.map(locale => locale.value)).toEqual(["en-US", "fr-FR", "it-IT"]);
		expect(testSubject.every(locale => locale.label.length > 0)).toBe(true);
	});
});
