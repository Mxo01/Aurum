import { Injectable, signal } from "@angular/core";

@Injectable({
	providedIn: "root"
})
export class PrivacyService {
	isPrivacyMode = signal<boolean>(localStorage.getItem("privacy") === "true");

	togglePrivacy() {
		this.isPrivacyMode.update(v => !v);
		localStorage.setItem("privacy", this.isPrivacyMode() ? "true" : "false");
	}
}
