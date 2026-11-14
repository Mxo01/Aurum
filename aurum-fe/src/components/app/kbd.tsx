import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

export function Kbd({ children, className }: { children: ReactNode; className?: string }) {
	return (
		<kbd
			className={cn(
				"bg-muted text-muted-foreground border-border inline-flex h-5 items-center rounded border px-1.5 font-sans text-[10px] font-medium",
				className
			)}
		>
			{children}
		</kbd>
	);
}
