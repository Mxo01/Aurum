import { Loader2 } from "lucide-react";

export function Splash() {
	return (
		<div className="bg-background flex min-h-dvh flex-col items-center justify-center gap-4">
			<img
				src="/favicon.svg"
				alt=""
				className="size-9"
			/>
			<div className="text-muted-foreground flex items-center gap-2 text-sm">
				<Loader2
					className="size-4 animate-spin"
					aria-hidden
				/>
				Loading Aurum
			</div>
		</div>
	);
}
