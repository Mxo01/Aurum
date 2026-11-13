import * as CheckboxPrimitive from "@radix-ui/react-checkbox";
import { Check } from "lucide-react";
import type { ComponentProps } from "react";
import { cn } from "@/lib/utils";

export function Checkbox({ className, ...props }: ComponentProps<typeof CheckboxPrimitive.Root>) {
	return (
		<CheckboxPrimitive.Root
			className={cn(
				"border-input focus-visible:ring-ring data-[state=checked]:bg-primary data-[state=checked]:border-primary data-[state=checked]:text-primary-foreground peer size-4 shrink-0 rounded-[4px] border transition-colors focus-visible:ring-2 focus-visible:ring-offset-2 focus-visible:outline-none disabled:cursor-not-allowed disabled:opacity-50",
				className
			)}
			{...props}
		>
			<CheckboxPrimitive.Indicator className="flex items-center justify-center">
				<Check className="size-3" />
			</CheckboxPrimitive.Indicator>
		</CheckboxPrimitive.Root>
	);
}
