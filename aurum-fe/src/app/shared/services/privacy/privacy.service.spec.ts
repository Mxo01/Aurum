import { TestBed } from "@angular/core/testing";
import { PrivacyService } from "./privacy.service";

describe("PrivacyService", () => {
	let testSubject: PrivacyService;

	beforeEach(() => {
		TestBed.configureTestingModule({
			providers: [PrivacyService]
		});
		testSubject = TestBed.inject(PrivacyService);
	});

	afterEach(() => {
		localStorage.clear();
	});

	describe("isPrivacyMode", () => {
		it("should be false when localStorage has no privacy key", () => {
			// GIVEN
			localStorage.removeItem("privacy");

			// THEN
			expect(testSubject.isPrivacyMode()).toBe(false);
		});

		it("should be true when localStorage has privacy set to true", () => {
			// GIVEN
			localStorage.setItem("privacy", "true");

			// WHEN
			TestBed.resetTestingModule();
			TestBed.configureTestingModule({ providers: [PrivacyService] });
			const freshService = TestBed.inject(PrivacyService);

			// THEN
			expect(freshService.isPrivacyMode()).toBe(true);
		});
	});

	describe("togglePrivacy", () => {
		it("should set isPrivacyMode to true and persist it when enabling privacy", () => {
			// GIVEN
			testSubject.isPrivacyMode.set(false);

			// WHEN
			testSubject.togglePrivacy();

			// THEN
			expect(testSubject.isPrivacyMode()).toBe(true);
			expect(localStorage.getItem("privacy")).toBe("true");
		});

		it("should set isPrivacyMode to false and persist it when disabling privacy", () => {
			// GIVEN
			testSubject.isPrivacyMode.set(true);

			// WHEN
			testSubject.togglePrivacy();

			// THEN
			expect(testSubject.isPrivacyMode()).toBe(false);
			expect(localStorage.getItem("privacy")).toBe("false");
		});
	});
});
