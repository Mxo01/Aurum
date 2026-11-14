import { create } from "zustand";

export type Theme = "light" | "dark";

function readTheme(): Theme {
	if (typeof document === "undefined") return "dark";
	return document.documentElement.classList.contains("dark") ? "dark" : "light";
}

function applyTheme(theme: Theme) {
	document.documentElement.classList.toggle("dark", theme === "dark");
	document.documentElement.style.colorScheme = theme;
	window.localStorage.setItem("theme", theme);
}

interface ThemeState {
	theme: Theme;
	setTheme: (theme: Theme) => void;
	toggleTheme: () => void;
}

export const useThemeStore = create<ThemeState>(set => ({
	theme: readTheme(),
	setTheme: theme => {
		applyTheme(theme);
		set({ theme });
	},
	toggleTheme: () =>
		set(state => {
			const theme = state.theme === "dark" ? "light" : "dark";
			applyTheme(theme);
			return { theme };
		})
}));
