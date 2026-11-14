import { ChevronDown } from "lucide-react";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Calendar } from "@/components/ui/calendar";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { parseDateOnly, toDateOnly } from "@/lib/format/date";
import { usePreferences } from "@/lib/format/use-preferences";
import { cn } from "@/lib/utils";

interface DatePickerProps {
	id?: string;
	value: string;
	onChange: (value: string) => void;
	label: string;
	min?: string;
	max?: string;
	fromYear?: number;
	toYear?: number;
	className?: string;
}

export function DatePicker({
	id,
	value,
	onChange,
	label,
	min,
	max,
	fromYear = 2000,
	toYear = new Date().getFullYear() + 10,
	className
}: DatePickerProps) {
	const { date } = usePreferences();
	const [open, setOpen] = useState(false);
	const selected = value ? parseDateOnly(value) : undefined;
	const minDate = min ? parseDateOnly(min) : undefined;
	const maxDate = max ? parseDateOnly(max) : undefined;
	const disabled = [
		...(minDate ? [{ before: minDate }] : []),
		...(maxDate ? [{ after: maxDate }] : [])
	];

	return (
		<Popover
			open={open}
			onOpenChange={setOpen}
		>
			<PopoverTrigger asChild>
				<Button
					id={id}
					variant="outline"
					aria-label={label}
					className={cn(
						"w-full justify-between gap-2 font-normal",
						!value && "text-muted-foreground",
						className
					)}
				>
					{value ? date(value) : "Pick a date"}
					<ChevronDown className="size-4 opacity-50" />
				</Button>
			</PopoverTrigger>
			<PopoverContent
				className="w-auto p-3"
				align="start"
			>
				<Calendar
					key={value}
					captionLayout="dropdown"
					mode="single"
					selected={selected}
					defaultMonth={selected}
					disabled={disabled.length > 0 ? disabled : undefined}
					startMonth={minDate ?? new Date(fromYear, 0, 1)}
					endMonth={maxDate ?? new Date(toYear, 11, 31)}
					onSelect={next => {
						if (!next) return;
						onChange(toDateOnly(next));
						setOpen(false);
					}}
				/>
			</PopoverContent>
		</Popover>
	);
}
