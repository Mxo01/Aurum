import { useEffect, useState } from "react";

const QUERY = "(prefers-reduced-motion: reduce)";

export function usePrefersReducedMotion() {
	const [reduced, setReduced] = useState(() => {
		if (typeof window === "undefined" || typeof window.matchMedia !== "function") return false;
		return window.matchMedia(QUERY).matches;
	});

	useEffect(() => {
		if (typeof window.matchMedia !== "function") return;
		const query = window.matchMedia(QUERY);
		const update = () => setReduced(query.matches);
		update();
		query.addEventListener("change", update);
		return () => query.removeEventListener("change", update);
	}, []);

	return reduced;
}
