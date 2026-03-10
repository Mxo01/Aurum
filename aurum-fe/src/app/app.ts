import { Component, OnInit, inject } from "@angular/core";
import { AuthService } from "./core/auth/auth.service";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";

@Component({
	selector: "app-root",
	imports: [],
	templateUrl: "./app.html",
	styleUrl: "./app.scss"
})
export class App implements OnInit {
	private readonly authService = inject(AuthService);

	isAuthenticated$ = this.authService.isAuthenticated$;

	ngOnInit() {
		this.authService
			.checkAuth()
			.pipe(takeUntilDestroyed())
			.subscribe(({ isAuthenticated, userData, accessToken }) => {
				console.log("App authenticated", isAuthenticated);
				if (userData) console.log("User data", userData);
				if (accessToken) console.log("Token ready");
			});
	}

	login() {
		this.authService.login();
	}

	logout() {
		this.authService.logout();
	}
}
