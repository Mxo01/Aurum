import { MessageService } from "primeng/api";
import { HttpClient } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { environment } from "../../../environments/environment";
import { AuthService } from "@auth0/auth0-angular";
import { switchMap, tap } from "rxjs";

@Injectable({
	providedIn: "root"
})
export class ProfileService {
	private authService = inject(AuthService);
	private readonly http = inject(HttpClient);
	private readonly messageService = inject(MessageService);

	private readonly userApiUrl = environment.apiUrl + "/users";

	deleteProfile() {
		return this.http.delete(this.userApiUrl).pipe(
			tap({
				next: () => {
					this.messageService.add({
						severity: "success",
						summary: "Success",
						detail: "Profile deleted successfully"
					});
				},
				error: () =>
					this.messageService.add({
						severity: "error",
						summary: "Error",
						detail: "Failed to delete profile"
					})
			}),
			switchMap(() => this.authService.logout())
		);
	}

	updateName(name: string) {
		return this.http.put(this.userApiUrl, { name }).pipe(
			switchMap(() => this.refreshAccessToken()),
			tap({
				next: () => {
					this.messageService.add({
						severity: "success",
						summary: "Success",
						detail: "Profile updated successfully"
					});
				},
				error: () =>
					this.messageService.add({
						severity: "error",
						summary: "Error",
						detail: "Failed to update profile"
					})
			})
		);
	}

	private refreshAccessToken() {
		return this.authService.getAccessTokenSilently({ cacheMode: "off" });
	}
}
