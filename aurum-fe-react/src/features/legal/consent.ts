export const CONSENT_KEY = "gdpr_consent_accepted";

export function readConsent() {
	if (typeof window === "undefined") return false;
	return window.localStorage.getItem(CONSENT_KEY) === "true";
}

export function storeConsent() {
	window.localStorage.setItem(CONSENT_KEY, "true");
}
