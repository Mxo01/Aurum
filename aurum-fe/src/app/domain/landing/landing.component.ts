import { ChangeDetectionStrategy, Component, inject, OnInit } from "@angular/core";
import { Router, RouterLink } from "@angular/router";
import { AuthService } from "@auth0/auth0-angular";
import { Meta, Title } from "@angular/platform-browser";
import { Button } from "primeng/button";
import { ThemeService } from "../../shared/services/theme/theme.service";
import { take } from "rxjs";

@Component({
	selector: "app-landing",
	standalone: true,
	imports: [RouterLink, Button],
	templateUrl: "./landing.component.html",
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class LandingComponent implements OnInit {
	private readonly authService = inject(AuthService);
	private readonly router = inject(Router);
	private readonly titleService = inject(Title);
	private readonly metaService = inject(Meta);
	readonly themeService = inject(ThemeService);

	ngOnInit() {
		this.titleService.setTitle("Aurum");
		this.metaService.updateTag({
			name: "description",
			content:
				"Track your assets, monitor net worth over time, set financial targets, and get multi-currency analytics — all in one place."
		});
		this.metaService.updateTag({
			property: "og:title",
			content: "Aurum"
		});
		this.metaService.updateTag({
			property: "og:url",
			content: "https://aurum-networth.com/"
		});

		this.authService.isAuthenticated$.pipe(take(1)).subscribe(isAuthenticated => {
			if (isAuthenticated) {
				this.router.navigate(["/dashboard"], { replaceUrl: true });
			}
		});
	}

	login() {
		this.authService.loginWithRedirect({ appState: { target: "/dashboard" } });
	}
}
