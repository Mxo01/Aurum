import { TestBed } from "@angular/core/testing";
import {
	HttpClient,
	HttpInterceptorFn,
	provideHttpClient,
	withInterceptors
} from "@angular/common/http";
import { HttpTestingController, provideHttpClientTesting } from "@angular/common/http/testing";
import { MockProvider } from "ng-mocks";
import { AuthService } from "@auth0/auth0-angular";
import { MessageService } from "primeng/api";
import { lastValueFrom } from "rxjs";
import { errorInterceptor } from "./error.interceptor";

describe("errorInterceptor", () => {
	let httpController: HttpTestingController;
	let httpClient: HttpClient;
	let mockAuthService: AuthService;
	let mockMessageService: MessageService;

	beforeEach(() => {
		TestBed.configureTestingModule({
			providers: [
				provideHttpClient(withInterceptors([errorInterceptor as HttpInterceptorFn])),
				provideHttpClientTesting(),
				MockProvider(AuthService, { logout: vi.fn() }),
				MockProvider(MessageService, { add: vi.fn() })
			]
		});
		httpController = TestBed.inject(HttpTestingController);
		httpClient = TestBed.inject(HttpClient);
		mockAuthService = TestBed.inject(AuthService);
		mockMessageService = TestBed.inject(MessageService);
	});

	afterEach(() => httpController.verify());

	describe("on 401", () => {
		it("should call authService.logout", async () => {
			// GIVEN / WHEN
			const result = lastValueFrom(httpClient.get("/test")).catch(() => {
				void 0;
			});
			httpController.expectOne("/test").flush(null, { status: 401, statusText: "Unauthorized" });
			await result;

			// THEN
			expect(mockAuthService.logout).toHaveBeenCalled();
		});
	});

	describe("on 403", () => {
		it("should show an access denied error message", async () => {
			// GIVEN / WHEN
			const result = lastValueFrom(httpClient.get("/test")).catch(() => {
				void 0;
			});
			httpController.expectOne("/test").flush(null, { status: 403, statusText: "Forbidden" });
			await result;

			// THEN
			expect(mockMessageService.add).toHaveBeenCalledWith(
				expect.objectContaining({ severity: "error", summary: "Access denied" })
			);
		});
	});

	describe("on 500+", () => {
		it("should show a generic server error message", async () => {
			// GIVEN / WHEN
			const result = lastValueFrom(httpClient.get("/test")).catch(() => {
				void 0;
			});
			httpController
				.expectOne("/test")
				.flush(null, { status: 500, statusText: "Internal Server Error" });
			await result;

			// THEN
			expect(mockMessageService.add).toHaveBeenCalledWith(
				expect.objectContaining({ severity: "error" })
			);
		});
	});

	describe("on any error", () => {
		it("should rethrow the error so callers can handle it", async () => {
			// GIVEN / WHEN
			const result = lastValueFrom(httpClient.get("/test"));
			httpController.expectOne("/test").flush(null, { status: 404, statusText: "Not Found" });

			// THEN
			await expect(result).rejects.toBeDefined();
		});
	});

	describe("on 404", () => {
		it("should not call authService.logout or show a message", async () => {
			// GIVEN / WHEN
			const result = lastValueFrom(httpClient.get("/test")).catch(() => {
				void 0;
			});
			httpController.expectOne("/test").flush(null, { status: 404, statusText: "Not Found" });
			await result;

			// THEN
			expect(mockAuthService.logout).not.toHaveBeenCalled();
			expect(mockMessageService.add).not.toHaveBeenCalled();
		});
	});
});
