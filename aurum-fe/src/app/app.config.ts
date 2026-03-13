import { ApplicationConfig, provideZonelessChangeDetection } from "@angular/core";
import { provideRouter } from "@angular/router";
import { provideHttpClient, withInterceptors } from "@angular/common/http";
import { provideAuth0, authHttpInterceptorFn } from "@auth0/auth0-angular";

import { routes } from "./app.routes";
import { providePrimeNG } from "primeng/config";
import { primengPreset, auth0Config, darkModeSelector } from "./app.utils";
import { ConfirmationService, MessageService } from "primeng/api";

export const appConfig: ApplicationConfig = {
	providers: [
		MessageService,
		ConfirmationService,
		provideZonelessChangeDetection(),
		provideRouter(routes),
		provideHttpClient(withInterceptors([authHttpInterceptorFn])),
		provideAuth0(auth0Config),
		providePrimeNG({
			theme: {
				preset: primengPreset,
				options: { darkModeSelector: `.${darkModeSelector}` }
			}
		})
	]
};
