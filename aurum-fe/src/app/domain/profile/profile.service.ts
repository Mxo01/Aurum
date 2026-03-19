import { MessageService } from "primeng/api";
import { HttpClient } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { environment } from "../../../environments/environment";
import { AuthService } from "@auth0/auth0-angular";
import { switchMap, tap } from "rxjs";
import { UserProfile } from "./model/user-profile.model";

@Injectable({
	providedIn: "root"
})
export class ProfileService {
	private authService = inject(AuthService);
	private readonly http = inject(HttpClient);
	private readonly messageService = inject(MessageService);

	private readonly userApiUrl = environment.apiUrl + "/users";

	getProfile() {
		return this.http.get<UserProfile>(this.userApiUrl);
	}

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

	updateCurrency(currency: string) {
		return this.http.put(`${this.userApiUrl}/currency`, { currency }).pipe(
			switchMap(() => this.getProfile()),
			tap({
				next: () => {
					this.messageService.add({
						severity: "success",
						summary: "Success",
						detail: "Currency updated successfully"
					});
				},
				error: () =>
					this.messageService.add({
						severity: "error",
						summary: "Error",
						detail: "Failed to update currency"
					})
			})
		);
	}

	updateLocale(locale: string) {
		return this.http.put(`${this.userApiUrl}/locale`, { locale }).pipe(
			switchMap(() => this.getProfile()),
			tap({
				next: () => {
					this.messageService.add({
						severity: "success",
						summary: "Success",
						detail: "Locale updated successfully"
					});
				},
				error: () =>
					this.messageService.add({
						severity: "error",
						summary: "Error",
						detail: "Failed to update locale"
					})
			})
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
