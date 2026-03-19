import { Injectable, inject, DOCUMENT, signal } from "@angular/core";
import { PrimeNG } from "primeng/config";
import { darkModeSelector } from "../../../app.utils";
import { Locale } from "../../../domain/profile/model/locale.model";
import { primengLocaleMap } from "../../primeng-locale.utils";

@Injectable({
	providedIn: "root"
})
export class ThemeService {
	private readonly document = inject(DOCUMENT);
	private readonly primeNG = inject(PrimeNG);

	isDarkMode = signal<boolean>(
		localStorage.getItem("theme") === "dark" ||
			(!localStorage.getItem("theme") &&
				globalThis.matchMedia("(prefers-color-scheme: dark)").matches)
	);

	applyLocale(locale: Locale) {
		this.primeNG.setTranslation(primengLocaleMap[locale]);
	}

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
