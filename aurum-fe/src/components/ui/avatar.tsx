import * as AvatarPrimitive from "@radix-ui/react-avatar";
import type { ComponentProps } from "react";
import { cn } from "@/lib/utils";

interface AvatarProps extends ComponentProps<typeof AvatarPrimitive.Root> {
	src?: string | null;
	alt: string;
	fallback: string;
}

export function Avatar({ src, alt, fallback, className, ...props }: AvatarProps) {
	return (
		<AvatarPrimitive.Root
			className={cn("relative flex size-8 shrink-0 overflow-hidden rounded-full", className)}
			{...props}
		>
			{src ? (
				<AvatarPrimitive.Image
					src={src}
					alt={alt}
					className="size-full object-cover"
					referrerPolicy="no-referrer"
				/>
			) : null}
			<AvatarPrimitive.Fallback className="bg-muted text-muted-foreground flex size-full items-center justify-center text-[11px] font-semibold">
				{fallback}
			</AvatarPrimitive.Fallback>
		</AvatarPrimitive.Root>
	);
}
