import { Command as CommandPrimitive } from "cmdk";
import { Search } from "lucide-react";
import type { ComponentProps, ReactNode } from "react";
import { Dialog, DialogContent, DialogTitle } from "@/components/ui/dialog";
import { cn } from "@/lib/utils";

export function Command({ className, ...props }: ComponentProps<typeof CommandPrimitive>) {
	return (
		<CommandPrimitive
			className={cn("bg-popover text-popover-foreground flex h-full w-full flex-col", className)}
			{...props}
		/>
	);
}

interface CommandDialogProps {
	open: boolean;
	onOpenChange: (open: boolean) => void;
	children: ReactNode;
}

export function CommandDialog({ open, onOpenChange, children }: CommandDialogProps) {
	return (
		<Dialog open={open} onOpenChange={onOpenChange}>
			<DialogContent className="overflow-hidden p-0 shadow-2xl sm:max-w-lg">
				<DialogTitle className="sr-only">Command palette</DialogTitle>
				<Command className="[&_[cmdk-group-heading]]:text-muted-foreground [&_[cmdk-group-heading]]:px-2 [&_[cmdk-group-heading]]:py-1.5 [&_[cmdk-group-heading]]:text-[11px] [&_[cmdk-group-heading]]:font-medium">
					{children}
				</Command>
			</DialogContent>
		</Dialog>
	);
}

export function CommandInput({ className, ...props }: ComponentProps<typeof CommandPrimitive.Input>) {
	return (
		<div className="border-border flex items-center gap-2 border-b px-3">
			<Search className="size-4 shrink-0 opacity-50" />
			<CommandPrimitive.Input
				className={cn(
					"placeholder:text-muted-foreground flex h-11 w-full bg-transparent py-3 text-sm outline-none disabled:cursor-not-allowed disabled:opacity-50",
					className
				)}
				{...props}
			/>
		</div>
	);
}

export function CommandList({ className, ...props }: ComponentProps<typeof CommandPrimitive.List>) {
	return (
		<CommandPrimitive.List
			className={cn("max-h-80 overflow-y-auto overflow-x-hidden p-1", className)}
			{...props}
		/>
	);
}

export function CommandEmpty(props: ComponentProps<typeof CommandPrimitive.Empty>) {
	return (
		<CommandPrimitive.Empty
			className="text-muted-foreground py-6 text-center text-sm"
			{...props}
		/>
	);
}

export function CommandGroup({ className, ...props }: ComponentProps<typeof CommandPrimitive.Group>) {
	return <CommandPrimitive.Group className={cn("text-foreground", className)} {...props} />;
}

export function CommandItem({ className, ...props }: ComponentProps<typeof CommandPrimitive.Item>) {
	return (
		<CommandPrimitive.Item
			className={cn(
				"data-[selected=true]:bg-accent data-[selected=true]:text-accent-foreground relative flex cursor-pointer items-center gap-2 rounded-md px-2 py-2 text-sm outline-none select-none data-[disabled=true]:pointer-events-none data-[disabled=true]:opacity-50",
				className
			)}
			{...props}
		/>
	);
}

export function CommandSeparator() {
	return <CommandPrimitive.Separator className="bg-border -mx-1 my-1 h-px" />;
}
