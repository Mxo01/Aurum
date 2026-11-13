import * as DropdownMenuPrimitive from "@radix-ui/react-dropdown-menu";
import { Check } from "lucide-react";
import type { ComponentProps } from "react";
import { cn } from "@/lib/utils";

export const DropdownMenu = DropdownMenuPrimitive.Root;
export const DropdownMenuTrigger = DropdownMenuPrimitive.Trigger;
export const DropdownMenuGroup = DropdownMenuPrimitive.Group;

export function DropdownMenuContent({
	className,
	sideOffset = 6,
	...props
}: ComponentProps<typeof DropdownMenuPrimitive.Content>) {
	return (
		<DropdownMenuPrimitive.Portal>
			<DropdownMenuPrimitive.Content
				sideOffset={sideOffset}
				className={cn(
					"bg-popover text-popover-foreground border-border z-50 min-w-44 overflow-hidden rounded-lg border p-1 shadow-lg",
					className
				)}
				{...props}
			/>
		</DropdownMenuPrimitive.Portal>
	);
}

export function DropdownMenuItem({
	className,
	inset,
	...props
}: ComponentProps<typeof DropdownMenuPrimitive.Item> & { inset?: boolean }) {
	return (
		<DropdownMenuPrimitive.Item
			className={cn(
				"focus:bg-accent focus:text-accent-foreground relative flex cursor-pointer items-center gap-2 rounded-md px-2 py-1.5 text-sm outline-none select-none data-[disabled]:pointer-events-none data-[disabled]:opacity-50",
				inset && "pl-8",
				className
			)}
			{...props}
		/>
	);
}

export function DropdownMenuCheckboxItem({
	className,
	children,
	checked,
	...props
}: ComponentProps<typeof DropdownMenuPrimitive.CheckboxItem>) {
	return (
		<DropdownMenuPrimitive.CheckboxItem
			checked={checked}
			className={cn(
				"focus:bg-accent focus:text-accent-foreground relative flex cursor-pointer items-center gap-2 rounded-md py-1.5 pr-2 pl-8 text-sm outline-none select-none",
				className
			)}
			{...props}
		>
			<span className="absolute left-2 flex size-4 items-center justify-center">
				<DropdownMenuPrimitive.ItemIndicator>
					<Check className="size-3.5" />
				</DropdownMenuPrimitive.ItemIndicator>
			</span>
			{children}
		</DropdownMenuPrimitive.CheckboxItem>
	);
}

export function DropdownMenuLabel({
	className,
	...props
}: ComponentProps<typeof DropdownMenuPrimitive.Label>) {
	return (
		<DropdownMenuPrimitive.Label
			className={cn("text-muted-foreground px-2 py-1.5 text-[11px] font-medium", className)}
			{...props}
		/>
	);
}

export function DropdownMenuSeparator({
	className,
	...props
}: ComponentProps<typeof DropdownMenuPrimitive.Separator>) {
	return (
		<DropdownMenuPrimitive.Separator
			className={cn("bg-border -mx-1 my-1 h-px", className)}
			{...props}
		/>
	);
}
