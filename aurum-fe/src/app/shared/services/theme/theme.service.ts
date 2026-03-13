import { Injectable, inject, DOCUMENT, signal } from "@angular/core";
import { darkModeSelector } from "../../../app.utils";

@Injectable({
	providedIn: "root"
})
export class ThemeService {
	private readonly document = inject(DOCUMENT);

	isDarkMode = signal<boolean>(
		localStorage.getItem("theme") === "dark" ||
			(!localStorage.getItem("theme") &&
				globalThis.matchMedia("(prefers-color-scheme: dark)").matches)
	);

	toggleTheme() {
		this.isDarkMode.update(dark => !dark);

		if (this.isDarkMode()) {
			this.document.documentElement.classList.add(darkModeSelector);
			localStorage.setItem("theme", "dark");
		} else {
			this.document.documentElement.classList.remove(darkModeSelector);
			localStorage.setItem("theme", "light");
		}
	}
}
