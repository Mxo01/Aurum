import { useEffect } from "react";
import { useLocation, useNavigate } from "react-router";
import { usePrivacyStore } from "@/stores/privacy";
import { useUiStore } from "@/stores/ui";

const SEQUENCE_TIMEOUT_MS = 900;

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

	useEffect(() => {
		let pendingG = 0;

		const onKeyDown = (event: KeyboardEvent) => {
			if (event.metaKey || event.ctrlKey) {
				if (event.key.toLowerCase() === "k") {
					event.preventDefault();
					toggleCommand();
				}
				return;
			}

			if (isEditableTarget(event.target)) return;

			const key = event.key.toLowerCase();
			const now = Date.now();

			if (key === "g") {
				pendingG = now;
				return;
			}

			if (now - pendingG < SEQUENCE_TIMEOUT_MS) {
				const routes: Record<string, string> = {
					d: "/dashboard",
					a: "/assets",
					c: "/cashflow",
					p: "/profile"
				};
				const route = routes[key];
				if (route) {
					event.preventDefault();
					pendingG = 0;
					navigate(route);
					return;
				}
			}

			if (key === "n") {
				event.preventDefault();
				navigate("/assets?new=1");
				return;
			}

			if (key === "/") {
				event.preventDefault();
				toggleCommand();
				return;
			}

			if (key === "p") {
				event.preventDefault();
				togglePrivacy();
			}
		};

		window.addEventListener("keydown", onKeyDown);
		return () => window.removeEventListener("keydown", onKeyDown);
	}, [navigate, toggleCommand, togglePrivacy]);

	useEffect(() => {
		window.scrollTo({ top: 0 });
	}, [location.pathname]);
}
