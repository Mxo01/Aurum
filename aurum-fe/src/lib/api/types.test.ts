import { describe, expect, expectTypeOf, it } from "vitest";
import { buildProfile } from "@/test/fixtures/portfolio";
import {
	ASSET_TYPE,
	CURRENCIES,
	LIABILITY_TYPE,
	LOCALES,
	PAYMENT_FREQUENCY,
	TARGET_STATUS,
	TARGET_TYPE
} from "./types";
import type { Currency, Locale, UserDTO } from "./types";

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

const PROFILE_ID = "3f8a1d2c-5b6e-4a7f-9c1d-2e3f4a5b6c7d";
const PROFILE_CURRENCY: Currency = "USD";
const PROFILE_LOCALE: Locale = "it-IT";
const PROFILE_PICTURE = "ZmFrZV9qcGVn";

describe("profile contract", () => {
	it("should accept every field the endpoint returns", () => {
		// GIVEN
		const testSubject: UserDTO = {
			id: PROFILE_ID,
			currency: PROFILE_CURRENCY,
			locale: PROFILE_LOCALE,
			picture: PROFILE_PICTURE
		};

		// THEN
		expect(testSubject).toEqual({
			id: PROFILE_ID,
			currency: PROFILE_CURRENCY,
			locale: PROFILE_LOCALE,
			picture: PROFILE_PICTURE
		});
	});

	it("should accept a profile without a picture", () => {
		// GIVEN
		const testSubject: UserDTO = {
			id: PROFILE_ID,
			currency: PROFILE_CURRENCY,
			locale: PROFILE_LOCALE,
			picture: null
		};

		// THEN
		expect(testSubject.picture).toBeNull();
	});

	it("should type the identifier, the currency and the locale as required", () => {
		// THEN
		expectTypeOf<UserDTO["id"]>().toEqualTypeOf<string>();
		expectTypeOf<UserDTO["currency"]>().toEqualTypeOf<Currency>();
		expectTypeOf<UserDTO["locale"]>().toEqualTypeOf<Locale>();
		expectTypeOf<UserDTO["picture"]>().toEqualTypeOf<string | null | undefined>();
	});

	it("should agree with the profile fixture", () => {
		// GIVEN
		const testSubject: UserDTO = buildProfile({
			currency: PROFILE_CURRENCY,
			locale: PROFILE_LOCALE,
			picture: PROFILE_PICTURE
		});

		// THEN
		expect(testSubject.id).toBeTruthy();
		expect(testSubject.currency).toBe(PROFILE_CURRENCY);
		expect(testSubject.locale).toBe(PROFILE_LOCALE);
		expect(testSubject.picture).toBe(PROFILE_PICTURE);
	});
});
