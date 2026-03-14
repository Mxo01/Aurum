import { HttpClient } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { environment } from "../../../environments/environment";
import { Snapshot } from "./model/snapshot.model";
import { switchMap, tap } from "rxjs";
import { MessageService } from "primeng/api";

@Injectable({
	providedIn: "root"
})
export class SnapshotService {
	private readonly messageService = inject(MessageService);
	private readonly http = inject(HttpClient);

	private readonly snapshotsUrl = `${environment.apiUrl}/snapshots`;

	getAllSnapshots() {
		return this.http.get<Snapshot[]>(this.snapshotsUrl);
	}

	saveSnapshot(snapshot: Snapshot) {
		return this.http.post<Snapshot>(this.snapshotsUrl, snapshot).pipe(
			switchMap(() => this.getAllSnapshots()),
			tap({
				next: () => {
					this.messageService.add({
						severity: "success",
						summary: "Success",
						detail: "Snapshot saved successfully"
					});
				},
				error: () => {
					this.messageService.add({
						severity: "error",
						summary: "Error",
						detail: "Failed to save snapshot"
					});
				}
			})
		);
	}

	deleteSnapshotsBulk(assetId: string, ids: string[]) {
		return this.http.delete(`${this.snapshotsUrl}/bulk?assetId=${assetId}`, { body: ids }).pipe(
			switchMap(() => this.getAllSnapshots()),
			tap({
				next: () => {
					this.messageService.add({
						severity: "success",
						summary: "Success",
						detail: "Snapshots deleted successfully"
					});
				},
				error: () => {
					this.messageService.add({
						severity: "error",
						summary: "Error",
						detail: "Failed to delete snapshots"
					});
				}
			})
		);
	}
}
