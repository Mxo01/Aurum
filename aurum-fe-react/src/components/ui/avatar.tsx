import { cn } from "@/lib/utils";

interface AvatarProps {
	src?: string | null;
	alt: string;
	fallback: string;
	className?: string;
}

export function Avatar({ src, alt, fallback, className }: AvatarProps) {
	return (
		<span
			className={cn(
				"bg-muted text-muted-foreground relative flex size-8 shrink-0 overflow-hidden rounded-full",
				className
			)}
		>
			{src ? (
				<img src={src} alt={alt} className="size-full object-cover" referrerPolicy="no-referrer" />
			) : (
				<span className="flex size-full items-center justify-center text-[11px] font-semibold">
					{fallback}
				</span>
			)}
		</span>
	);
}
