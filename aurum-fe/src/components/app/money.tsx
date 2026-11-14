import { usePreferences } from "@/lib/format/use-preferences";
import { cn } from "@/lib/utils";
import type { MoneyOptions } from "@/lib/format/money";

interface MoneyProps {
	value: number;
	digits?: number;
	compact?: boolean;
	signDisplay?: MoneyOptions["signDisplay"];
	className?: string;
}

export function Money({ value, digits, compact, signDisplay, className }: MoneyProps) {
	const { money } = usePreferences();
	return (
		<span className={cn("num", className)}>{money(value, { digits, compact, signDisplay })}</span>
	);
}
