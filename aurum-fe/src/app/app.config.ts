import {
	ApplicationConfig,
	provideBrowserGlobalErrorListeners,
	provideZonelessChangeDetection
} from "@angular/core";
import { provideRouter } from "@angular/router";
import { provideHttpClient, withInterceptors } from "@angular/common/http";
import { provideAuth, LogLevel } from "angular-auth-oidc-client";

import { routes } from "./app.routes";
import { providePrimeNG } from "primeng/config";
import Aura from "@primeuix/themes/aura";
import { definePreset } from "@primeuix/themes";
import { authInterceptor } from "./core/auth/auth.interceptor";

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

export const appConfig: ApplicationConfig = {
	providers: [
		provideZonelessChangeDetection(),
		provideBrowserGlobalErrorListeners(),
		provideRouter(routes),
		provideHttpClient(withInterceptors([authInterceptor])),
		provideAuth({
			config: {
				authority: "https://AUTHENTIK_URL/application/o/APPLICATION_SLUG/", // Replace with your Authentik URL
				redirectUrl: window.location.origin,
				postLogoutRedirectUri: window.location.origin,
				clientId: "YOUR_CLIENT_ID", // Replace with your Authentik Client ID
				scope: "openid profile email offline_access",
				responseType: "code",
				silentRenew: true,
				useRefreshToken: true,
				logLevel: LogLevel.Debug
			}
		}),
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
