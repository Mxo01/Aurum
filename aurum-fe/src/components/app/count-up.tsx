import { useEffect, useRef, useState } from "react";

const DURATION = 1200;

function prefersReducedMotion() {
	return (
		typeof window !== "undefined" &&
		typeof window.matchMedia === "function" &&
		window.matchMedia("(prefers-reduced-motion: reduce)").matches
	);
}

interface CountUpProps {
	value: number;
	format: (value: number) => string;
	className?: string;
}

export function CountUp({ value, format, className }: CountUpProps) {
	const ref = useRef<HTMLSpanElement>(null);
	const started = useRef(false);
	const [display, setDisplay] = useState(() =>
		prefersReducedMotion() || typeof IntersectionObserver === "undefined" ? value : 0
	);

	useEffect(() => {
		const element = ref.current;
		if (
			!element ||
			started.current ||
			prefersReducedMotion() ||
			typeof IntersectionObserver === "undefined"
		)
			return;

		let frame = 0;
		const observer = new IntersectionObserver(
			entries => {
				if (!entries[0]?.isIntersecting || started.current) return;
				started.current = true;
				observer.disconnect();
				const start = performance.now();
				const tick = (now: number) => {
					const progress = Math.min(1, (now - start) / DURATION);
					const eased = 1 - Math.pow(1 - progress, 3);
					setDisplay(value * eased);
					if (progress < 1) frame = requestAnimationFrame(tick);
				};
				frame = requestAnimationFrame(tick);
			},
			{ threshold: 0.4 }
		);

		observer.observe(element);
		return () => {
			observer.disconnect();
			if (frame) cancelAnimationFrame(frame);
		};
	}, [value]);

	return (
		<span
			ref={ref}
			className={className}
		>
			{format(display)}
		</span>
	);
}
