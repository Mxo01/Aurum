import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

interface PageHeaderProps {
	title: string;
	description?: ReactNode;
	actions?: ReactNode;
	className?: string;
}

export function PageHeader({ title, description, actions, className }: PageHeaderProps) {
	return (
		<header className={cn("flex flex-wrap items-end justify-between gap-4", className)}>
			<div className="flex flex-col gap-1">
				<h1 className="text-xl font-semibold tracking-tight">{title}</h1>
				{description ? (
					<p className="text-muted-foreground text-sm">{description}</p>
				) : null}
			</div>
			{actions ? <div className="flex flex-wrap items-center gap-2">{actions}</div> : null}
		</header>
	);
}
