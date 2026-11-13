import { beforeEach, describe, expect, it } from "vitest";
import { useThemeStore } from "./theme";

describe("theme store", () => {
	beforeEach(() => {
		document.documentElement.className = "";
		window.localStorage.clear();
		useThemeStore.setState({ theme: "light" });
	});

	describe("setTheme", () => {
		it("should add the dark class and persist the choice", () => {
			// GIVEN
			const testSubject = useThemeStore.getState();

			// WHEN
			testSubject.setTheme("dark");

			// THEN
			expect(document.documentElement.classList.contains("dark")).toBe(true);
			expect(window.localStorage.getItem("theme")).toBe("dark");
			expect(useThemeStore.getState().theme).toBe("dark");
		});

		it("should remove the dark class when switching to light", () => {
			// GIVEN
			useThemeStore.getState().setTheme("dark");

			// WHEN
			useThemeStore.getState().setTheme("light");

			// THEN
			expect(document.documentElement.classList.contains("dark")).toBe(false);
			expect(window.localStorage.getItem("theme")).toBe("light");
		});
	});

	describe("toggleTheme", () => {
		it("should flip the current theme", () => {
			// GIVEN
			const testSubject = useThemeStore.getState();

			// WHEN
			testSubject.toggleTheme();

			// THEN
			expect(useThemeStore.getState().theme).toBe("dark");
		});

		it("should flip back to light", () => {
			// GIVEN
			useThemeStore.setState({ theme: "dark" });

			// WHEN
			useThemeStore.getState().toggleTheme();

			// THEN
			expect(useThemeStore.getState().theme).toBe("light");
		});
	});
});
