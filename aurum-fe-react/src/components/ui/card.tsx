import type { HTMLAttributes } from "react";
import { cn } from "@/lib/utils";

export function Card({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
	return (
		<div
			className={cn("bg-card text-card-foreground border-border rounded-xl border", className)}
			{...props}
		/>
	);
}

export function CardHeader({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
	return <div className={cn("flex flex-col gap-1 p-5", className)} {...props} />;
}

export function CardTitle({ className, children, ...props }: HTMLAttributes<HTMLHeadingElement>) {
	return (
		<h3 className={cn("text-sm leading-none font-semibold", className)} {...props}>
			{children}
		</h3>
	);
}

export function CardDescription({ className, ...props }: HTMLAttributes<HTMLParagraphElement>) {
	return <p className={cn("text-muted-foreground text-xs", className)} {...props} />;
}

export function CardContent({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
	return <div className={cn("p-5 pt-0", className)} {...props} />;
}

export function CardFooter({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
	return <div className={cn("flex items-center p-5 pt-0", className)} {...props} />;
}
