import { HttpClient } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { environment } from "../../../../environments/environment";
import { ApiKeyMeta, GeneratedKey } from "./model/mcp.model";

@Injectable({
	providedIn: "root"
})
export class McpService {
	private readonly http = inject(HttpClient);

	private readonly mcpApiUrl = environment.apiUrl + "/mcp";
	readonly mcpSseUrl = environment.apiUrl.replace(/\/api$/, "") + "/sse";

	generateKey() {
		return this.http.post<GeneratedKey>(`${this.mcpApiUrl}/keys`, {});
	}

	getKeyMeta() {
		return this.http.get<ApiKeyMeta>(`${this.mcpApiUrl}/keys`);
	}

	revokeKey() {
		return this.http.delete(`${this.mcpApiUrl}/keys`);
	}
}
