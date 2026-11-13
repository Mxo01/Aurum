import * as DialogPrimitive from "@radix-ui/react-dialog";
import { X } from "lucide-react";
import type { ComponentProps } from "react";
import { cn } from "@/lib/utils";

export const Sheet = DialogPrimitive.Root;
export const SheetTrigger = DialogPrimitive.Trigger;
export const SheetClose = DialogPrimitive.Close;

export function SheetContent({
	className,
	children,
	side = "right",
	...props
}: ComponentProps<typeof DialogPrimitive.Content> & { side?: "right" | "left" }) {
	return (
		<DialogPrimitive.Portal>
			<DialogPrimitive.Overlay className="fixed inset-0 z-50 bg-black/50" />
			<DialogPrimitive.Content
				className={cn(
					"bg-card border-border fixed inset-y-0 z-50 flex w-full flex-col gap-4 border-l p-5 shadow-xl sm:max-w-md",
					side === "right" ? "right-0" : "left-0 border-r border-l-0",
					className
				)}
				{...props}
			>
				{children}
				<DialogPrimitive.Close className="ring-offset-background focus-visible:ring-ring absolute top-4 right-4 rounded-md opacity-60 transition-opacity hover:opacity-100 focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:outline-none">
					<X className="size-4" />
					<span className="sr-only">Close</span>
				</DialogPrimitive.Close>
			</DialogPrimitive.Content>
		</DialogPrimitive.Portal>
	);
}

export function SheetHeader({ className, ...props }: ComponentProps<"div">) {
	return (
		<div
			className={cn("flex flex-col gap-1 pr-8", className)}
			{...props}
		/>
	);
}

export function SheetTitle({ className, ...props }: ComponentProps<typeof DialogPrimitive.Title>) {
	return (
		<DialogPrimitive.Title
			className={cn("text-lg font-semibold", className)}
			{...props}
		/>
	);
}

export function SheetDescription({
	className,
	...props
}: ComponentProps<typeof DialogPrimitive.Description>) {
	return (
		<DialogPrimitive.Description
			className={cn("text-muted-foreground text-sm", className)}
			{...props}
		/>
	);
}

export function SheetFooter({ className, ...props }: ComponentProps<"div">) {
	return (
		<div
			className={cn("mt-auto flex flex-col-reverse gap-2 pt-2 sm:flex-row", className)}
			{...props}
		/>
	);
}
