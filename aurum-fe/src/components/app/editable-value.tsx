import { useEffect, useRef, useState } from "react";
import { Input } from "@/components/ui/input";
import { formatCurrency } from "@/lib/format/money";
import { usePreferences } from "@/lib/format/use-preferences";
import { cn } from "@/lib/utils";
import { MESSAGE, parseDecimalInput } from "@/lib/validation";

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
	const [invalid, setInvalid] = useState(false);
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
		setInvalid(false);
		setEditing(true);
	};

	const commit = () => {
		const parsed = parseDecimalInput(draft);
		if (parsed === null) {
			setInvalid(true);
			return;
		}

		setInvalid(false);
		setEditing(false);
		if (parsed !== value) onSave(parsed);
	};

	const cancel = () => {
		setInvalid(false);
		setEditing(false);
	};

	if (editing) {
		return (
			<div className="relative">
				<Input
					ref={inputRef}
					type="text"
					inputMode="decimal"
					value={draft}
					onChange={event => {
						setDraft(event.target.value);
						setInvalid(false);
					}}
					onBlur={commit}
					onKeyDown={event => {
						if (event.key === "Enter") commit();
						if (event.key === "Escape") cancel();
					}}
					aria-invalid={invalid}
					aria-label={`Edit ${label} in ${currency}`}
					className={cn(
						"num h-8 w-32",
						align === "left" ? "text-left" : "ml-auto text-right",
						className
					)}
				/>
				{invalid ? (
					<p
						role="alert"
						className="text-danger text-xs"
					>
						{MESSAGE.numberInvalid}
					</p>
				) : null}
			</div>
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
