import { provideHttpClient } from "@angular/common/http";
import { HttpTestingController, provideHttpClientTesting } from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { faker } from "@faker-js/faker";
import { AuthService } from "@auth0/auth0-angular";
import { MockProvider } from "ng-mocks";
import { MessageService } from "primeng/api";
import { of, lastValueFrom } from "rxjs";
import { Currency } from "./model/currency.model";
import { Locale } from "./model/locale.model";
import { UserProfile } from "./model/user-profile.model";
import { ProfileService } from "./profile.service";

const buildMockProfile = (): UserProfile => ({
	id: faker.string.uuid(),
	email: faker.internet.email(),
	currency: Currency.EUR,
	locale: Locale.EN_US,
	picture: faker.internet.url()
});

describe("ProfileService", () => {
	let testSubject: ProfileService;
	let httpController: HttpTestingController;

	beforeEach(() => {
		TestBed.configureTestingModule({
			providers: [
				ProfileService,
				MockProvider(MessageService),
				MockProvider(AuthService, {
					getAccessTokenSilently: vi.fn().mockReturnValue(of("mock-token")),
					logout: vi.fn().mockReturnValue(of(undefined))
				}),
				provideHttpClient(),
				provideHttpClientTesting()
			]
		});
		testSubject = TestBed.inject(ProfileService);
		httpController = TestBed.inject(HttpTestingController);
	});

	afterEach(() => {
		httpController.verify();
	});

	describe("getProfile", () => {
		it("should return the profile and update the profile signal on success", async () => {
			// GIVEN
			const stubbedProfile = buildMockProfile();

			// WHEN
			const result = lastValueFrom(testSubject.getProfile());
			httpController
				.expectOne(req => req.method === "GET" && req.url.endsWith("/users"))
				.flush(stubbedProfile);
			await result;

			// THEN
			expect(testSubject.profile()).toEqual(stubbedProfile);
		});

		it("should throw on error", async () => {
			// WHEN
			const result = lastValueFrom(testSubject.getProfile());
			httpController
				.expectOne(req => req.method === "GET" && req.url.endsWith("/users"))
				.flush(null, { status: 500, statusText: "Server Error" });

			// THEN
			await expect(result).rejects.toThrow();
		});
	});

	describe("updateCurrency", () => {
		it("should make a PUT request then refetch the profile", async () => {
			// GIVEN
			const stubbedCurrency = faker.helpers.arrayElement(Object.values(Currency));
			const stubbedProfile = buildMockProfile();

			// WHEN
			const result = lastValueFrom(testSubject.updateCurrency(stubbedCurrency));
			httpController
				.expectOne(req => req.method === "PUT" && req.url.endsWith("/users/currency"))
				.flush(null);
			httpController
				.expectOne(req => req.method === "GET" && req.url.endsWith("/users"))
				.flush(stubbedProfile);

			// THEN
			await result;
		});

		it("should throw on error", async () => {
			// WHEN
			const result = lastValueFrom(testSubject.updateCurrency(Currency.EUR));
			httpController
				.expectOne(req => req.method === "PUT" && req.url.endsWith("/users/currency"))
				.flush(null, { status: 500, statusText: "Server Error" });

			// THEN
			await expect(result).rejects.toThrow();
		});
	});

	describe("updateLocale", () => {
		it("should make a PUT request then refetch the profile", async () => {
			// GIVEN
			const stubbedLocale = faker.helpers.arrayElement(Object.values(Locale));
			const stubbedProfile = buildMockProfile();

			// WHEN
			const result = lastValueFrom(testSubject.updateLocale(stubbedLocale));
			httpController
				.expectOne(req => req.method === "PUT" && req.url.endsWith("/users/locale"))
				.flush(null);
			httpController
				.expectOne(req => req.method === "GET" && req.url.endsWith("/users"))
				.flush(stubbedProfile);

			// THEN
			await result;
		});

		it("should throw on error", async () => {
			// WHEN
			const result = lastValueFrom(testSubject.updateLocale(Locale.EN_US));
			httpController
				.expectOne(req => req.method === "PUT" && req.url.endsWith("/users/locale"))
				.flush(null, { status: 500, statusText: "Server Error" });

			// THEN
			await expect(result).rejects.toThrow();
		});
	});

	describe("updateName", () => {
		it("should make a PUT request then silently refresh the access token", async () => {
			// GIVEN
			const stubbedName = faker.person.firstName();
			const authService = TestBed.inject(AuthService);

			// WHEN
			const result = lastValueFrom(testSubject.updateName(stubbedName));
			httpController
				.expectOne(req => req.method === "PUT" && req.url.endsWith("/users"))
				.flush(null);
			await result;

			// THEN
			expect(authService.getAccessTokenSilently).toHaveBeenCalledOnce();
		});

		it("should throw on error", async () => {
			// WHEN
			const result = lastValueFrom(testSubject.updateName(faker.person.firstName()));
			httpController
				.expectOne(req => req.method === "PUT" && req.url.endsWith("/users"))
				.flush(null, { status: 500, statusText: "Server Error" });

			// THEN
			await expect(result).rejects.toThrow();
		});
	});

	describe("updatePicture", () => {
		it("should make a POST request with FormData then refetch the profile", async () => {
			// GIVEN
			const stubbedFile = new File(["content"], "avatar.jpg", { type: "image/jpeg" });
			const stubbedProfile = buildMockProfile();

			// WHEN
			const result = lastValueFrom(testSubject.updatePicture(stubbedFile));
			const req = httpController.expectOne(
				req => req.method === "POST" && req.url.endsWith("/users/picture")
			);
			expect(req.request.body).toBeInstanceOf(FormData);
			req.flush(null);
			httpController
				.expectOne(req => req.method === "GET" && req.url.endsWith("/users"))
				.flush(stubbedProfile);

			// THEN
			await result;
		});

		it("should throw on error", async () => {
			// GIVEN
			const stubbedFile = new File(["content"], "avatar.jpg", { type: "image/jpeg" });

			// WHEN
			const result = lastValueFrom(testSubject.updatePicture(stubbedFile));
			httpController
				.expectOne(req => req.method === "POST" && req.url.endsWith("/users/picture"))
				.flush(null, { status: 500, statusText: "Server Error" });

			// THEN
			await expect(result).rejects.toThrow();
		});
	});

	describe("deletePicture", () => {
		it("should make a DELETE request then refetch the profile", async () => {
			// GIVEN
			const stubbedProfile = buildMockProfile();

			// WHEN
			const result = lastValueFrom(testSubject.deletePicture());
			httpController
				.expectOne(req => req.method === "DELETE" && req.url.endsWith("/users/picture"))
				.flush(null);
			httpController
				.expectOne(req => req.method === "GET" && req.url.endsWith("/users"))
				.flush(stubbedProfile);

			// THEN
			await result;
		});

		it("should throw on error", async () => {
			// WHEN
			const result = lastValueFrom(testSubject.deletePicture());
			httpController
				.expectOne(req => req.method === "DELETE" && req.url.endsWith("/users/picture"))
				.flush(null, { status: 500, statusText: "Server Error" });

			// THEN
			await expect(result).rejects.toThrow();
		});
	});

	describe("deleteProfile", () => {
		it("should make a DELETE request then call authService.logout on success", async () => {
			// GIVEN
			const authService = TestBed.inject(AuthService);

			// WHEN
			const result = lastValueFrom(testSubject.deleteProfile());
			httpController
				.expectOne(req => req.method === "DELETE" && req.url.endsWith("/users"))
				.flush(null);

			// THEN
			await result;
			expect(authService.logout).toHaveBeenCalledOnce();
		});

		it("should throw on error", async () => {
			// WHEN
			const result = lastValueFrom(testSubject.deleteProfile());
			httpController
				.expectOne(req => req.method === "DELETE" && req.url.endsWith("/users"))
				.flush(null, { status: 500, statusText: "Server Error" });

			// THEN
			await expect(result).rejects.toThrow();
		});
	});

	describe("exportData", () => {
		it("should make a GET request to the export endpoint", async () => {
			// GIVEN
			vi.spyOn(URL, "createObjectURL").mockReturnValue("blob:mock");
			vi.spyOn(URL, "revokeObjectURL").mockImplementation(vi.fn());

			// WHEN
			const result = lastValueFrom(testSubject.exportData());
			httpController
				.expectOne(req => req.method === "GET" && req.url.endsWith("/users/export"))
				.flush({ data: faker.lorem.words() });

			// THEN
			await result;
		});

		it("should throw on error", async () => {
			// WHEN
			const result = lastValueFrom(testSubject.exportData());
			httpController
				.expectOne(req => req.method === "GET" && req.url.endsWith("/users/export"))
				.flush(null, { status: 500, statusText: "Server Error" });

			// THEN
			await expect(result).rejects.toThrow();
		});
	});
});
