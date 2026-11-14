import { useEffect, useRef, useState } from "react";
import { Input } from "@/components/ui/input";
import { formatCurrency } from "@/lib/format/money";
import { usePreferences } from "@/lib/format/use-preferences";
import { cn } from "@/lib/utils";

interface EditableValueProps {
	value: number;
	currency: string;
	onSave: (value: number) => void;
	label?: string;
	align?: "left" | "right";
	disabled?: boolean;
	className?: string;
}

export function EditableValue({
	value,
	currency,
	onSave,
	label = "value",
	align = "right",
	disabled,
	className
}: EditableValueProps) {
	const { locale, isPrivacy } = usePreferences();
	const [editing, setEditing] = useState(false);
	const [draft, setDraft] = useState(String(value));
	const inputRef = useRef<HTMLInputElement>(null);

	useEffect(() => {
		if (editing) {
			inputRef.current?.focus();
			inputRef.current?.select();
		}
	}, [editing]);

	const display = formatCurrency(value, { currency, locale, privacy: isPrivacy });

	const start = () => {
		setDraft(String(value));
		setEditing(true);
	};

	const commit = () => {
		const parsed = Number(draft.replace(",", "."));
		setEditing(false);
		if (Number.isFinite(parsed) && parsed !== value) onSave(parsed);
	};

	if (editing) {
		return (
			<Input
				ref={inputRef}
				type="number"
				step="0.01"
				value={draft}
				onChange={event => setDraft(event.target.value)}
				onBlur={commit}
				onKeyDown={event => {
					if (event.key === "Enter") event.currentTarget.blur();
					if (event.key === "Escape") setEditing(false);
				}}
				className={cn(
					"num h-8 w-32",
					align === "left" ? "text-left" : "ml-auto text-right",
					className
				)}
				aria-label={`Edit ${label} in ${currency}`}
			/>
		);
	}

	return (
		<button
			type="button"
			onClick={start}
			disabled={disabled}
			className={cn(
				"num focus-ring group -mx-1.5 inline-flex items-center gap-1.5 rounded-md px-1.5 py-0.5 transition-colors",
				!disabled && "hover:bg-accent",
				align === "left" ? "justify-start text-left" : "justify-end text-right",
				className
			)}
			aria-label={`Edit ${label}, currently ${display}`}
		>
			{display}
		</button>
	);
}
