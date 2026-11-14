import { Tag } from "lucide-react";
import { createElement } from "react";
import { cn } from "@/lib/utils";
import { CATEGORY_ICON_OPTIONS } from "./icon-options";

const ICONS = new Map(CATEGORY_ICON_OPTIONS.map(option => [option.value, option.icon]));

export function CategoryIcon({ name, className }: { name?: string | null; className?: string }) {
	const Icon = (name ? ICONS.get(name) : undefined) ?? Tag;
	return createElement(Icon, { className: cn("size-4 shrink-0", className), "aria-hidden": true });
}
