import { ChangeDetectionStrategy, Component, signal } from "@angular/core";
import { RouterLink } from "@angular/router";
import { Button } from "primeng/button";

const CONSENT_KEY = "gdpr_consent_accepted";

@Component({
	selector: "app-gdpr-consent",
	standalone: true,
	imports: [RouterLink, Button],
	changeDetection: ChangeDetectionStrategy.OnPush,
	template: `
		@if (visible()) {
			<div
				class="fixed bottom-0 left-0 right-0 z-50 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 px-4 py-3 bg-surface-900 dark:bg-surface-800 border-t border-surface-700 shadow-xl text-sm text-surface-100"
			>
				<p class="flex-1">
					Aurum stores your financial data to provide the service. By continuing, you agree to our
					<a
						routerLink="/privacy"
						class="underline hover:text-primary ml-1"
					>
						Privacy Policy
					</a>
					. We do not use tracking cookies or share your data with third parties.
				</p>
				<div class="flex gap-2 shrink-0">
					<p-button
						label="Accept"
						size="small"
						(onClick)="accept()"
					/>
				</div>
			</div>
		}
	`
})
export class GdprConsentComponent {
	protected readonly visible = signal(!localStorage.getItem(CONSENT_KEY));

	accept() {
		localStorage.setItem(CONSENT_KEY, "true");
		this.visible.set(false);
	}
}
