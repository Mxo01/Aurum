import { cva, type VariantProps } from "class-variance-authority";
import type { HTMLAttributes } from "react";
import { cn } from "@/lib/utils";

const badgeVariants = cva(
	"inline-flex items-center gap-1 rounded-md border px-2 py-0.5 text-[11px] font-medium whitespace-nowrap",
	{
		variants: {
			variant: {
				default: "border-transparent bg-secondary text-secondary-foreground",
				outline: "border-border text-muted-foreground",
				success: "border-transparent bg-success/10 text-success",
				danger: "border-transparent bg-danger/10 text-danger",
				gold: "border-primary/30 bg-primary/10 text-foreground"
			}
		},
		defaultVariants: { variant: "default" }
	}
);

interface BadgeProps extends HTMLAttributes<HTMLSpanElement>, VariantProps<typeof badgeVariants> {}

export function Badge({ className, variant, ...props }: BadgeProps) {
	return <span className={cn(badgeVariants({ variant }), className)} {...props} />;
}
