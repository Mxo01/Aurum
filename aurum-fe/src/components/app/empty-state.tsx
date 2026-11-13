import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

interface EmptyStateProps {
	icon: ReactNode;
	title: string;
	description?: string;
	action?: ReactNode;
	className?: string;
}

export function EmptyState({ icon, title, description, action, className }: EmptyStateProps) {
	return (
		<div
			className={cn(
				"border-border flex flex-col items-center justify-center gap-3 rounded-xl border border-dashed px-6 py-12 text-center",
				className
			)}
		>
			<span className="text-muted-foreground">{icon}</span>
			<div className="flex flex-col gap-1">
				<p className="text-sm font-medium">{title}</p>
				{description ? <p className="text-muted-foreground text-xs">{description}</p> : null}
			</div>
			{action}
		</div>
	);
}
