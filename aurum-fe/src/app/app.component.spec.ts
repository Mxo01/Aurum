import { ComponentFixture, TestBed } from "@angular/core/testing";
import { WritableSignal, signal } from "@angular/core";
import { MockComponent, MockDirective, MockProvider } from "ng-mocks";
import { faker } from "@faker-js/faker";
import { BehaviorSubject, of } from "rxjs";
import { AuthService } from "@auth0/auth0-angular";
import { DOCUMENT } from "@angular/common";
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from "@angular/router";
import { ButtonDirective, Button } from "primeng/button";
import { Avatar } from "primeng/avatar";
import { Menu } from "primeng/menu";
import { Toolbar } from "primeng/toolbar";
import { ConfirmDialog } from "primeng/confirmdialog";
import { Toast } from "primeng/toast";
import { App } from "./app.component";
import { ThemeService } from "./shared/services/theme/theme.service";
import { PrivacyService } from "./shared/services/privacy/privacy.service";
import { ProfileService } from "./domain/profile/profile.service";
import { GdprConsentComponent } from "./domain/legal/gdpr-consent/gdpr-consent.component";
import { UserProfile } from "./domain/profile/model/user-profile.model";
import { Currency } from "./domain/profile/model/currency.model";
import { Locale } from "./domain/profile/model/locale.model";
import { darkModeSelector } from "./app.utils";

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
	let mockPrivacyService: PrivacyService;
	let mockThemeService: ThemeService;
	let mockRouter: Router;

	let doc: Document;
	let mockProfile: WritableSignal<UserProfile | undefined>;
	let mockIsDarkMode: WritableSignal<boolean>;
	let mockIsPrivacyMode: WritableSignal<boolean>;
	let userSubject: BehaviorSubject<{ picture?: string } | null>;

	beforeEach(() => {
		mockProfile = signal<UserProfile | undefined>(undefined);
		mockIsDarkMode = signal<boolean>(false);
		mockIsPrivacyMode = signal<boolean>(false);
		userSubject = new BehaviorSubject<{ picture?: string } | null>(null);

		TestBed.configureTestingModule({
			imports: [
				App,
				MockDirective(RouterOutlet),
				MockDirective(ButtonDirective),
				MockComponent(Avatar),
				MockComponent(Toolbar),
				MockDirective(RouterLink),
				MockDirective(RouterLinkActive),
				MockComponent(Menu),
				MockComponent(Button),
				MockComponent(ConfirmDialog),
				MockComponent(Toast),
				MockComponent(GdprConsentComponent)
			],
			providers: [
				MockProvider(AuthService, {
					user$: userSubject.asObservable(),
					isAuthenticated$: of(false),
					logout: vi.fn()
				}),
				MockProvider(ThemeService, {
					isDarkMode: mockIsDarkMode,
					toggleTheme: vi.fn()
				}),
				MockProvider(PrivacyService, {
					isPrivacyMode: mockIsPrivacyMode,
					togglePrivacy: vi.fn()
				}),
				MockProvider(ProfileService, {
					profile: mockProfile,
					getProfile: vi.fn().mockReturnValue(of(buildMockProfile()))
				}),
				MockProvider(Router, { navigate: vi.fn().mockResolvedValue(true) })
			]
		});

		mockProfileService = TestBed.inject(ProfileService);
		mockAuthService = TestBed.inject(AuthService);
		mockPrivacyService = TestBed.inject(PrivacyService);
		mockThemeService = TestBed.inject(ThemeService);
		mockRouter = TestBed.inject(Router);
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

		it("should return the auth user picture when profile has no custom picture but user has one", () => {
			// GIVEN
			const stubbedPictureUrl = "http://example.com/user-pic.jpg";
			mockProfile.set(undefined);

			// WHEN
			userSubject.next({ picture: stubbedPictureUrl });
			fixture.detectChanges();

			// THEN
			expect(testSubject.avatarUrl()).toBe(stubbedPictureUrl);
		});
	});

	describe("isPrivacyMode", () => {
		it("should reflect the privacy service signal when false", () => {
			// GIVEN
			mockIsPrivacyMode.set(false);

			// WHEN
			fixture.detectChanges();

			// THEN
			expect(testSubject.isPrivacyMode()).toBe(false);
		});

		it("should reflect the privacy service signal when true", () => {
			// GIVEN
			mockIsPrivacyMode.set(true);

			// WHEN
			fixture.detectChanges();

			// THEN
			expect(testSubject.isPrivacyMode()).toBe(true);
		});
	});

	describe("profileMenuItems", () => {
		it("should contain five items including a separator", () => {
			// WHEN
			fixture.detectChanges();

			// THEN
			expect(testSubject.profileMenuItems()).toHaveLength(5);
		});

		it("should navigate to profile when the first item command is invoked", () => {
			// GIVEN
			const navigate = vi.spyOn(mockRouter, "navigate").mockResolvedValue(true);
			fixture.detectChanges();

			// WHEN
			testSubject.profileMenuItems()[0].command!({} as never);

			// THEN
			expect(navigate).toHaveBeenCalledWith(["/profile"]);
		});

		it("should toggle privacy when the second item command is invoked", () => {
			// GIVEN
			const togglePrivacy = vi.spyOn(mockPrivacyService, "togglePrivacy");
			fixture.detectChanges();

			// WHEN
			testSubject.profileMenuItems()[1].command!({} as never);

			// THEN
			expect(togglePrivacy).toHaveBeenCalledOnce();
		});

		it("should toggle theme when the third item command is invoked", () => {
			// GIVEN
			const toggleTheme = vi.spyOn(mockThemeService, "toggleTheme");
			fixture.detectChanges();

			// WHEN
			testSubject.profileMenuItems()[2].command!({} as never);

			// THEN
			expect(toggleTheme).toHaveBeenCalledOnce();
		});

		it("should call logout when the fifth item command is invoked", () => {
			// GIVEN
			const logout = vi.spyOn(mockAuthService, "logout");
			fixture.detectChanges();

			// WHEN
			testSubject.profileMenuItems()[4].command!({} as never);

			// THEN
			expect(logout).toHaveBeenCalled();
		});
	});

	describe("togglePrivacy", () => {
		it("should delegate to privacyService.togglePrivacy", () => {
			// GIVEN
			const togglePrivacy = vi.spyOn(mockPrivacyService, "togglePrivacy");

			// WHEN
			testSubject.togglePrivacy();

			// THEN
			expect(togglePrivacy).toHaveBeenCalledOnce();
		});
	});

	describe("onProfileClick", () => {
		it("should navigate to /profile", () => {
			// GIVEN
			const navigate = vi.spyOn(mockRouter, "navigate").mockResolvedValue(true);

			// WHEN
			testSubject.onProfileClick();

			// THEN
			expect(navigate).toHaveBeenCalledWith(["/profile"]);
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
