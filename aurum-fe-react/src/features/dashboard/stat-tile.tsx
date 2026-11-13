import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

interface StatTileProps {
	label: string;
	value: ReactNode;
	hint?: ReactNode;
	right?: ReactNode;
	className?: string;
}

export function StatTile({ label, value, hint, right, className }: StatTileProps) {
	return (
		<div
			className={cn(
				"bg-card border-border flex min-h-[104px] flex-col justify-between gap-2 rounded-xl border p-4",
				className
			)}
		>
			<div className="flex items-start justify-between gap-2">
				<span className="text-muted-foreground text-[11px] font-medium">{label}</span>
				{right}
			</div>
			<div className="flex flex-col gap-0.5">
				<div className="num text-xl font-semibold tracking-tight">{value}</div>
				{hint ? <span className="text-muted-foreground text-[11px]">{hint}</span> : null}
			</div>
		</div>
	);
}

export function Sparkline({ values, className }: { values: number[]; className?: string }) {
	if (values.length < 2) return null;

	const min = Math.min(...values);
	const max = Math.max(...values);
	const range = max - min || 1;
	const points = values
		.map((value, index) => {
			const x = (index / (values.length - 1)) * 100;
			const y = 28 - ((value - min) / range) * 24;
			return `${x},${y}`;
		})
		.join(" ");

	return (
		<svg
			viewBox="0 0 100 32"
			className={cn("text-muted-foreground/60 h-8 w-20", className)}
			preserveAspectRatio="none"
			aria-hidden
		>
			<polyline
				points={points}
				fill="none"
				stroke="currentColor"
				strokeWidth="1.5"
				vectorEffect="non-scaling-stroke"
			/>
		</svg>
	);
}
