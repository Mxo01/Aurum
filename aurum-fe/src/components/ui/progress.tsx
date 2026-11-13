import * as ProgressPrimitive from "@radix-ui/react-progress";
import type { ComponentProps } from "react";
import { cn } from "@/lib/utils";

interface ProgressProps extends ComponentProps<typeof ProgressPrimitive.Root> {
	value: number;
	label?: string;
}

export function Progress({ value, label, className, ...props }: ProgressProps) {
	const clamped = Math.min(100, Math.max(0, value));

	return (
		<ProgressPrimitive.Root
			value={clamped}
			aria-label={label ?? "Progress"}
			className={cn("bg-muted relative h-1.5 w-full overflow-hidden rounded-full", className)}
			{...props}
		>
			<ProgressPrimitive.Indicator
				className="bg-primary h-full w-full flex-1 transition-transform"
				style={{ transform: `translateX(-${100 - clamped}%)` }}
			/>
		</ProgressPrimitive.Root>
	);
}
