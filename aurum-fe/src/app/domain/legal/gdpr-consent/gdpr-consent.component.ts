import { ChangeDetectionStrategy, Component, signal } from "@angular/core";
import { RouterLink } from "@angular/router";
import { Button } from "primeng/button";

const CONSENT_KEY = "gdpr_consent_accepted";

@Component({
	selector: "app-gdpr-consent",
	standalone: true,
	imports: [RouterLink, Button],
	templateUrl: "./gdpr-consent.component.html",
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class GdprConsentComponent {
	protected readonly visible = signal(!localStorage.getItem(CONSENT_KEY));

	accept() {
		localStorage.setItem(CONSENT_KEY, "true");
		this.visible.set(false);
	}
}
