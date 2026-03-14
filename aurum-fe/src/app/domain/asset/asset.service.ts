import { HttpClient } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { environment } from "../../../environments/environment";
import { Asset } from "./model/asset.model";

@Injectable({
	providedIn: "root"
})
export class AssetService {
	private http = inject(HttpClient);

	private readonly assetsUrl = `${environment.apiUrl}/assets`;

	getAssets() {
		return this.http.get<Asset[]>(this.assetsUrl);
	}
}
