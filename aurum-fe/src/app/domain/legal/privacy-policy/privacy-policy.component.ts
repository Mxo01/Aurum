import { ChangeDetectionStrategy, Component } from "@angular/core";
import { RouterLink } from "@angular/router";
import { Button } from "primeng/button";

@Component({
	selector: "app-privacy-policy",
	standalone: true,
	imports: [RouterLink, Button],
	templateUrl: "./privacy-policy.component.html",
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class PrivacyPolicyComponent {}
