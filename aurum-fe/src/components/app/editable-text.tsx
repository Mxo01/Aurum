import { useEffect, useRef, useState } from "react";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";

interface EditableTextProps {
	value: string;
	onSave: (value: string) => void;
	label: string;
	maxLength?: number;
	disabled?: boolean;
	className?: string;
}

export function EditableText({
	value,
	onSave,
	label,
	maxLength = 100,
	disabled,
	className
}: EditableTextProps) {
	const [editing, setEditing] = useState(false);
	const [draft, setDraft] = useState(value);
	const inputRef = useRef<HTMLInputElement>(null);

	useEffect(() => {
		if (editing) {
			inputRef.current?.focus();
			inputRef.current?.select();
		}
	}, [editing]);

	const start = () => {
		setDraft(value);
		setEditing(true);
	};

	const commit = () => {
		const next = draft.trim();
		setEditing(false);
		if (next && next !== value) onSave(next);
	};

	if (editing) {
		return (
			<Input
				ref={inputRef}
				value={draft}
				maxLength={maxLength}
				onChange={event => setDraft(event.target.value)}
				onBlur={commit}
				onKeyDown={event => {
					if (event.key === "Enter") event.currentTarget.blur();
					if (event.key === "Escape") setEditing(false);
				}}
				className={cn("h-8 text-sm", className)}
				aria-label={`Edit ${label}`}
			/>
		);
	}

	return (
		<button
			type="button"
			onClick={start}
			disabled={disabled}
			className={cn(
				"focus-ring group -mx-1 inline-flex items-center gap-1.5 rounded-md px-1 py-0.5 text-left text-sm font-medium transition-colors",
				!disabled && "hover:bg-accent",
				className
			)}
			aria-label={`Edit ${label}, currently ${value}`}
		>
			<span className="truncate">{value}</span>
		</button>
	);
}
