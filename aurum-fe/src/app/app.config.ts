import {
	ApplicationConfig,
	inject,
	provideAppInitializer,
	provideZonelessChangeDetection
} from "@angular/core";
import { registerLocaleData } from "@angular/common";
import localeIt from "@angular/common/locales/it";
import localeFr from "@angular/common/locales/fr";

registerLocaleData(localeIt);
registerLocaleData(localeFr);
import { provideRouter } from "@angular/router";
import { provideHttpClient, withInterceptors } from "@angular/common/http";
import { provideAuth0, authHttpInterceptorFn } from "@auth0/auth0-angular";

import { routes } from "./app.routes";
import { provideHighlightOptions } from "ngx-highlightjs";
import { providePrimeNG } from "primeng/config";
import { primengPreset, auth0Config, darkModeSelector } from "./app.utils";
import { ConfirmationService, MessageService } from "primeng/api";
import { NavigationService } from "./shared/services/navigation/navigation.service";

export const appConfig: ApplicationConfig = {
	providers: [
		MessageService,
		ConfirmationService,
		provideAppInitializer(() => {
			inject(NavigationService);
		}),
		provideZonelessChangeDetection(),
		provideRouter(routes),
		provideHttpClient(withInterceptors([authHttpInterceptorFn])),
		provideAuth0(auth0Config),
		provideHighlightOptions({
			fullLibraryLoader: () => import("highlight.js")
		}),
		providePrimeNG({
			theme: {
				preset: primengPreset,
				options: { darkModeSelector: `.${darkModeSelector}` }
			}
		})
	]
};
