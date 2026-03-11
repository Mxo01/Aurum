import { ApplicationConfig, provideZonelessChangeDetection } from "@angular/core";
import { provideRouter } from "@angular/router";
import { provideHttpClient, withInterceptors } from "@angular/common/http";
import { provideAuth0, authHttpInterceptorFn, AuthConfig } from "@auth0/auth0-angular";

import { routes } from "./app.routes";
import { providePrimeNG } from "primeng/config";
import Aura from "@primeuix/themes/aura";
import { definePreset } from "@primeuix/themes";

const primengPreset = definePreset(Aura, {
	semantic: {
		primary: {
			50: "{blue.50}",
			100: "{blue.100}",
			200: "{blue.200}",
			300: "{blue.300}",
			400: "{blue.400}",
			500: "{blue.500}",
			600: "{blue.600}",
			700: "{blue.700}",
			800: "{blue.800}",
			900: "{blue.900}",
			950: "{blue.950}"
		}
	}
});

const auth0Config: AuthConfig = {
	domain: "aurum-api.eu.auth0.com",
	clientId: "ZHikUaUM2UOqcOHtTzdPephPVMWifRPj",
	authorizationParams: {
		redirect_uri: window.location.origin,
		audience: "https://api.aurum.com"
	},
	httpInterceptor: {
		allowedList: [
			{
				uri: "http://localhost:8080/api/*",
				tokenOptions: {
					authorizationParams: {
						audience: "https://api.aurum.com"
					}
				}
			}
		]
	}
};

export const appConfig: ApplicationConfig = {
	providers: [
		provideZonelessChangeDetection(),
		provideRouter(routes),
		provideHttpClient(withInterceptors([authHttpInterceptorFn])),
		provideAuth0(auth0Config),
		providePrimeNG({
			theme: {
				preset: primengPreset,
				options: {
					darkModeSelector: "none"
				}
			}
		})
	]
};
