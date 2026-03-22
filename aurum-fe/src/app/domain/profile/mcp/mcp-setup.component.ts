import {
	ChangeDetectionStrategy,
	Component,
	computed,
	inject,
	OnInit,
	signal
} from "@angular/core";
import { McpService } from "./mcp.service";
import { ApiKeyMeta } from "./model/mcp.model";
import { DatePipe } from "@angular/common";
import { Button } from "primeng/button";
import { Dialog } from "primeng/dialog";
import { Tooltip } from "primeng/tooltip";
import { finalize, switchMap } from "rxjs";
import { Message } from "primeng/message";
import { Highlight } from "ngx-highlightjs";
import { SelectButton } from "primeng/selectbutton";
import { SelectItem, MessageService } from "primeng/api";
import { FormsModule } from "@angular/forms";

@Component({
	selector: "app-mcp-setup",
	standalone: true,
	imports: [Button, Dialog, DatePipe, Tooltip, Message, Highlight, SelectButton, FormsModule],
	templateUrl: "./mcp-setup.component.html",
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class McpSetupComponent implements OnInit {
	private readonly mcpService = inject(McpService);
	private readonly messageService = inject(MessageService);

	protected readonly sseUrl = this.mcpService.mcpSseUrl;
	protected readonly isMcpDesktop = signal(true);
	protected readonly mcpPlatformOptions = signal<SelectItem<boolean>[]>([
		{ label: "Desktop", value: true },
		{ label: "Mobile/Web", value: false }
	]);
	protected readonly keyMeta = signal<ApiKeyMeta | null>(null);
	protected readonly generatedKey = signal<string | null>(null);
	protected readonly isApiKeyInstructionsDialogVisible = signal(false);
	protected readonly isGenerating = signal(false);
	protected readonly isRevoking = signal(false);
	protected readonly mcpConfig = computed(() => {
		const key = this.generatedKey() ?? "<your-api-key>";
		const sseUrl = `${this.sseUrl}?key=${key}`;
		return this.isMcpDesktop()
			? JSON.stringify({ mcpServers: { aurum: { type: "sse", url: sseUrl } } }, null, 2)
			: sseUrl;
	});

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

	copyConfig() {
		navigator.clipboard
			.writeText(this.mcpConfig())
			.then(() =>
				this.messageService.add({
					severity: "success",
					summary: "Copied",
					detail: "Config copied"
				})
			)
			.catch(() =>
				this.messageService.add({
					severity: "error",
					summary: "Error",
					detail: "Failed to copy config"
				})
			);
	}

	confirmSaved() {
		this.isApiKeyInstructionsDialogVisible.set(false);
		this.generatedKey.set(null);
	}
}
