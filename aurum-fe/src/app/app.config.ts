import {
	ApplicationConfig,
	inject,
	provideAppInitializer,
	provideZonelessChangeDetection
} from "@angular/core";
import { registerLocaleData } from "@angular/common";
import localeIt from "@angular/common/locales/it";
import localeFr from "@angular/common/locales/fr";
import localeDe from "@angular/common/locales/de";
import localeEnGb from "@angular/common/locales/en-GB";
import localeZhHans from "@angular/common/locales/zh-Hans";

registerLocaleData(localeIt);
registerLocaleData(localeFr);
registerLocaleData(localeDe);
registerLocaleData(localeEnGb);
registerLocaleData(localeZhHans, "zh-CN");
import { provideRouter } from "@angular/router";
import { provideHttpClient, withInterceptors } from "@angular/common/http";
import { provideAuth0, authHttpInterceptorFn } from "@auth0/auth0-angular";

import { routes } from "./app.routes";
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
		providePrimeNG({
			theme: {
				preset: primengPreset,
				options: { darkModeSelector: `.${darkModeSelector}` }
			}
		})
	]
};
