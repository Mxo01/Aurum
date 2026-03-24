import { ComponentFixture, TestBed } from "@angular/core/testing";
import { GdprConsentComponent } from "./gdpr-consent.component";

describe("GdprConsentComponent", () => {
	let fixture: ComponentFixture<GdprConsentComponent>;
	let testSubject: GdprConsentComponent;

	beforeEach(() => {
		TestBed.overrideComponent(GdprConsentComponent, { set: { template: "", imports: [] } });
		TestBed.configureTestingModule({ imports: [GdprConsentComponent] });
	});

	afterEach(() => {
		localStorage.removeItem("gdpr_consent_accepted");
	});

	describe("visible", () => {
		it("should be true when localStorage has no consent record", () => {
			// GIVEN
			localStorage.removeItem("gdpr_consent_accepted");

			// WHEN
			fixture = TestBed.createComponent(GdprConsentComponent);
			testSubject = fixture.componentInstance;

			// THEN
			expect(testSubject.visible()).toBe(true);
		});

		it("should be false when localStorage already has consent", () => {
			// GIVEN
			localStorage.setItem("gdpr_consent_accepted", "true");

			// WHEN
			fixture = TestBed.createComponent(GdprConsentComponent);
			testSubject = fixture.componentInstance;

			// THEN
			expect(testSubject.visible()).toBe(false);
		});
	});

	describe("accept", () => {
		it("should store consent in localStorage and hide the banner", () => {
			// GIVEN
			localStorage.removeItem("gdpr_consent_accepted");
			fixture = TestBed.createComponent(GdprConsentComponent);
			testSubject = fixture.componentInstance;

			// WHEN
			testSubject.accept();

			// THEN
			expect(localStorage.getItem("gdpr_consent_accepted")).toBe("true");
			expect(testSubject.visible()).toBe(false);
		});
	});
});
