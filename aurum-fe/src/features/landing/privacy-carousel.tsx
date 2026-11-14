import { useEffect, useRef, useState, type ReactNode } from "react";
import type { LucideIcon } from "lucide-react";
import { cn } from "@/lib/utils";

interface PrivacyItem {
	icon: LucideIcon;
	title: string;
	body: string;
}

interface PrivacyCarouselProps {
	items: PrivacyItem[];
	header: ReactNode;
}

export function PrivacyCarousel({ items, header }: PrivacyCarouselProps) {
	const ref = useRef<HTMLDivElement>(null);
	const [active, setActive] = useState(0);

	useEffect(() => {
		const element = ref.current!;

		let frame = 0;
		const update = () => {
			frame = 0;
			const rect = element.getBoundingClientRect();
			const runway = element.offsetHeight - window.innerHeight;
			const progress = runway <= 0 ? 0 : Math.min(1, Math.max(0, -rect.top / runway));
			setActive(Math.min(items.length - 1, Math.round(progress * (items.length - 1))));
		};
		const onScroll = () => {
			if (frame) return;
			frame = requestAnimationFrame(update);
		};

		update();
		window.addEventListener("scroll", onScroll, { passive: true });
		window.addEventListener("resize", onScroll);
		return () => {
			window.removeEventListener("scroll", onScroll);
			window.removeEventListener("resize", onScroll);
			if (frame) cancelAnimationFrame(frame);
		};
	}, [items.length]);

	return (
		<div
			ref={ref}
			className="relative hidden h-[240vh] lg:block"
		>
			<div className="sticky top-14 pt-24 pb-10">
				{header}
				<div className="mx-auto w-full max-w-6xl px-4">
					<div className="relative mt-12 h-64">
						{items.map((item, index) => {
							const state = index === active ? "active" : index === active + 1 ? "next" : "hidden";

							return (
								<article
									key={item.title}
									aria-hidden={state !== "active"}
									className={cn(
										"border-border bg-card absolute inset-x-0 top-0 rounded-2xl border p-6 shadow-xl transition-all duration-500 ease-out",
										state === "active" && "translate-y-0 opacity-100 blur-0",
										state === "next" &&
											"translate-y-[calc(100%+1rem)] scale-[0.98] opacity-45 blur-[2px]",
										state === "hidden" && "translate-y-[calc(200%+2rem)] opacity-0 blur-md"
									)}
								>
									<div className="bg-muted text-muted-foreground flex size-9 items-center justify-center rounded-lg">
										<item.icon
											className="size-4"
											aria-hidden
										/>
									</div>
									<p className="mt-4 text-lg font-semibold">{item.title}</p>
									<p className="text-muted-foreground mt-1.5 max-w-2xl text-sm leading-relaxed">
										{item.body}
									</p>
									<div className="text-muted-foreground mt-6 flex items-center gap-3 text-[11px]">
										<span className="num">{String(index + 1).padStart(2, "0")}</span>
										<span className="bg-border h-px flex-1" />
										<span className="num">{String(items.length).padStart(2, "0")}</span>
									</div>
								</article>
							);
						})}
					</div>
				</div>
			</div>
		</div>
	);
}
