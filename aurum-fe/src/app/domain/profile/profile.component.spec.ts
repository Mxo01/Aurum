import { ComponentFixture, TestBed } from "@angular/core/testing";
import { WritableSignal, signal } from "@angular/core";
import { MockComponent, MockDirective, MockModule, MockProvider } from "ng-mocks";
import { FormsModule } from "@angular/forms";
import { RouterLink, provideRouter } from "@angular/router";
import { Button } from "primeng/button";
import { IconField } from "primeng/iconfield";
import { InputIcon } from "primeng/inputicon";
import { InputText } from "primeng/inputtext";
import { SelectButton } from "primeng/selectbutton";
import { Select, SelectChangeEvent } from "primeng/select";
import { Divider } from "primeng/divider";
import { faker } from "@faker-js/faker";
import { of, throwError } from "rxjs";
import { AuthService } from "@auth0/auth0-angular";
import { MessageService, ConfirmationService } from "primeng/api";
import { McpService } from "./mcp/mcp.service";
import { McpSetupComponent } from "./mcp/mcp-setup.component";
import { GeneratedKey } from "./mcp/model/mcp.model";
import { ProfileComponent } from "./profile.component";
import { ProfileService } from "./profile.service";
import { ThemeService } from "../../shared/services/theme/theme.service";
import { NavigationService } from "../../shared/services/navigation/navigation.service";
import { Currency } from "./model/currency.model";
import { Locale } from "./model/locale.model";
import { UserProfile } from "./model/user-profile.model";

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
	let mockConfirmationService: ConfirmationService;
	let profileSignal: WritableSignal<UserProfile | undefined>;

	beforeEach(() => {
		profileSignal = signal<UserProfile | undefined>(undefined);
		TestBed.configureTestingModule({
			imports: [
				ProfileComponent,
				MockComponent(Button),
				MockComponent(IconField),
				MockComponent(InputIcon),
				MockModule(FormsModule),
				MockDirective(InputText),
				MockComponent(SelectButton),
				MockComponent(Select),
				MockDirective(RouterLink),
				MockComponent(Divider),
				MockComponent(McpSetupComponent)
			],
			providers: [
				MockProvider(ProfileService, {
					profile: profileSignal,
					getProfile: vi.fn().mockReturnValue(of(buildMockProfile())),
					updateName: vi.fn().mockReturnValue(of("mock-token")),
					updateCurrency: vi.fn().mockReturnValue(of(buildMockProfile())),
					updateLocale: vi.fn().mockReturnValue(of(buildMockProfile())),
					updatePicture: vi.fn().mockReturnValue(of(buildMockProfile())),
					deletePicture: vi.fn().mockReturnValue(of(buildMockProfile())),
					exportData: vi.fn().mockReturnValue(of({})),
					deleteProfile: vi.fn().mockReturnValue(of(undefined))
				}),
				MockProvider(AuthService, { user$: of(null), logout: vi.fn() }),
				MockProvider(ThemeService, { isDarkMode: signal(false), toggleTheme: vi.fn() }),
				MockProvider(NavigationService, { previousRoute: signal("/assets") }),
				MockProvider(ConfirmationService),
				MockProvider(McpService, {
					getKeyMeta: vi.fn().mockReturnValue(of(null)),
					generateKey: vi.fn().mockReturnValue(of({ key: "test-key" } as GeneratedKey)),
					revokeKey: vi.fn().mockReturnValue(of({})),
					mcpSseUrl: "http://test-api/sse"
				}),
				MockProvider(MessageService, { add: vi.fn() }),
				provideRouter([])
			]
		});
		fixture = TestBed.createComponent(ProfileComponent);
		testSubject = fixture.componentInstance;
		mockProfileService = TestBed.inject(ProfileService);
		mockConfirmationService = TestBed.inject(ConfirmationService);
		fixture.detectChanges();
	});

	describe("ngOnInit", () => {
		it("should call profileService.getProfile on init", () => {
			// GIVEN
			const getProfile = vi
				.spyOn(mockProfileService, "getProfile")
				.mockReturnValue(of(buildMockProfile()));

			// WHEN
			testSubject.ngOnInit();

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

		it("should return a falsy pictureUrl when there is no profile picture and no auth user", () => {
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

		it("should reset isUpdatingCurrency after completion", () => {
			// GIVEN
			vi.spyOn(mockProfileService, "updateCurrency").mockReturnValue(of(buildMockProfile()));

			// WHEN
			testSubject.chooseCurrency({ value: Currency.USD } as SelectChangeEvent);

			// THEN
			expect(testSubject.isUpdatingCurrency()).toBe(false);
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
		it("should call profileService.exportData and reset isExportingData after completion", () => {
			// GIVEN
			const exportData = vi.spyOn(mockProfileService, "exportData").mockReturnValue(of({}));

			// WHEN
			testSubject.exportData();

			// THEN
			expect(exportData).toHaveBeenCalled();
			expect(testSubject.isExportingData()).toBe(false);
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

		it("should reset isUpdatingLocale after completion", () => {
			// GIVEN
			vi.spyOn(mockProfileService, "updateLocale").mockReturnValue(of(buildMockProfile()));

			// WHEN
			testSubject.chooseLocale({ value: Locale.IT } as SelectChangeEvent);

			// THEN
			expect(testSubject.isUpdatingLocale()).toBe(false);
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
			vi.advanceTimersByTime(1000);

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
				vi.advanceTimersByTime(1000);
			}).not.toThrow();

			vi.useRealTimers();
		});
	});

	describe("onFileSelected", () => {
		it("should call profileService.updatePicture with the selected file and reset isUploadingPicture", () => {
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
			expect(testSubject.isUploadingPicture()).toBe(false);
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
			const confirm = vi.spyOn(mockConfirmationService, "confirm");

			// WHEN
			testSubject.deleteProfile(new MouseEvent("click"));

			// THEN
			expect(confirm).toHaveBeenCalled();
		});

		it("should call profileService.deleteProfile and reset isDeletingProfile when the user confirms", () => {
			// GIVEN
			vi.spyOn(mockConfirmationService, "confirm").mockImplementation(({ accept }) => accept?.());
			const deleteProfile = vi
				.spyOn(mockProfileService, "deleteProfile")
				.mockReturnValue(of(undefined));

			// WHEN
			testSubject.deleteProfile(new MouseEvent("click"));

			// THEN
			expect(deleteProfile).toHaveBeenCalled();
			expect(testSubject.isDeletingProfile()).toBe(false);
		});
	});
});
