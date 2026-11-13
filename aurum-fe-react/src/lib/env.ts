export const env = {
	apiUrl: import.meta.env.VITE_API_URL ?? "http://localhost:8080",
	auth0Domain: import.meta.env.VITE_AUTH0_DOMAIN ?? "aurum-api.eu.auth0.com",
	auth0ClientId: import.meta.env.VITE_AUTH0_CLIENT_ID ?? "ZHikUaUM2UOqcOHtTzdPephPVMWifRPj",
	auth0Audience: import.meta.env.VITE_AUTH0_AUDIENCE ?? "https://api.aurum.com"
} as const;
