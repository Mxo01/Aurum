import { DOCUMENT } from "@angular/common";
import { TestBed } from "@angular/core/testing";
import { MockProvider } from "ng-mocks";
import { PrimeNG } from "primeng/config";
import { darkModeSelector } from "../../../app.utils";
import { Locale } from "../../../domain/profile/model/locale.model";
import { ThemeService } from "./theme.service";
import { primengLocaleMap } from "../../primeng-locale.utils";

describe("ThemeService", () => {
	let testSubject: ThemeService;
	let primeNG: PrimeNG;
	let doc: Document;

	beforeEach(() => {
		Object.defineProperty(globalThis, "matchMedia", {
			writable: true,
			value: vi.fn().mockReturnValue({ matches: false })
		});

		TestBed.configureTestingModule({
			providers: [ThemeService, MockProvider(PrimeNG, { setTranslation: vi.fn() })]
		});

		testSubject = TestBed.inject(ThemeService);
		primeNG = TestBed.inject(PrimeNG);
		doc = TestBed.inject(DOCUMENT);
	});

	afterEach(() => {
		localStorage.clear();
		doc.documentElement.classList.remove(darkModeSelector);
	});

	describe("isDarkMode", () => {
		it("should be true when set to true", () => {
			// GIVEN
			testSubject.isDarkMode.set(true);

			// THEN
			expect(testSubject.isDarkMode()).toBe(true);
		});

		it("should be false when set to false", () => {
			// GIVEN
			testSubject.isDarkMode.set(false);

			// THEN
			expect(testSubject.isDarkMode()).toBe(false);
		});
	});

	describe("toggleTheme", () => {
		it("should add dark class to document and persist dark theme when switching to dark mode", () => {
			// GIVEN
			testSubject.isDarkMode.set(false);

			// WHEN
			testSubject.toggleTheme();

			// THEN
			expect(doc.documentElement.classList.contains(darkModeSelector)).toBe(true);
			expect(localStorage.getItem("theme")).toBe("dark");
		});

		it("should remove dark class from document and persist light theme when switching to light mode", () => {
			// GIVEN
			testSubject.isDarkMode.set(true);
			doc.documentElement.classList.add(darkModeSelector);

			// WHEN
			testSubject.toggleTheme();

			// THEN
			expect(doc.documentElement.classList.contains(darkModeSelector)).toBe(false);
			expect(localStorage.getItem("theme")).toBe("light");
		});
	});

	describe("applyLocale", () => {
		it("should delegate translation to PrimeNG once for the given locale", () => {
			// WHEN
			testSubject.applyLocale(Locale.EN_US);

			// THEN
			expect(primeNG.setTranslation).toHaveBeenCalledExactlyOnceWith(
				primengLocaleMap[Locale.EN_US]
			);
		});
	});
});
