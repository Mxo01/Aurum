import { LineChart } from "lucide-react";
import { Toggle } from "@/components/ui/toggle";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { cn } from "@/lib/utils";

interface TrackToggleProps {
	pressed: boolean;
	onPressedChange: (pressed: boolean) => void;
	label: string;
	className?: string;
}

export function TrackToggle({ pressed, onPressedChange, label, className }: TrackToggleProps) {
	return (
		<Tooltip>
			<TooltipTrigger asChild>
				<Toggle
					pressed={pressed}
					onPressedChange={onPressedChange}
					variant="outline"
					size="sm"
					aria-label={label}
					className={cn("aria-pressed:border-primary/40 aria-pressed:text-primary", className)}
				>
					<LineChart className="size-3.5" />
				</Toggle>
			</TooltipTrigger>
			<TooltipContent>
				{pressed ? "Shown in the dashboard chart" : "Not shown in the dashboard chart"}
			</TooltipContent>
		</Tooltip>
	);
}
