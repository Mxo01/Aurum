import { ArrowDownRight, ArrowUpRight } from "lucide-react";
import { usePreferences } from "@/lib/format/use-preferences";
import { cn } from "@/lib/utils";

interface DeltaProps {
	value: number | null | undefined;
	invert?: boolean;
	showIcon?: boolean;
	digits?: number;
	className?: string;
}

export function Delta({ value, invert = false, showIcon = true, digits = 2, className }: DeltaProps) {
	const { percent } = usePreferences();

	if (value === null || value === undefined) return null;

	const positive = invert ? value < 0 : value >= 0;
	const Icon = value >= 0 ? ArrowUpRight : ArrowDownRight;

	return (
		<span
			className={cn(
				"inline-flex items-center gap-1 text-xs font-medium tabular-nums",
				positive ? "text-success" : "text-danger",
				className
			)}
		>
			{showIcon ? <Icon className="size-3.5" aria-hidden /> : null}
			{percent(value, digits)}
		</span>
	);
}
