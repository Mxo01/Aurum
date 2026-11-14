import { renderHook } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useProfile } from "@/features/profile/api";
import { PRIVACY_MASK } from "@/lib/format/money";
import { buildProfile } from "@/test/fixtures/portfolio";
import { usePrivacyStore } from "@/stores/privacy";
import { usePreferences } from "./use-preferences";

vi.mock("@/features/profile/api", () => ({ useProfile: vi.fn() }));

const useProfileMock = vi.mocked(useProfile);

function givenProfile(data: ReturnType<typeof buildProfile> | undefined) {
	useProfileMock.mockReturnValue({
		data
	} as unknown as ReturnType<typeof useProfile>);
}

const AMOUNT = 1234.5;

describe("usePreferences", () => {
	beforeEach(() => {
		usePrivacyStore.setState({ isPrivacy: false });
		givenProfile(undefined);
	});

	it("should fall back to the default currency and locale without a profile", () => {
		// GIVEN
		const testSubject = () => usePreferences();

		// WHEN
		const { result } = renderHook(testSubject);

		// THEN
		expect(result.current.currency).toBe("EUR");
		expect(result.current.locale).toBe("en-US");
		expect(result.current.money(AMOUNT)).toBe("€1,234.50");
	});

	it("should use the currency and locale of the profile", () => {
		// GIVEN
		givenProfile(buildProfile({ currency: "USD", locale: "it-IT" }));
		const testSubject = () => usePreferences();

		// WHEN
		const { result } = renderHook(testSubject);

		// THEN
		expect(result.current.currency).toBe("USD");
		expect(result.current.locale).toBe("it-IT");
		expect(result.current.money(AMOUNT)).toBe("1234,50 USD");
	});

	it("should expose the profile returned by the query", () => {
		// GIVEN
		const profile = buildProfile();
		givenProfile(profile);
		const testSubject = () => usePreferences();

		// WHEN
		const { result } = renderHook(testSubject);

		// THEN
		expect(result.current.profile).toBe(profile);
	});

	it("should mask money while privacy mode is on", () => {
		// GIVEN
		usePrivacyStore.setState({ isPrivacy: true });
		const testSubject = () => usePreferences();

		// WHEN
		const { result } = renderHook(testSubject);

		// THEN
		expect(result.current.isPrivacy).toBe(true);
		expect(result.current.money(AMOUNT)).toBe(PRIVACY_MASK);
	});

	it("should let the caller override the money options", () => {
		// GIVEN
		usePrivacyStore.setState({ isPrivacy: true });
		const testSubject = () => usePreferences();
		const { result } = renderHook(testSubject);

		// WHEN
		const expected = result.current.money(AMOUNT, { privacy: false, digits: 0 });

		// THEN
		expect(expected).toBe("€1,235");
	});

	it("should format percentages with the profile locale", () => {
		// GIVEN
		givenProfile(buildProfile({ locale: "it-IT" }));
		const testSubject = () => usePreferences();
		const { result } = renderHook(testSubject);

		// WHEN
		const expected = result.current.percent(-4.2);

		// THEN
		expect(expected).toBe("4,20%");
	});

	it("should format percentages with the requested digits", () => {
		// GIVEN
		const testSubject = () => usePreferences();
		const { result } = renderHook(testSubject);

		// WHEN
		const expected = result.current.percent(-4.2, 1);

		// THEN
		expect(expected).toBe("4.2%");
	});

	it("should format dates with the profile locale", () => {
		// GIVEN
		givenProfile(buildProfile({ locale: "fr-FR" }));
		const testSubject = () => usePreferences();
		const { result } = renderHook(testSubject);

		// WHEN
		const expected = result.current.date("2026-11-13");

		// THEN
		expect(expected).toBe("13 nov. 2026");
	});
});
