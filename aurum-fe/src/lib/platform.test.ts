import { afterEach, describe, expect, it, vi } from "vitest";

async function loadPlatform(platform: string | undefined) {
	vi.resetModules();
	vi.stubGlobal("navigator", platform === undefined ? undefined : { platform });
	return import("./platform");
}

describe("platform", () => {
	afterEach(() => {
		vi.unstubAllGlobals();
	});

	it("should use the mac modifiers on apple platforms", async () => {
		// GIVEN
		const testSubject = await loadPlatform("MacIntel");

		// THEN
		expect(testSubject.isMac).toBe(true);
		expect(testSubject.commandLabel).toBe("⌘");
		expect(testSubject.optionLabel).toBe("⌥");
	});

	it("should use alt and ctrl on other platforms", async () => {
		// GIVEN
		const testSubject = await loadPlatform("Win32");

		// THEN
		expect(testSubject.isMac).toBe(false);
		expect(testSubject.commandLabel).toBe("Ctrl");
		expect(testSubject.optionLabel).toBe("Alt");
	});

	it("should fall back to alt and ctrl without a navigator", async () => {
		// GIVEN
		const testSubject = await loadPlatform(undefined);

		// THEN
		expect(testSubject.isMac).toBe(false);
		expect(testSubject.commandLabel).toBe("Ctrl");
	});
});
