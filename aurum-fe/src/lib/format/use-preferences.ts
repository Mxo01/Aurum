import { useCallback } from "react";
import { useProfile } from "@/features/profile/api";
import { usePrivacyStore } from "@/stores/privacy";
import type { Currency, Locale } from "@/lib/api/types";
import { formatCurrency, formatPercent, type MoneyOptions } from "./money";
import { formatDate } from "./date";

export function usePreferences() {
	const { data: profile } = useProfile();
	const isPrivacy = usePrivacyStore(state => state.isPrivacy);

	const currency: Currency = profile?.currency ?? "EUR";
	const locale: Locale = profile?.locale ?? "en-US";

	const money = useCallback(
		(value: number, options: Partial<MoneyOptions> = {}) =>
			formatCurrency(value, { currency, locale, privacy: isPrivacy, ...options }),
		[currency, locale, isPrivacy]
	);

	const percent = useCallback(
		(value: number, digits = 2) => formatPercent(value, locale, digits),
		[locale]
	);

	const date = useCallback((value: string | Date) => formatDate(value, locale), [locale]);

	return { profile, currency, locale, isPrivacy, money, percent, date };
}
