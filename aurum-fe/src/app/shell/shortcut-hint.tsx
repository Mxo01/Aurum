import { useEffect } from "react";
import { Kbd } from "@/components/app/kbd";
import { useShortcutHintStore } from "@/stores/shortcut-hint";

export const SHORTCUT_HINT_DURATION = 1400;

export function ShortcutHintCard() {
	const hint = useShortcutHintStore(state => state.hint);
	const clearHint = useShortcutHintStore(state => state.clearHint);

	useEffect(() => {
		if (!hint) return;
		const timer = window.setTimeout(clearHint, SHORTCUT_HINT_DURATION);
		return () => window.clearTimeout(timer);
	}, [hint, clearHint]);

	if (!hint) return null;

	return (
		<div className="pointer-events-none fixed inset-x-0 top-[calc(0.75rem_+_env(safe-area-inset-top))] z-50 flex justify-center px-4">
			<div
				role="status"
				aria-live="polite"
				className="bg-popover text-popover-foreground border-border animate-in fade-in-0 slide-in-from-top-2 motion-reduce:animate-none flex items-center gap-2 rounded-lg border px-3 py-1.5 shadow-lg duration-200"
			>
				<span className="flex items-center gap-1">
					{hint.keys.map(key => (
						<Kbd key={key}>{key}</Kbd>
					))}
				</span>
				<span className="text-xs font-medium">{hint.label}</span>
			</div>
		</div>
	);
}
