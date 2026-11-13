import * as TabsPrimitive from "@radix-ui/react-tabs";
import type { ComponentProps } from "react";
import { cn } from "@/lib/utils";

export const Tabs = TabsPrimitive.Root;

export function TabsList({ className, ...props }: ComponentProps<typeof TabsPrimitive.List>) {
	return (
		<TabsPrimitive.List
			className={cn(
				"bg-muted text-muted-foreground inline-flex h-9 items-center justify-center gap-1 rounded-lg p-1",
				className
			)}
			{...props}
		/>
	);
}

export function TabsTrigger({ className, ...props }: ComponentProps<typeof TabsPrimitive.Trigger>) {
	return (
		<TabsPrimitive.Trigger
			className={cn(
				"focus-visible:ring-ring data-[state=active]:bg-card data-[state=active]:text-foreground inline-flex items-center justify-center gap-1.5 rounded-md px-3 py-1 text-sm font-medium whitespace-nowrap transition-colors focus-visible:ring-2 focus-visible:outline-none disabled:pointer-events-none disabled:opacity-50 data-[state=active]:shadow-sm",
				className
			)}
			{...props}
		/>
	);
}

export function TabsContent({ className, ...props }: ComponentProps<typeof TabsPrimitive.Content>) {
	return (
		<TabsPrimitive.Content
			className={cn("focus-visible:ring-ring mt-4 focus-visible:ring-2 focus-visible:outline-none", className)}
			{...props}
		/>
	);
}
