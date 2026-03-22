import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from "@angular/core";
import { McpService } from "./mcp.service";
import { ApiKeyMeta } from "./model/mcp.model";
import { DatePipe } from "@angular/common";
import { Button } from "primeng/button";
import { Dialog } from "primeng/dialog";
import { Tooltip } from "primeng/tooltip";
import { MessageService } from "primeng/api";
import { finalize, switchMap } from "rxjs";
import { Message } from "primeng/message";
import { Highlight } from "ngx-highlightjs";

@Component({
	selector: "app-mcp-setup",
	standalone: true,
	imports: [Button, Dialog, DatePipe, Tooltip, Message, Highlight],
	templateUrl: "./mcp-setup.component.html",
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class McpSetupComponent implements OnInit {
	private readonly mcpService = inject(McpService);
	private readonly messageService = inject(MessageService);

	protected readonly keyMeta = signal<ApiKeyMeta | null>(null);
	protected readonly generatedKey = signal<string | null>(null);
	protected readonly isApiKeyInstructionsDialogVisible = signal(false);
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
			.pipe(
				switchMap(generatedKey => {
					this.generatedKey.set(generatedKey.key);
					this.isApiKeyInstructionsDialogVisible.set(true);

					return this.mcpService.getKeyMeta();
				}),
				finalize(() => this.isGenerating.set(false))
			)
			.subscribe({
				next: meta => this.keyMeta.set(meta),
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
			navigator.clipboard
				.writeText(key)
				.then(() =>
					this.messageService.add({
						severity: "success",
						summary: "Copied",
						detail: "API key copied"
					})
				)
				.catch(() =>
					this.messageService.add({
						severity: "error",
						summary: "Error",
						detail: "Failed to copy API key"
					})
				);
		}
	}

	copyJson() {
		navigator.clipboard
			.writeText(this.configJson())
			.then(() =>
				this.messageService.add({
					severity: "success",
					summary: "Copied",
					detail: "Config JSON copied"
				})
			)
			.catch(() =>
				this.messageService.add({
					severity: "error",
					summary: "Error",
					detail: "Failed to copy config JSON"
				})
			);
	}

	confirmSaved() {
		this.isApiKeyInstructionsDialogVisible.set(false);
		this.generatedKey.set(null);
	}

	configJson(): string {
		const key = this.generatedKey() ?? "<your-api-key>";
		return JSON.stringify(
			{ mcpServers: { aurum: { type: "sse", url: `${this.sseUrl}?key=${key}` } } },
			null,
			2
		);
	}
}
