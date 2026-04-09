import { ChangeDetectionStrategy, Component, signal, inject } from "@angular/core";
import { Button } from "primeng/button";
import { Tooltip } from "primeng/tooltip";
import { MessageService, SelectItem } from "primeng/api";
import { Highlight } from "ngx-highlightjs";
import { SelectButton } from "primeng/selectbutton";
import { FormsModule } from "@angular/forms";
import { environment } from "../../../../environments/environment";

@Component({
	selector: "app-mcp-setup",
	standalone: true,
	imports: [Button, Tooltip, Highlight, SelectButton, FormsModule],
	templateUrl: "./mcp-setup.component.html",
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class McpSetupComponent {
	private readonly messageService = inject(MessageService);

	readonly mcpSseUrl = environment.apiUrl + "/sse";
	readonly isMcpDesktop = signal(true);
	readonly mcpPlatformOptions = signal<SelectItem<boolean>[]>([
		{ label: "Desktop", value: true },
		{ label: "Mobile/Web", value: false }
	]);

	get mcpConfig(): string {
		return this.isMcpDesktop()
			? JSON.stringify({ mcpServers: { aurum: { type: "sse", url: this.mcpSseUrl } } }, null, 2)
			: this.mcpSseUrl;
	}

	copyConfig() {
		navigator.clipboard
			.writeText(this.mcpConfig)
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
}
