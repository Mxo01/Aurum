import { useEffect } from "react";
import { useLocation, useNavigate } from "react-router";
import { commandLabel, optionLabel } from "@/lib/platform";
import { usePrivacyStore } from "@/stores/privacy";
import { useShortcutHintStore } from "@/stores/shortcut-hint";
import { useThemeStore } from "@/stores/theme";
import { useUiStore } from "@/stores/ui";

const CHORD_TIMEOUT = 1500;

interface RouteShortcut {
	to: string;
	label: string;
	keys: string[];
}

const ROUTES: Record<string, RouteShortcut> = {
	KeyD: { to: "/dashboard", label: "Dashboard", keys: [optionLabel, "D"] },
	KeyA: { to: "/assets", label: "Assets", keys: [optionLabel, "A"] },
	KeyC: { to: "/cashflow", label: "Cash flow", keys: [optionLabel, "C"] },
	KeyP: { to: "/profile", label: "Profile", keys: [optionLabel, "P"] }
};

function isEditableTarget(target: EventTarget | null) {
	if (!(target instanceof HTMLElement)) return false;
	const tag = target.tagName.toLowerCase();
	return (
		target.isContentEditable ||
		tag === "input" ||
		tag === "textarea" ||
		tag === "select" ||
		tag === "button"
	);
}

export function useShortcuts() {
	const navigate = useNavigate();
	const location = useLocation();
	const togglePrivacy = usePrivacyStore(state => state.togglePrivacy);
	const toggleCommand = useUiStore(state => state.toggleCommand);
	const toggleTheme = useThemeStore(state => state.toggleTheme);
	const showHint = useShortcutHintStore(state => state.showHint);

	useEffect(() => {
		let newChordAt = 0;

		const onKeyDown = (event: KeyboardEvent) => {
			if (event.metaKey || event.ctrlKey) {
				if (event.key.toLowerCase() === "k") {
					event.preventDefault();
					showHint({ keys: [`${commandLabel}K`], label: "Command palette" });
					toggleCommand();
				}
				return;
			}

			if (isEditableTarget(event.target)) {
				newChordAt = 0;
				return;
			}

			if (event.altKey) {
				if (event.code === "KeyN") {
					event.preventDefault();
					newChordAt = Date.now();
					return;
				}

				const route = ROUTES[event.code];

				if (route) {
					event.preventDefault();
					showHint({ keys: route.keys, label: route.label });
					navigate(route.to);
					return;
				}

				if (event.code === "KeyH") {
					event.preventDefault();
					showHint({ keys: [optionLabel, "H"], label: "Toggle amounts" });
					togglePrivacy();
					return;
				}
				if (event.code === "KeyT") {
					event.preventDefault();
					showHint({ keys: [optionLabel, "T"], label: "Toggle theme" });
					toggleTheme();
				}
				return;
			}

			if (newChordAt > 0) {
				const inChord = Date.now() - newChordAt <= CHORD_TIMEOUT;
				newChordAt = 0;

				if (inChord) {
					if (event.code === "KeyA") {
						event.preventDefault();
						showHint({ keys: [`${optionLabel}N`, "A"], label: "New asset" });
						navigate("/assets?new=1");
						return;
					}
					if (event.code === "KeyT") {
						event.preventDefault();
						showHint({ keys: [`${optionLabel}N`, "T"], label: "New target" });
						navigate("/dashboard?target=new");
						return;
					}
				}
			}

			if (event.key === "/") {
				event.preventDefault();
				showHint({ keys: ["/"], label: "Command palette" });
				toggleCommand();
			}
		};

		window.addEventListener("keydown", onKeyDown);
		return () => window.removeEventListener("keydown", onKeyDown);
	}, [navigate, showHint, toggleCommand, togglePrivacy, toggleTheme]);

	useEffect(() => {
		window.scrollTo({ top: 0 });
	}, [location.pathname]);
}
