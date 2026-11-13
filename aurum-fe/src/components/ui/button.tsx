import { Slot } from "@radix-ui/react-slot";
import { cva, type VariantProps } from "class-variance-authority";
import type { ButtonHTMLAttributes } from "react";
import { cn } from "@/lib/utils";

export const buttonVariants = cva(
	"focus-visible:ring-ring focus-visible:ring-offset-background inline-flex shrink-0 items-center justify-center gap-2 rounded-md text-sm font-medium whitespace-nowrap transition-colors focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:outline-none disabled:pointer-events-none disabled:opacity-50 [&_svg]:pointer-events-none [&_svg]:size-4 [&_svg]:shrink-0",
	{
		variants: {
			variant: {
				default: "bg-primary text-primary-foreground hover:bg-primary/90",
				secondary: "bg-secondary text-secondary-foreground hover:bg-secondary/80",
				outline: "border-border bg-background hover:bg-accent hover:text-accent-foreground border",
				ghost: "hover:bg-accent hover:text-accent-foreground",
				destructive: "bg-destructive text-destructive-foreground hover:bg-destructive/90",
				link: "text-foreground underline-offset-4 hover:underline"
			},
			size: {
				default: "h-9 px-4 py-2",
				sm: "h-8 rounded-md px-3 text-xs",
				lg: "h-11 rounded-lg px-6",
				icon: "size-9",
				"icon-sm": "size-8"
			}
		},
		defaultVariants: { variant: "default", size: "default" }
	}
);

interface ButtonProps
	extends ButtonHTMLAttributes<HTMLButtonElement>, VariantProps<typeof buttonVariants> {
	asChild?: boolean;
}

export function Button({ className, variant, size, asChild, ...props }: ButtonProps) {
	const Component = asChild ? Slot : "button";
	return (
		<Component
			className={cn(buttonVariants({ variant, size }), className)}
			{...props}
		/>
	);
}
