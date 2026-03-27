import { inject, LOCALE_ID, Pipe, PipeTransform } from "@angular/core";
import { CurrencyPipe } from "@angular/common";
import { PrivacyService } from "../services/privacy/privacy.service";

export const PRIVACY_PLACEHOLDER = "••••••";

@Pipe({
	name: "privacyCurrency",
	standalone: true,
	pure: false
})
export class PrivacyCurrencyPipe implements PipeTransform {
	private readonly privacyService = inject(PrivacyService);
	private readonly appLocale = inject(LOCALE_ID) as string;

	transform(
		value: number | string | null | undefined,
		currencyCode?: string,
		display: string | boolean = "symbol",
		digitsInfo?: string,
		locale?: string
	): string | null {
		if (this.privacyService.isPrivacyMode()) {
			return PRIVACY_PLACEHOLDER;
		}
		return new CurrencyPipe(locale ?? this.appLocale).transform(
			value,
			currencyCode,
			display,
			digitsInfo,
			locale
		);
	}
}
