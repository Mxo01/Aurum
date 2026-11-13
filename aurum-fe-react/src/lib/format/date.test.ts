import { describe, expect, it } from "vitest";
import { formatDate, formatDatePattern, parseDateOnly, toDateOnly, todayISO } from "./date";

describe("date formatting", () => {
	describe("parseDateOnly", () => {
		it("should parse a date-only string at local midnight", () => {
			// GIVEN
			const testSubject = parseDateOnly;

			// WHEN
			const expected = testSubject("2026-11-13");

			// THEN
			expect(expected.getFullYear()).toBe(2026);
			expect(expected.getMonth()).toBe(10);
			expect(expected.getDate()).toBe(13);
			expect(expected.getHours()).toBe(0);
		});
	});

	describe("formatDate", () => {
		it("should format a date-only string in the requested locale", () => {
			// GIVEN
			const testSubject = formatDate;

			// WHEN
			const expected = testSubject("2026-11-13", "en-US");

			// THEN
			expect(expected).toBe("Nov 13, 2026");
		});

		it("should accept a Date instance", () => {
			// GIVEN
			const testSubject = formatDate;

			// WHEN
			const expected = testSubject(new Date(2026, 0, 2), "en-US");

			// THEN
			expect(expected).toBe("Jan 2, 2026");
		});
	});

	describe("toDateOnly", () => {
		it("should render a Date as YYYY-MM-DD using local fields", () => {
			// GIVEN
			const testSubject = toDateOnly;

			// WHEN
			const expected = testSubject(new Date(2026, 10, 5));

			// THEN
			expect(expected).toBe("2026-11-05");
		});
	});

	describe("todayISO", () => {
		it("should match the local date of today", () => {
			// GIVEN
			const testSubject = todayISO;
			const now = new Date();

			// WHEN
			const expected = testSubject();

			// THEN
			expect(expected).toBe(toDateOnly(now));
		});
	});

	describe("formatDatePattern", () => {
		it("should format with the requested pattern", () => {
			// GIVEN
			const testSubject = formatDatePattern;

			// WHEN
			const expected = testSubject("2026-11-13", "dd/MM/yyyy");

			// THEN
			expect(expected).toBe("13/11/2026");
		});
	});
});
