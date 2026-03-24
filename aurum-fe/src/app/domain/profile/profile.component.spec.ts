import { ComponentFixture, TestBed } from "@angular/core/testing";
import { WritableSignal, signal } from "@angular/core";
import { MockProvider } from "ng-mocks";
import { faker } from "@faker-js/faker";
import { of, EMPTY, throwError } from "rxjs";
import { AuthService } from "@auth0/auth0-angular";
import { ProfileComponent } from "./profile.component";
import { ProfileService } from "./profile.service";
import { ThemeService } from "../../shared/services/theme/theme.service";
import { NavigationService } from "../../shared/services/navigation/navigation.service";
import { ConfirmationService } from "primeng/api";
import { Currency } from "./model/currency.model";
import { Locale } from "./model/locale.model";
import { UserProfile } from "./model/user-profile.model";
import { SelectChangeEvent } from "primeng/select";

const buildMockProfile = (overrides: Partial<UserProfile> = {}): UserProfile => ({
	id: faker.string.uuid(),
	email: faker.internet.email(),
	currency: Currency.EUR,
	locale: Locale.EN_US,
	...overrides
});

describe("ProfileComponent", () => {
	let fixture: ComponentFixture<ProfileComponent>;
	let testSubject: ProfileComponent;
	let mockProfileService: ProfileService;
	let profileSignal: WritableSignal<UserProfile | undefined>;

	beforeEach(() => {
		profileSignal = signal<UserProfile | undefined>(undefined);
		TestBed.overrideComponent(ProfileComponent, { set: { template: "", imports: [] } });
		TestBed.configureTestingModule({
			imports: [ProfileComponent],
			providers: [
				MockProvider(ProfileService, {
					profile: profileSignal,
					getProfile: vi.fn().mockReturnValue(of(buildMockProfile())),
					updateName: vi.fn().mockReturnValue(of(buildMockProfile())),
					updateCurrency: vi.fn().mockReturnValue(of(buildMockProfile())),
					updateLocale: vi.fn().mockReturnValue(of(buildMockProfile())),
					updatePicture: vi.fn().mockReturnValue(of(buildMockProfile())),
					deletePicture: vi.fn().mockReturnValue(of(buildMockProfile())),
					exportData: vi.fn().mockReturnValue(of({})),
					deleteProfile: vi.fn().mockReturnValue(of(undefined))
				}),
				MockProvider(AuthService, { user$: EMPTY }),
				MockProvider(ThemeService, { isDarkMode: signal(false), toggleTheme: vi.fn() }),
				MockProvider(NavigationService, { previousRoute: "/assets" }),
				MockProvider(ConfirmationService)
			]
		});
		fixture = TestBed.createComponent(ProfileComponent);
		testSubject = fixture.componentInstance;
		mockProfileService = TestBed.inject(ProfileService);
		fixture.detectChanges();
	});

	describe("ngOnInit", () => {
		it("should call profileService.getProfile on init", () => {
			// GIVEN
			const getProfile = vi
				.spyOn(mockProfileService, "getProfile")
				.mockReturnValue(of(buildMockProfile()));
			fixture = TestBed.createComponent(ProfileComponent);

			// WHEN
			fixture.detectChanges();

			// THEN
			expect(getProfile).toHaveBeenCalled();
		});
	});

	describe("pictureUrl", () => {
		it("should return the base64 data URL when profile has a custom picture", () => {
			// GIVEN
			profileSignal.set(buildMockProfile({ picture: "abc123" }));
			fixture.detectChanges();

			// THEN
			expect(testSubject.pictureUrl()).toContain("data:image/jpeg;base64,abc123");
		});

		it("should return a falsy pictureUrl when there is no profile picture", () => {
			// GIVEN
			profileSignal.set(buildMockProfile({ picture: undefined }));
			fixture.detectChanges();

			// THEN
			expect(testSubject.pictureUrl()).toBeFalsy();
		});
	});

	describe("hasPicture", () => {
		it("should return true when the profile has a custom picture", () => {
			// GIVEN
			profileSignal.set(buildMockProfile({ picture: "abc123" }));
			fixture.detectChanges();

			// THEN
			expect(testSubject.hasPicture()).toBe(true);
		});

		it("should return false when there is no custom picture", () => {
			// GIVEN
			profileSignal.set(buildMockProfile({ picture: undefined }));
			fixture.detectChanges();

			// THEN
			expect(testSubject.hasPicture()).toBe(false);
		});
	});

	describe("chooseCurrency", () => {
		it("should call profileService.updateCurrency with the selected currency", () => {
			// GIVEN
			const updateCurrency = vi
				.spyOn(mockProfileService, "updateCurrency")
				.mockReturnValue(of(buildMockProfile()));

			// WHEN
			testSubject.chooseCurrency({ value: Currency.USD } as SelectChangeEvent);

			// THEN
			expect(updateCurrency).toHaveBeenCalledWith(Currency.USD);
		});
	});

	describe("toggleTheme", () => {
		it("should call themeService.toggleTheme", () => {
			// GIVEN
			const toggleTheme = vi.spyOn(TestBed.inject(ThemeService), "toggleTheme");

			// WHEN
			testSubject.toggleTheme();

			// THEN
			expect(toggleTheme).toHaveBeenCalled();
		});
	});

	describe("exportData", () => {
		it("should call profileService.exportData", () => {
			// GIVEN
			const exportData = vi.spyOn(mockProfileService, "exportData").mockReturnValue(of({}));

			// WHEN
			testSubject.exportData();

			// THEN
			expect(exportData).toHaveBeenCalled();
		});
	});

	describe("clearPicture", () => {
		it("should call profileService.deletePicture", () => {
			// GIVEN
			const deletePicture = vi
				.spyOn(mockProfileService, "deletePicture")
				.mockReturnValue(of(buildMockProfile()));

			// WHEN
			testSubject.clearPicture();

			// THEN
			expect(deletePicture).toHaveBeenCalled();
		});
	});

	describe("chooseLocale", () => {
		it("should call profileService.updateLocale with the selected locale", () => {
			// GIVEN
			const updateLocale = vi
				.spyOn(mockProfileService, "updateLocale")
				.mockReturnValue(of(buildMockProfile()));

			// WHEN
			testSubject.chooseLocale({ value: Locale.IT } as SelectChangeEvent);

			// THEN
			expect(updateLocale).toHaveBeenCalledWith(Locale.IT);
		});
	});

	describe("updateName", () => {
		it("should call profileService.updateName after the debounce period", () => {
			// GIVEN
			vi.useFakeTimers();
			const stubbedName = faker.person.firstName();
			const updateName = vi
				.spyOn(mockProfileService, "updateName")
				.mockReturnValue(of("mock-token"));

			// WHEN
			testSubject.updateName(stubbedName);
			vi.advanceTimersByTime(500);

			// THEN
			expect(updateName).toHaveBeenCalledWith(stubbedName);

			vi.useRealTimers();
		});

		it("should swallow errors from profileService.updateName", () => {
			// GIVEN
			vi.useFakeTimers();
			vi.spyOn(mockProfileService, "updateName").mockReturnValue(
				throwError(() => new Error("fail"))
			);

			// WHEN / THEN — no error thrown
			expect(() => {
				testSubject.updateName(faker.person.firstName());
				vi.advanceTimersByTime(500);
			}).not.toThrow();

			vi.useRealTimers();
		});
	});

	describe("onFileSelected", () => {
		it("should call profileService.updatePicture with the selected file", () => {
			// GIVEN
			const mockFile = new File(["content"], "photo.jpg", { type: "image/jpeg" });
			const updatePicture = vi
				.spyOn(mockProfileService, "updatePicture")
				.mockReturnValue(of(buildMockProfile()));
			const mockEvent = { target: { files: [mockFile] } } as unknown as Event;

			// WHEN
			testSubject.onFileSelected(mockEvent);

			// THEN
			expect(updatePicture).toHaveBeenCalledWith(mockFile);
		});

		it("should not call updatePicture when no file is selected", () => {
			// GIVEN
			const updatePicture = vi.spyOn(mockProfileService, "updatePicture");
			const mockEvent = { target: { files: [] } } as unknown as Event;

			// WHEN
			testSubject.onFileSelected(mockEvent);

			// THEN
			expect(updatePicture).not.toHaveBeenCalled();
		});
	});

	describe("deleteProfile", () => {
		it("should call confirmationService.confirm", () => {
			// GIVEN
			const confirm = vi.spyOn(TestBed.inject(ConfirmationService), "confirm");

			// WHEN
			testSubject.deleteProfile(new MouseEvent("click"));

			// THEN
			expect(confirm).toHaveBeenCalled();
		});

		it("should call profileService.deleteProfile when the user confirms", () => {
			// GIVEN
			const mockConfirmationService = TestBed.inject(ConfirmationService);
			vi.spyOn(mockConfirmationService, "confirm").mockImplementation(({ accept }) => accept?.());
			const deleteProfile = vi
				.spyOn(mockProfileService, "deleteProfile")
				.mockReturnValue(of(undefined));

			// WHEN
			testSubject.deleteProfile(new MouseEvent("click"));

			// THEN
			expect(deleteProfile).toHaveBeenCalled();
		});
	});
});
