import type { ComponentProps } from "react";
import { Drawer as DrawerPrimitive } from "vaul";
import { cn } from "@/lib/utils";

export function Drawer({ ...props }: ComponentProps<typeof DrawerPrimitive.Root>) {
	return <DrawerPrimitive.Root {...props} />;
}

export const DrawerTrigger = DrawerPrimitive.Trigger;
export const DrawerClose = DrawerPrimitive.Close;
export const DrawerPortal = DrawerPrimitive.Portal;

export function DrawerOverlay({
	className,
	...props
}: ComponentProps<typeof DrawerPrimitive.Overlay>) {
	return (
		<DrawerPrimitive.Overlay
			className={cn("fixed inset-0 z-50 bg-black/50", className)}
			{...props}
		/>
	);
}

export function DrawerContent({
	className,
	children,
	...props
}: ComponentProps<typeof DrawerPrimitive.Content>) {
	return (
		<DrawerPortal>
			<DrawerOverlay />
			<DrawerPrimitive.Content
				className={cn(
					"bg-background fixed inset-x-0 bottom-0 z-50 mt-24 flex max-h-[85dvh] flex-col rounded-t-xl border-t",
					className
				)}
				{...props}
			>
				<div className="bg-muted mx-auto mt-4 h-1.5 w-[100px] shrink-0 rounded-full" />
				{children}
			</DrawerPrimitive.Content>
		</DrawerPortal>
	);
}

export function DrawerHeader({ className, ...props }: ComponentProps<"div">) {
	return (
		<div
			className={cn("flex flex-col gap-1.5 p-4", className)}
			{...props}
		/>
	);
}

export function DrawerFooter({ className, ...props }: ComponentProps<"div">) {
	return (
		<div
			className={cn("mt-auto flex flex-col gap-2 p-4", className)}
			{...props}
		/>
	);
}

export function DrawerTitle({ className, ...props }: ComponentProps<typeof DrawerPrimitive.Title>) {
	return (
		<DrawerPrimitive.Title
			className={cn("text-foreground text-lg font-semibold", className)}
			{...props}
		/>
	);
}

export function DrawerDescription({
	className,
	...props
}: ComponentProps<typeof DrawerPrimitive.Description>) {
	return (
		<DrawerPrimitive.Description
			className={cn("text-muted-foreground text-sm", className)}
			{...props}
		/>
	);
}
