import { Injectable, inject } from "@angular/core";
import { LoginResponse, OidcSecurityService } from "angular-auth-oidc-client";
import { Observable, map } from "rxjs";

@Injectable({
	providedIn: "root"
})
export class AuthService {
	private readonly oidcSecurityService = inject(OidcSecurityService);

	get isAuthenticated$(): Observable<boolean> {
		return this.oidcSecurityService.isAuthenticated$.pipe(map(result => result.isAuthenticated));
	}

	login() {
		this.oidcSecurityService.authorize();
	}

	logout() {
		this.oidcSecurityService.logoff().subscribe();
	}

	getAccessToken(): Observable<string> {
		return this.oidcSecurityService.getAccessToken();
	}

	checkAuth(): Observable<LoginResponse> {
		return this.oidcSecurityService.checkAuth();
	}
}
