import type { ComponentPropsWithRef } from "react";
import { cn } from "@/lib/utils";

export function Input({ className, type, onFocus, ...props }: ComponentPropsWithRef<"input">) {
	return (
		<input
			type={type}
			onFocus={event => {
				if (type === "number") event.currentTarget.select();
				onFocus?.(event);
			}}
			className={cn(
				"border-input placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-ring/50 aria-invalid:border-destructive aria-invalid:ring-destructive/20 dark:aria-invalid:border-destructive/50 dark:aria-invalid:ring-destructive/40 h-8 w-full min-w-0 rounded-lg border bg-transparent px-2.5 py-1 text-base transition-colors outline-none file:inline-flex file:h-6 file:border-0 file:bg-transparent file:text-sm file:font-medium file:text-foreground focus-visible:ring-3 disabled:pointer-events-none disabled:cursor-not-allowed disabled:bg-input/50 disabled:opacity-50 md:text-sm dark:bg-input/30 dark:disabled:bg-input/80",
				className
			)}
			{...props}
		/>
	);
}
