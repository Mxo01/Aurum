import { LOCALE_ID } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { PRIVACY_PLACEHOLDER, PrivacyCurrencyPipe } from "./privacy-currency.pipe";
import { PrivacyService } from "../services/privacy/privacy.service";

describe("PrivacyCurrencyPipe", () => {
	let testSubject: PrivacyCurrencyPipe;
	let mockPrivacyService: PrivacyService;

	beforeEach(() => {
		TestBed.configureTestingModule({
			providers: [PrivacyCurrencyPipe, PrivacyService, { provide: LOCALE_ID, useValue: "en-US" }]
		});
		testSubject = TestBed.inject(PrivacyCurrencyPipe);
		mockPrivacyService = TestBed.inject(PrivacyService);
	});

	afterEach(() => {
		localStorage.clear();
	});

	describe("transform", () => {
		it("should return the privacy placeholder when privacy mode is on", () => {
			// GIVEN
			mockPrivacyService.isPrivacyMode.set(true);

			// WHEN
			const expectedResult = testSubject.transform(1234.56, "EUR", "symbol", "1.2-2", "en-US");

			// THEN
			expect(expectedResult).toBe(PRIVACY_PLACEHOLDER);
		});

		it("should return the formatted currency string when privacy mode is off", () => {
			// GIVEN
			mockPrivacyService.isPrivacyMode.set(false);

			// WHEN
			const expectedResult = testSubject.transform(1234.56, "USD", "symbol", "1.2-2", "en-US");

			// THEN
			expect(expectedResult).toContain("1,234.56");
		});

		it("should return null when value is null and privacy mode is off", () => {
			// GIVEN
			mockPrivacyService.isPrivacyMode.set(false);

			// WHEN
			const expectedResult = testSubject.transform(null, "EUR");

			// THEN
			expect(expectedResult).toBeNull();
		});
	});
});
