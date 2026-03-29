import { TestBed } from "@angular/core/testing";
import { NavigationEnd, Router } from "@angular/router";
import { faker } from "@faker-js/faker";
import { MockProvider } from "ng-mocks";
import { Subject } from "rxjs";
import { NavigationService } from "./navigation.service";

describe("NavigationService", () => {
	let testSubject: NavigationService;

	let routerEvents$: Subject<NavigationEnd>;

	beforeEach(() => {
		routerEvents$ = new Subject<NavigationEnd>();

		TestBed.configureTestingModule({
			providers: [NavigationService, MockProvider(Router, { events: routerEvents$.asObservable() })]
		});

		testSubject = TestBed.inject(NavigationService);
	});

	describe("previousRoute", () => {
		it("should return /dashboard when no navigation has occurred", () => {
			expect(testSubject.previousRoute()).toBe("/dashboard");
		});

		it("should return the previous route after multiple navigations", () => {
			// GIVEN
			const stubbedFirstRoute = `/${faker.lorem.word()}`;
			const stubbedLastRoute = `/${faker.lorem.word()}`;

			// WHEN
			routerEvents$.next(new NavigationEnd(1, stubbedFirstRoute, stubbedFirstRoute));
			routerEvents$.next(new NavigationEnd(2, stubbedLastRoute, stubbedLastRoute));

			// THEN
			expect(testSubject.previousRoute()).toBe(stubbedFirstRoute);
		});

		it("should return /dashboard after a single navigation", () => {
			// GIVEN
			const stubbedRoute = `/${faker.lorem.word()}`;

			// WHEN
			routerEvents$.next(new NavigationEnd(1, stubbedRoute, stubbedRoute));

			// THEN
			expect(testSubject.previousRoute()).toBe("/dashboard");
		});
	});
});
