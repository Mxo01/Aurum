import { describe, expect, it } from "vitest";
import { buildSnapshot } from "./portfolio";

describe("buildSnapshot", () => {
	it("should build a snapshot recorded by the user", () => {
		// GIVEN
		const testSubject = buildSnapshot;

		// WHEN
		const expectedSnapshot = testSubject();

		// THEN
		expect(expectedSnapshot.source).toBe("USER");
	});

	it("should let the override replace the source", () => {
		// GIVEN
		const testSubject = buildSnapshot;

		// WHEN
		const expectedSnapshot = testSubject({ source: "SCHEDULER" });

		// THEN
		expect(expectedSnapshot.source).toBe("SCHEDULER");
	});
});
