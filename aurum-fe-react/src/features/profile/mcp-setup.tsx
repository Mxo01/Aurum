import { Check, Copy } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { env } from "@/lib/env";
import { cn } from "@/lib/utils";

type Platform = "desktop" | "mobile";

const PLATFORMS: { value: Platform; label: string }[] = [
	{ value: "desktop", label: "Desktop" },
	{ value: "mobile", label: "Mobile / Web" }
];

export function McpSetup() {
	const [platform, setPlatform] = useState<Platform>("desktop");
	const [copied, setCopied] = useState(false);

	const mcpUrl = `${env.apiUrl}/mcp`;
	const config =
		platform === "desktop"
			? JSON.stringify({ mcpServers: { aurum: { type: "http", url: mcpUrl } } }, null, 2)
			: mcpUrl;

	const copy = async () => {
		try {
			await navigator.clipboard.writeText(config);
			setCopied(true);
			toast.success("Config copied");
			window.setTimeout(() => setCopied(false), 1500);
		} catch {
			toast.error("Could not copy the config");
		}
	};

	return (
		<div className="flex flex-col gap-3">
			<div className="flex flex-col gap-1">
				<span className="text-sm font-medium">Connect your MCP client</span>
				<span className="text-muted-foreground text-xs">
					Authentication is handled automatically via browser login.
				</span>
			</div>

			<div className="bg-muted flex w-fit items-center gap-0.5 rounded-md p-0.5">
				{PLATFORMS.map(item => (
					<button
						key={item.value}
						type="button"
						onClick={() => setPlatform(item.value)}
						aria-pressed={platform === item.value}
						className={cn(
							"focus-ring rounded px-2.5 py-1 text-[11px] font-medium transition-colors",
							platform === item.value
								? "bg-card text-foreground shadow-sm"
								: "text-muted-foreground hover:text-foreground"
						)}
					>
						{item.label}
					</button>
				))}
			</div>

			<div className="relative">
				<pre className="border-border bg-muted/40 overflow-x-auto rounded-lg border p-3 text-xs leading-relaxed">
					<code>{config}</code>
				</pre>
				<Button
					variant="ghost"
					size="icon-sm"
					className="absolute top-2 right-2"
					onClick={() => void copy()}
					aria-label="Copy MCP config"
				>
					{copied ? <Check className="size-4" /> : <Copy className="size-4" />}
				</Button>
			</div>

			<p className="text-muted-foreground text-xs italic">
				On first connection your MCP client will open a browser window for login.
			</p>
		</div>
	);
}
