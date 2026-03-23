import { MessageService } from "primeng/api";
import { HttpClient } from "@angular/common/http";
import { inject, Injectable, signal } from "@angular/core";
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

	private readonly _profile = signal<UserProfile | undefined>(undefined);
	readonly profile = this._profile.asReadonly();

	getProfile() {
		return this.http.get<UserProfile>(this.userApiUrl).pipe(tap(p => this._profile.set(p)));
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
			switchMap(() =>
				this.authService.logout({ logoutParams: { returnTo: window.location.origin } })
			)
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

	updatePicture(file: File) {
		const formData = new FormData();
		formData.append("file", file);

		return this.http.post<void>(`${this.userApiUrl}/picture`, formData).pipe(
			switchMap(() => this.getProfile()),
			tap({
				next: () => {
					this.messageService.add({
						severity: "success",
						summary: "Success",
						detail: "Profile picture updated"
					});
				},
				error: () =>
					this.messageService.add({
						severity: "error",
						summary: "Error",
						detail: "Failed to update profile picture"
					})
			})
		);
	}

	deletePicture() {
		return this.http.delete<void>(`${this.userApiUrl}/picture`).pipe(
			switchMap(() => this.getProfile()),
			tap({
				next: () => {
					this.messageService.add({
						severity: "success",
						summary: "Success",
						detail: "Profile picture removed"
					});
				},
				error: () =>
					this.messageService.add({
						severity: "error",
						summary: "Error",
						detail: "Failed to remove profile picture"
					})
			})
		);
	}

	exportData() {
		return this.http.get(`${this.userApiUrl}/export`).pipe(
			tap({
				next: data => {
					const blob = new Blob([JSON.stringify(data, null, 2)], {
						type: "application/json"
					});
					const url = URL.createObjectURL(blob);
					const a = document.createElement("a");
					a.href = url;
					a.download = `aurum-export-${new Date().toISOString().slice(0, 10)}.json`;
					a.click();
					URL.revokeObjectURL(url);
					this.messageService.add({
						severity: "success",
						summary: "Export ready",
						detail: "Your data has been downloaded"
					});
				},
				error: () =>
					this.messageService.add({
						severity: "error",
						summary: "Error",
						detail: "Failed to export data"
					})
			})
		);
	}

	private refreshAccessToken() {
		return this.authService.getAccessTokenSilently({ cacheMode: "off" });
	}
}
