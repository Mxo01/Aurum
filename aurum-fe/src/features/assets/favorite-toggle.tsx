import { Star } from "lucide-react";
import { Toggle } from "@/components/ui/toggle";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { cn } from "@/lib/utils";

interface FavoriteToggleProps {
	pressed: boolean;
	onPressedChange: (pressed: boolean) => void;
	label: string;
	className?: string;
}

export function FavoriteToggle({
	pressed,
	onPressedChange,
	label,
	className
}: FavoriteToggleProps) {
	return (
		<Tooltip>
			<TooltipTrigger asChild>
				<Toggle
					pressed={pressed}
					onPressedChange={onPressedChange}
					size="sm"
					aria-label={label}
					className={cn(
						"text-muted-foreground hover:text-foreground shrink-0 px-1.5 aria-pressed:bg-transparent aria-pressed:text-primary",
						className
					)}
				>
					<Star className={cn("size-3.5", pressed && "fill-primary")} />
				</Toggle>
			</TooltipTrigger>
			<TooltipContent>
				{pressed ? "Shown in the dashboard chart" : "Not shown in the dashboard chart"}
			</TooltipContent>
		</Tooltip>
	);
}
