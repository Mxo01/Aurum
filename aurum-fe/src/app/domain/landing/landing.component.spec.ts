import { ComponentFixture, TestBed } from "@angular/core/testing";
import { signal } from "@angular/core";
import { MockComponent, MockDirective, MockProvider } from "ng-mocks";
import { RouterLink, Router } from "@angular/router";
import { Button } from "primeng/button";
import { AuthService } from "@auth0/auth0-angular";
import { Meta, Title } from "@angular/platform-browser";
import { of } from "rxjs";
import { LandingComponent } from "./landing.component";
import { ThemeService } from "../../shared/services/theme/theme.service";

describe("LandingComponent", () => {
	let fixture: ComponentFixture<LandingComponent>;
	let testSubject: LandingComponent;
	let mockRouter: Router;
	let mockTitleService: Title;
	let mockMetaService: Meta;
	let mockAuthService: AuthService;

	beforeEach(() => {
		TestBed.configureTestingModule({
			imports: [LandingComponent, MockDirective(RouterLink), MockComponent(Button)],
			providers: [
				MockProvider(AuthService, { isLoading$: of(false), isAuthenticated$: of(false) }),
				MockProvider(Router, { navigate: vi.fn() }),
				MockProvider(Title, { setTitle: vi.fn() }),
				MockProvider(Meta, { updateTag: vi.fn() }),
				MockProvider(ThemeService, { isDarkMode: signal(false), toggleTheme: vi.fn() })
			]
		});
		fixture = TestBed.createComponent(LandingComponent);
		testSubject = fixture.componentInstance;
		mockRouter = TestBed.inject(Router);
		mockTitleService = TestBed.inject(Title);
		mockMetaService = TestBed.inject(Meta);
		mockAuthService = TestBed.inject(AuthService);
	});

	describe("ngOnInit", () => {
		it("should set the page title", () => {
			// WHEN
			fixture.detectChanges();

			// THEN
			expect(mockTitleService.setTitle).toHaveBeenCalledWith("Aurum");
		});

		it("should update the description meta tag", () => {
			// WHEN
			fixture.detectChanges();

			// THEN
			expect(mockMetaService.updateTag).toHaveBeenCalledWith(
				expect.objectContaining({ name: "description" })
			);
		});

		it("should update og:title meta tag", () => {
			// WHEN
			fixture.detectChanges();

			// THEN
			expect(mockMetaService.updateTag).toHaveBeenCalledWith(
				expect.objectContaining({ property: "og:title", content: "Aurum" })
			);
		});

		it("should navigate to dashboard when user is already authenticated", () => {
			// GIVEN
			vi.spyOn(mockAuthService, "isLoading$", "get").mockReturnValue(of(false));
			vi.spyOn(mockAuthService, "isAuthenticated$", "get").mockReturnValue(of(true));

			// WHEN
			fixture.detectChanges();

			// THEN
			expect(mockRouter.navigate).toHaveBeenCalledWith(["/dashboard"], { replaceUrl: true });
		});

		it("should not navigate when user is not authenticated", () => {
			// GIVEN
			vi.spyOn(mockAuthService, "isLoading$", "get").mockReturnValue(of(false));
			vi.spyOn(mockAuthService, "isAuthenticated$", "get").mockReturnValue(of(false));

			// WHEN
			fixture.detectChanges();

			// THEN
			expect(mockRouter.navigate).not.toHaveBeenCalled();
		});

		it("should not navigate while auth is still loading", () => {
			// GIVEN
			vi.spyOn(mockAuthService, "isLoading$", "get").mockReturnValue(of(true));
			vi.spyOn(mockAuthService, "isAuthenticated$", "get").mockReturnValue(of(true));

			// WHEN
			fixture.detectChanges();

			// THEN
			expect(mockRouter.navigate).not.toHaveBeenCalled();
		});
	});

	describe("stat signals initial values", () => {
		it("should initialise stat100 to 0%", () => {
			// THEN
			expect(testSubject.stat100()).toBe("0%");
		});

		it("should initialise stat170 to 0+", () => {
			// THEN
			expect(testSubject.stat170()).toBe("0+");
		});

		it("should initialise statInf to 0", () => {
			// THEN
			expect(testSubject.statInf()).toBe("0");
		});
	});

	describe("hero dashboard signals initial values", () => {
		it("should initialise heroNetWorth to €0", () => {
			// THEN
			expect(testSubject.heroNetWorth()).toBe("€0");
		});

		it("should initialise heroTotalAssets to €0", () => {
			// THEN
			expect(testSubject.heroTotalAssets()).toBe("€0");
		});

		it("should initialise heroGrowth to +0.00%", () => {
			// THEN
			expect(testSubject.heroGrowth()).toBe("+0.00%");
		});

		it("should initialise heroSavingsRate to 0.0%", () => {
			// THEN
			expect(testSubject.heroSavingsRate()).toBe("0.0%");
		});
	});

	describe("dashboardFeatures", () => {
		it("should expose three dashboard feature items", () => {
			// THEN
			expect(testSubject.dashboardFeatures).toHaveLength(3);
		});
	});

	describe("currencies", () => {
		it("should expose the five primary display currencies", () => {
			// THEN
			expect(testSubject.currencies).toEqual(["EUR", "USD", "GBP", "CHF", "JPY"]);
		});
	});

	describe("login", () => {
		it("should call loginWithRedirect targeting the dashboard", () => {
			// GIVEN
			const loginWithRedirect = vi
				.spyOn(mockAuthService, "loginWithRedirect")
				.mockReturnValue(of(undefined));

			// WHEN
			testSubject.login();

			// THEN
			expect(loginWithRedirect).toHaveBeenCalledWith({
				appState: { target: "/dashboard" }
			});
		});
	});
});
