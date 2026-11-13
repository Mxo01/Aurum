import { useEffect } from "react";
import { useLocation, useNavigate } from "react-router";
import { usePrivacyStore } from "@/stores/privacy";
import { useThemeStore } from "@/stores/theme";
import { useUiStore } from "@/stores/ui";

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

	useEffect(() => {
		const onKeyDown = (event: KeyboardEvent) => {
			if (event.metaKey || event.ctrlKey) {
				if (event.key.toLowerCase() === "k") {
					event.preventDefault();
					toggleCommand();
				}
				return;
			}

			if (isEditableTarget(event.target)) return;

			if (event.altKey) {
				const routes: Record<string, string> = {
					Digit1: "/dashboard",
					Digit2: "/assets",
					Digit3: "/cashflow",
					Digit4: "/profile"
				};
				const route = routes[event.code];

				if (route) {
					event.preventDefault();
					navigate(route);
					return;
				}

				if (event.code === "KeyN") {
					event.preventDefault();
					navigate("/assets?new=1");
					return;
				}
				if (event.code === "KeyT") {
					event.preventDefault();
					navigate("/dashboard?target=new");
					return;
				}
				if (event.code === "KeyP") {
					event.preventDefault();
					togglePrivacy();
					return;
				}
				if (event.code === "KeyM") {
					event.preventDefault();
					toggleTheme();
				}
				return;
			}

			if (event.key === "/") {
				event.preventDefault();
				toggleCommand();
			}
		};

		window.addEventListener("keydown", onKeyDown);
		return () => window.removeEventListener("keydown", onKeyDown);
	}, [navigate, toggleCommand, togglePrivacy, toggleTheme]);

	useEffect(() => {
		window.scrollTo({ top: 0 });
	}, [location.pathname]);
}
