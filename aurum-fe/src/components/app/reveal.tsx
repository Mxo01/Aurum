import { useEffect, useRef, useState, type CSSProperties, type ReactNode } from "react";
import { cn } from "@/lib/utils";

interface RevealProps {
	children: ReactNode;
	className?: string;
	delay?: number;
}

export function Reveal({ children, className, delay = 0 }: RevealProps) {
	const ref = useRef<HTMLDivElement>(null);
	const [visible, setVisible] = useState(() => typeof IntersectionObserver === "undefined");

	useEffect(() => {
		const element = ref.current!;

		if (typeof IntersectionObserver === "undefined") return;

		const observer = new IntersectionObserver(
			entries => setVisible(entries[0]?.isIntersecting ?? false),
			{ threshold: 0.12, rootMargin: "0px 0px -6% 0px" }
		);
		observer.observe(element);
		return () => observer.disconnect();
	}, []);

	return (
		<div
			ref={ref}
			className={cn("reveal", className)}
			data-visible={visible}
			style={{ "--reveal-delay": `${delay}ms` } as CSSProperties}
		>
			{children}
		</div>
	);
}
