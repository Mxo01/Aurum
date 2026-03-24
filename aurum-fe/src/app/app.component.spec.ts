import { ComponentFixture, TestBed } from "@angular/core/testing";
import { WritableSignal, signal } from "@angular/core";
import { MockComponent, MockDirective, MockProvider } from "ng-mocks";
import { faker } from "@faker-js/faker";
import { of, EMPTY } from "rxjs";
import { AuthService } from "@auth0/auth0-angular";
import { DOCUMENT } from "@angular/common";
import { App } from "./app.component";
import { ThemeService } from "./shared/services/theme/theme.service";
import { ProfileService } from "./domain/profile/profile.service";
import { UserProfile } from "./domain/profile/model/user-profile.model";
import { Currency } from "./domain/profile/model/currency.model";
import { Locale } from "./domain/profile/model/locale.model";
import { darkModeSelector } from "./app.utils";
import { RouterLink, RouterLinkActive, RouterOutlet } from "@angular/router";
import { Button, ButtonDirective } from "primeng/button";
import { Avatar } from "primeng/avatar";
import { Toolbar } from "primeng/toolbar";
import { ConfirmDialog } from "primeng/confirmdialog";
import { Toast } from "primeng/toast";
import { GdprConsentComponent } from "./domain/legal/gdpr-consent/gdpr-consent.component";

const buildMockProfile = (overrides: Partial<UserProfile> = {}): UserProfile => ({
	id: faker.string.uuid(),
	email: faker.internet.email(),
	currency: Currency.EUR,
	locale: Locale.EN_US,
	...overrides
});

describe("App", () => {
	let testSubject: App;
	let fixture: ComponentFixture<App>;

	let mockAuthService: AuthService;
	let mockProfileService: ProfileService;

	let doc: Document;
	let mockProfile: WritableSignal<UserProfile | undefined>;
	let mockIsDarkMode: WritableSignal<boolean>;

	beforeEach(() => {
		mockProfile = signal<UserProfile | undefined>(undefined);
		mockIsDarkMode = signal<boolean>(false);

		TestBed.configureTestingModule({
			imports: [
				App,
				MockDirective(RouterOutlet),
				MockComponent(Button),
				MockDirective(ButtonDirective),
				MockComponent(Avatar),
				MockComponent(Toolbar),
				MockDirective(RouterLink),
				MockDirective(RouterLinkActive),
				MockComponent(ConfirmDialog),
				MockComponent(Toast),
				MockComponent(GdprConsentComponent)
			],
			providers: [
				MockProvider(AuthService, { user$: EMPTY, logout: vi.fn() }),
				MockProvider(ThemeService, { isDarkMode: mockIsDarkMode }),
				MockProvider(ProfileService, {
					profile: mockProfile,
					getProfile: vi.fn().mockReturnValue(of(buildMockProfile()))
				})
			]
		});

		mockProfileService = TestBed.inject(ProfileService);
		mockAuthService = TestBed.inject(AuthService);
		doc = TestBed.inject(DOCUMENT);

		fixture = TestBed.createComponent(App);
		testSubject = fixture.componentInstance;
	});

	afterEach(() => {
		doc.documentElement.classList.remove(darkModeSelector);
	});

	describe("ngOnInit", () => {
		it("should add dark class to documentElement when isDarkMode is true", () => {
			// GIVEN
			mockIsDarkMode.set(true);

			// WHEN
			fixture.detectChanges();

			// THEN
			expect(doc.documentElement.classList.contains(darkModeSelector)).toBeTruthy();
		});

		it("should call profileService.getProfile on init", () => {
			// GIVEN
			const getProfile = vi.spyOn(mockProfileService, "getProfile");

			// WHEN
			fixture.detectChanges();

			// THEN
			expect(getProfile).toHaveBeenCalled();
		});
	});

	describe("avatarUrl", () => {
		it("should return the base64 data URL when profile has a custom picture", () => {
			// GIVEN
			const mockPicture = "abc123";
			mockProfile.set(buildMockProfile({ picture: mockPicture }));

			// WHEN
			fixture.detectChanges();

			// THEN
			expect(testSubject.avatarUrl()).toContain(`data:image/jpeg;base64,${mockPicture}`);
		});

		it("should return a falsy value when there is no custom picture and no auth user", () => {
			// GIVEN
			mockProfile.set(undefined);

			// WHEN
			fixture.detectChanges();

			// THEN
			expect(testSubject.avatarUrl()).toBeFalsy();
		});
	});

	describe("logout", () => {
		it("should call authService.logout", () => {
			// GIVEN
			const logout = vi.spyOn(mockAuthService, "logout");

			// WHEN
			testSubject.logout();

			// THEN
			expect(logout).toHaveBeenCalled();
		});
	});
});
