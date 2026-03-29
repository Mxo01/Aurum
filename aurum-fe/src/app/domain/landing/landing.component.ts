import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit } from "@angular/core";
import { Router, RouterLink } from "@angular/router";
import { AuthService } from "@auth0/auth0-angular";
import { Meta, Title } from "@angular/platform-browser";
import { Button } from "primeng/button";
import { ThemeService } from "../../shared/services/theme/theme.service";
import { filter, switchMap, take } from "rxjs";
import { takeUntilDestroyed, toSignal } from "@angular/core/rxjs-interop";

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
	private readonly destroyRef = inject(DestroyRef);
	readonly themeService = inject(ThemeService);

	readonly isLoading = toSignal(this.authService.isLoading$, { initialValue: true });

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

		this.authService.isLoading$
			.pipe(
				filter(isLoading => !isLoading),
				take(1),
				switchMap(() => this.authService.isAuthenticated$.pipe(take(1))),
				takeUntilDestroyed(this.destroyRef)
			)
			.subscribe({
				next: isAuthenticated => {
					if (isAuthenticated) this.router.navigate(["/dashboard"], { replaceUrl: true });
				}
			});
	}

	login() {
		this.authService.loginWithRedirect({ appState: { target: "/dashboard" } });
	}
}
