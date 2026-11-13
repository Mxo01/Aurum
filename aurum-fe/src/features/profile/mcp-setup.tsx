import { Terminal } from "lucide-react";
import { useState } from "react";
import {
	CodeBlock,
	CodeBlockActions,
	CodeBlockContent,
	CodeBlockCopyButton,
	CodeBlockHeader,
	CodeBlockTitle
} from "@/components/ui/code-block";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { env } from "@/lib/env";

type Platform = "desktop" | "mobile";

const PLATFORMS: { value: Platform; label: string }[] = [
	{ value: "desktop", label: "Desktop" },
	{ value: "mobile", label: "Mobile / Web" }
];

export function McpSetup() {
	const [platform, setPlatform] = useState<Platform>("desktop");

	const mcpUrl = `${env.apiUrl}/mcp`;
	const config =
		platform === "desktop"
			? JSON.stringify({ mcpServers: { aurum: { type: "http", url: mcpUrl } } }, null, 2)
			: mcpUrl;

	return (
		<div className="flex flex-col gap-3">
			<div className="flex flex-col gap-1">
				<span className="text-sm font-medium">Connect your MCP client</span>
				<span className="text-muted-foreground text-xs">
					Authentication is handled automatically via browser login.
				</span>
			</div>

			<Tabs
				value={platform}
				onValueChange={value => setPlatform(value as Platform)}
			>
				<TabsList aria-label="MCP platform">
					{PLATFORMS.map(item => (
						<TabsTrigger
							key={item.value}
							value={item.value}
							aria-controls={undefined}
						>
							{item.label}
						</TabsTrigger>
					))}
				</TabsList>
			</Tabs>

			<CodeBlock>
				<CodeBlockHeader>
					<CodeBlockTitle>
						<Terminal className="size-3.5" /> MCP config
					</CodeBlockTitle>
					<CodeBlockActions>
						<CodeBlockCopyButton value={config} />
					</CodeBlockActions>
				</CodeBlockHeader>
				<CodeBlockContent wrap>{config}</CodeBlockContent>
			</CodeBlock>

			<p className="text-muted-foreground text-xs italic">
				On first connection your MCP client will open a browser window for login.
			</p>
		</div>
	);
}
