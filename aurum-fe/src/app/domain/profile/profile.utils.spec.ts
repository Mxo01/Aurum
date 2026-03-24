import { getCurrencySymbol } from "./profile.utils";

describe("getCurrencySymbol", () => {
	it("should return the euro symbol for EUR", () => {
		// GIVEN / WHEN
		const symbol = getCurrencySymbol("EUR");

		// THEN
		expect(symbol).toBe("€");
	});

	it("should return the dollar symbol for USD", () => {
		// GIVEN / WHEN
		const symbol = getCurrencySymbol("USD");

		// THEN
		expect(symbol).toBe("$");
	});

	it("should return the pound symbol for GBP", () => {
		// GIVEN / WHEN
		const symbol = getCurrencySymbol("GBP");

		// THEN
		expect(symbol).toBe("£");
	});

	it("should return the currency code as fallback for an unrecognized currency", () => {
		// GIVEN / WHEN
		const symbol = getCurrencySymbol("INVALID");

		// THEN
		expect(symbol).toBe("INVALID");
	});
});
