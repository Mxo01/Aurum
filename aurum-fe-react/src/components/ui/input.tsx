import type { ComponentPropsWithRef } from "react";
import { cn } from "@/lib/utils";

export function Input({ className, type, ...props }: ComponentPropsWithRef<"input">) {
	return (
		<input
			type={type}
			className={cn(
				"border-input bg-background placeholder:text-muted-foreground flex h-9 w-full rounded-md border px-3 py-1 text-sm transition-colors",
				"focus-visible:border-ring focus-visible:ring-ring/40 focus-visible:ring-2 focus-visible:outline-none",
				"disabled:cursor-not-allowed disabled:opacity-50",
				"aria-invalid:border-destructive aria-invalid:ring-destructive/30",
				className
			)}
			{...props}
		/>
	);
}
