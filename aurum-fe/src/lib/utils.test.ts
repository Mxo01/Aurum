import { describe, expect, it } from "vitest";
import { cn, isDefined } from "./utils";

describe("cn", () => {
	it("should merge conflicting classes", () => {
		// GIVEN
		const testSubject = cn;

		// WHEN
		const expected = testSubject("px-2", "px-4");

		// THEN
		expect(expected).toBe("px-4");
	});
});

describe("isDefined", () => {
	it("should accept a value", () => {
		// GIVEN
		const testSubject = isDefined;

		// THEN
		expect(testSubject(0)).toBe(true);
	});

	it("should reject null and undefined", () => {
		// GIVEN
		const testSubject = isDefined;

		// THEN
		expect(testSubject(null)).toBe(false);
		expect(testSubject(undefined)).toBe(false);
	});
});
