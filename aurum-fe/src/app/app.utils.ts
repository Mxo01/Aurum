import Aura from "@primeuix/themes/aura";
import { definePreset } from "@primeuix/themes";
import { AuthConfig } from "@auth0/auth0-angular";
import { environment } from "../environments/environment";

export const primengPreset = definePreset(Aura, {
	semantic: {
		primary: {
			50: "{zinc.50}",
			100: "{zinc.100}",
			200: "{zinc.200}",
			300: "{zinc.300}",
			400: "{zinc.400}",
			500: "{zinc.500}",
			600: "{zinc.600}",
			700: "{zinc.700}",
			800: "{zinc.800}",
			900: "{zinc.900}",
			950: "{zinc.950}"
		},
		colorScheme: {
			light: {
				primary: {
					color: "{zinc.950}",
					contrastColor: "#ffffff",
					hoverColor: "{zinc.800}",
					activeColor: "{zinc.700}"
				},
				highlight: {
					background: "{zinc.950}",
					focusBackground: "{zinc.800}",
					color: "#ffffff",
					focusColor: "#ffffff"
				}
			},
			dark: {
				primary: {
					color: "{zinc.50}",
					contrastColor: "{zinc.950}",
					hoverColor: "{zinc.200}",
					activeColor: "{zinc.300}"
				},
				highlight: {
					background: "{zinc.50}",
					focusBackground: "{zinc.200}",
					color: "{zinc.950}",
					focusColor: "{zinc.950}"
				}
			}
		}
	}
});

export const auth0Config: AuthConfig = {
	domain: environment.auth0Domain,
	clientId: environment.auth0ClientId,
	authorizationParams: {
		redirect_uri: window.location.origin,
		audience: environment.auth0Audience
	},
	httpInterceptor: {
		allowedList: [
			{
				uri: `${environment.apiUrl}/*`,
				tokenOptions: {
					authorizationParams: {
						audience: environment.auth0Audience
					}
				}
			}
		]
	}
};

export const darkModeSelector = "p-dark";
