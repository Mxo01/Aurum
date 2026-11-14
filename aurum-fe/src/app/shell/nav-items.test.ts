import { describe, expect, it } from "vitest";
import { NAV_ITEMS } from "./nav-items";

describe("NAV_ITEMS", () => {
	it("should expose the three main sections", () => {
		// GIVEN
		const testSubject = NAV_ITEMS;

		// THEN
		expect(testSubject.map(item => item.to)).toEqual(["/dashboard", "/assets", "/cashflow"]);
		expect(testSubject.every(item => typeof item.icon === "object")).toBe(true);
	});
});
