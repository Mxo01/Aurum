import { create } from "zustand";

export interface ShortcutHint {
	id: number;
	keys: string[];
	label: string;
}

interface ShortcutHintState {
	hint: ShortcutHint | null;
	showHint: (hint: Omit<ShortcutHint, "id">) => void;
	clearHint: () => void;
}

let nextId = 0;

export const useShortcutHintStore = create<ShortcutHintState>(set => ({
	hint: null,
	showHint: hint => set({ hint: { ...hint, id: (nextId += 1) } }),
	clearHint: () => set({ hint: null })
}));
