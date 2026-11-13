import { create } from "zustand";

function readPrivacy() {
	if (typeof window === "undefined") return false;
	return window.localStorage.getItem("privacy") === "true";
}

interface PrivacyState {
	isPrivacy: boolean;
	togglePrivacy: () => void;
}

export const usePrivacyStore = create<PrivacyState>(set => ({
	isPrivacy: readPrivacy(),
	togglePrivacy: () =>
		set(state => {
			const isPrivacy = !state.isPrivacy;
			window.localStorage.setItem("privacy", String(isPrivacy));
			return { isPrivacy };
		})
}));
