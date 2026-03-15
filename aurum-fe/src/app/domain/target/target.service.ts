import { HttpClient } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { environment } from "../../../environments/environment";
import { Target } from "./model/target.model";
import { Observable, switchMap, tap } from "rxjs";
import { MessageService } from "primeng/api";

@Injectable({
	providedIn: "root"
})
export class TargetService {
	private readonly messageService = inject(MessageService);
	private readonly http = inject(HttpClient);

	private readonly apiUrl = `${environment.apiUrl}/targets`;

	getTargets(): Observable<Target[]> {
		return this.http.get<Target[]>(this.apiUrl);
	}

	saveTarget(target: Target): Observable<Target[]> {
		return target.id
			? this.http.put<Target[]>(`${this.apiUrl}/${target.id}`, target).pipe(
					switchMap(() => this.getTargets()),
					tap({
						next: () =>
							this.messageService.add({
								severity: "success",
								summary: "Success",
								detail: "Target updated successfully"
							}),
						error: () =>
							this.messageService.add({
								severity: "error",
								summary: "Error",
								detail: "Failed to update the target"
							})
					})
				)
			: this.http.post<Target[]>(this.apiUrl, target).pipe(
					switchMap(() => this.getTargets()),
					tap({
						next: () =>
							this.messageService.add({
								severity: "success",
								summary: "Success",
								detail: "Target created successfully"
							}),
						error: () =>
							this.messageService.add({
								severity: "error",
								summary: "Error",
								detail: "Failed to create the target"
							})
					})
				);
	}

	deleteTarget(id: string): Observable<Target[]> {
		return this.http.delete<Target[]>(`${this.apiUrl}/${id}`).pipe(
			switchMap(() => this.getTargets()),
			tap({
				next: () =>
					this.messageService.add({
						severity: "success",
						summary: "Success",
						detail: "Target deleted successfully"
					}),
				error: () =>
					this.messageService.add({
						severity: "error",
						summary: "Error",
						detail: "Failed to delete the target"
					})
			})
		);
	}
}
