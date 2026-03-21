import {
	ChangeDetectionStrategy,
	Component,
	inject,
	OnInit,
	signal
} from "@angular/core";
import { McpService } from "./mcp.service";
import { ApiKeyMeta } from "./mcp.model";
import { DatePipe } from "@angular/common";
import { Button } from "primeng/button";
import { Dialog } from "primeng/dialog";
import { PrimeTemplate } from "primeng/api";
import { Tooltip } from "primeng/tooltip";
import { MessageService } from "primeng/api";
import { finalize } from "rxjs";

@Component({
	selector: "app-mcp-setup",
	standalone: true,
	imports: [Button, Dialog, PrimeTemplate, DatePipe, Tooltip],
	templateUrl: "./mcp-setup.component.html",
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class McpSetupComponent implements OnInit {
	private readonly mcpService = inject(McpService);
	private readonly messageService = inject(MessageService);

	protected readonly keyMeta = signal<ApiKeyMeta | null>(null);
	protected readonly generatedKey = signal<string | null>(null);
	protected readonly showDialog = signal(false);
	protected readonly isGenerating = signal(false);
	protected readonly isRevoking = signal(false);
	protected readonly sseUrl = this.mcpService.mcpSseUrl;

	ngOnInit() {
		this.mcpService.getKeyMeta().subscribe({
			next: meta => this.keyMeta.set(meta),
			error: () => this.keyMeta.set(null)
		});
	}

	generateKey() {
		this.isGenerating.set(true);
		this.mcpService
			.generateKey()
			.pipe(finalize(() => this.isGenerating.set(false)))
			.subscribe({
				next: res => {
					this.generatedKey.set(res.key);
					this.showDialog.set(true);
					this.mcpService.getKeyMeta().subscribe(meta => this.keyMeta.set(meta));
				},
				error: () =>
					this.messageService.add({
						severity: "error",
						summary: "Error",
						detail: "Failed to generate API key"
					})
			});
	}

	revokeKey() {
		this.isRevoking.set(true);
		this.mcpService
			.revokeKey()
			.pipe(finalize(() => this.isRevoking.set(false)))
			.subscribe({
				next: () => {
					this.keyMeta.set(null);
					this.messageService.add({
						severity: "success",
						summary: "Revoked",
						detail: "API key revoked successfully"
					});
				},
				error: () =>
					this.messageService.add({
						severity: "error",
						summary: "Error",
						detail: "Failed to revoke API key"
					})
			});
	}

	copyKey() {
		const key = this.generatedKey();
		if (key) {
			navigator.clipboard.writeText(key);
			this.messageService.add({ severity: "success", summary: "Copied", detail: "API key copied" });
		}
	}

	copyJson() {
		navigator.clipboard.writeText(this.configJson());
		this.messageService.add({ severity: "success", summary: "Copied", detail: "Config JSON copied" });
	}

	confirmSaved() {
		this.showDialog.set(false);
		this.generatedKey.set(null);
	}

	configJson(): string {
		const key = this.generatedKey();
		return JSON.stringify(
			{
				mcpServers: {
					aurum: {
						url: this.sseUrl,
						headers: {
							Authorization: `Bearer ${key ?? "<your-api-key>"}`
						}
					}
				}
			},
			null,
			2
		);
	}
}
