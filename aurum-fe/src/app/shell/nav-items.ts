import { ArrowLeftRight, LayoutDashboard, Wallet } from "lucide-react";
import type { LucideIcon } from "lucide-react";

export interface NavItem {
	to: string;
	label: string;
	icon: LucideIcon;
}

export const NAV_ITEMS: NavItem[] = [
	{ to: "/dashboard", label: "Dashboard", icon: LayoutDashboard },
	{ to: "/assets", label: "Assets", icon: Wallet },
	{ to: "/cashflow", label: "Cash flow", icon: ArrowLeftRight }
];
