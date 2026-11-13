import "react-day-picker/style.css";
import { ChevronLeft, ChevronRight } from "lucide-react";
import type { ComponentProps } from "react";
import { DayPicker } from "react-day-picker";
import { cn } from "@/lib/utils";

export function Calendar({ className, classNames, ...props }: ComponentProps<typeof DayPicker>) {
	return (
		<DayPicker
			showOutsideDays
			captionLayout="label"
			className={cn(
				"rdp-root w-fit",
				"[--rdp-accent-color:var(--primary)]",
				"[--rdp-accent-background-color:var(--accent)]",
				"[--rdp-day-height:2.25rem]",
				"[--rdp-day-width:2.25rem]",
				"[--rdp-day_button-height:2rem]",
				"[--rdp-day_button-width:2rem]",
				"[--rdp-day_button-border-radius:0.375rem]",
				"[--rdp-selected-border:0px_solid_transparent]",
				className
			)}
			classNames={{
				root: "w-fit",
				months: "relative flex flex-col",
				month: "flex flex-col gap-2",
				month_caption: "relative flex h-8 w-full items-center justify-center px-8",
				caption_label: "text-sm font-medium",
				nav: "absolute inset-x-0 top-0 flex w-full items-center justify-between",
				button_previous:
					"hover:bg-accent focus-visible:ring-ring inline-flex size-8 items-center justify-center rounded-md opacity-60 transition-colors hover:opacity-100 focus-visible:ring-2 focus-visible:outline-none",
				button_next:
					"hover:bg-accent focus-visible:ring-ring inline-flex size-8 items-center justify-center rounded-md opacity-60 transition-colors hover:opacity-100 focus-visible:ring-2 focus-visible:outline-none",
				month_grid: "w-full border-collapse",
				weekdays: "flex",
				weekday: "text-muted-foreground w-9 text-center text-[11px] font-normal",
				week: "mt-1 flex w-full",
				day: "relative size-9 p-0 text-center text-sm",
				day_button:
					"size-9 rounded-md border-0 p-0 font-normal transition-colors hover:bg-accent aria-selected:opacity-100",
				selected:
					"bg-primary text-primary-foreground hover:bg-primary hover:text-primary-foreground focus:bg-primary focus:text-primary-foreground rounded-md",
				today: "bg-accent text-accent-foreground rounded-md",
				outside: "text-muted-foreground/50",
				disabled: "text-muted-foreground opacity-50",
				hidden: "invisible",
				...classNames
			}}
			components={{
				Chevron: ({ orientation, ...chevronProps }) =>
					orientation === "left" ? (
						<ChevronLeft
							className="size-4"
							{...chevronProps}
						/>
					) : (
						<ChevronRight
							className="size-4"
							{...chevronProps}
						/>
					)
			}}
			{...props}
		/>
	);
}
