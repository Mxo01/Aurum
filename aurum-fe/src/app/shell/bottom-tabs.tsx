import { useAuth0 } from "@auth0/auth0-react";
import { Eye, EyeOff, LogOut, Moon, Sun, User } from "lucide-react";
import { useCallback, useEffect, useRef, useState } from "react";
import type { TransitionEvent } from "react";
import { NavLink, useNavigate } from "react-router";
import { Avatar } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import {
	Drawer,
	DrawerContent,
	DrawerDescription,
	DrawerHeader,
	DrawerTitle,
	DrawerTrigger
} from "@/components/ui/drawer";
import { Separator } from "@/components/ui/separator";
import { usePressAction } from "@/lib/use-press-action";
import { usePrivacyStore } from "@/stores/privacy";
import { useThemeStore } from "@/stores/theme";
import { cn } from "@/lib/utils";
import { NAV_ITEMS } from "./nav-items";

/** Upper bound for the close barrier, in case the drawer never reports its transition end. */
const CLOSE_BARRIER_FALLBACK_MS = 500;

function initials(name?: string) {
	if (!name) return "AU";
	return name
		.split(" ")
		.filter(Boolean)
		.slice(0, 2)
		.map(part => part.charAt(0).toUpperCase())
		.join("");
}

export function BottomTabs() {
	const { theme, toggleTheme } = useThemeStore();
	const { isPrivacy, togglePrivacy } = usePrivacyStore();
	const { user, logout } = useAuth0();
	const navigate = useNavigate();
	const [accountOpen, setAccountOpen] = useState(false);
	const [closeBarrierVisible, setCloseBarrierVisible] = useState(false);
	const closeBarrierTimer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);

	const clearCloseBarrierTimer = useCallback(() => {
		if (closeBarrierTimer.current === undefined) return;
		clearTimeout(closeBarrierTimer.current);
		closeBarrierTimer.current = undefined;
	}, []);

	const clearCloseBarrier = useCallback(() => {
		clearCloseBarrierTimer();
		setCloseBarrierVisible(false);
	}, [clearCloseBarrierTimer]);

	// Unmount clears the pending timer only: the component is gone, so its state
	// must not be touched during cleanup.
	useEffect(() => clearCloseBarrierTimer, [clearCloseBarrierTimer]);

	const handleAccountOpenChange = (open: boolean) => {
		setAccountOpen(open);
		if (open) {
			clearCloseBarrier();
			return;
		}
		setCloseBarrierVisible(true);
		closeBarrierTimer.current = window.setTimeout(clearCloseBarrier, CLOSE_BARRIER_FALLBACK_MS);
	};

	const handleDrawerTransitionEnd = (event: TransitionEvent<HTMLDivElement>) => {
		if (event.target !== event.currentTarget) return;
		clearCloseBarrier();
	};

	const pressProfile = usePressAction(() => {
		handleAccountOpenChange(false);
		navigate("/profile");
	});
	const pressPrivacy = usePressAction(togglePrivacy);
	const pressTheme = usePressAction(toggleTheme);
	const pressLogout = usePressAction(() =>
		logout({ logoutParams: { returnTo: window.location.origin } })
	);

	return (
		<>
			<nav
				aria-label="Main"
				className="fixed inset-x-0 bottom-0 z-40 pb-[var(--safe-area-bottom,0px)] md:hidden"
			>
				<div className="bg-background/80 supports-[backdrop-filter]:bg-background/60 border-t backdrop-blur-md">
					<div className="grid grid-cols-4">
						{NAV_ITEMS.map(item => (
							<NavLink
								key={item.to}
								to={item.to}
								className={({ isActive }) =>
									cn(
										"flex flex-col items-center gap-1 py-2.5 text-[11px] font-medium",
										isActive ? "text-foreground" : "text-muted-foreground"
									)
								}
							>
								<item.icon
									className="size-5"
									aria-hidden
								/>
								{item.label}
							</NavLink>
						))}

						<Drawer
							open={accountOpen}
							onOpenChange={handleAccountOpenChange}
						>
							<DrawerTrigger asChild>
								<button
									type="button"
									aria-label="Profile"
									className={cn(
										"flex flex-col items-center gap-1 py-2.5 text-[11px] font-medium",
										accountOpen ? "text-foreground" : "text-muted-foreground"
									)}
								>
									<Avatar
										src={user?.picture}
										alt={user?.name ?? "User"}
										fallback={initials(user?.name)}
										className="size-5"
									/>
									Profile
								</button>
							</DrawerTrigger>
							<DrawerContent onTransitionEnd={handleDrawerTransitionEnd}>
								<DrawerHeader className="flex-row items-center gap-3 pb-2">
									<Avatar
										src={user?.picture}
										alt={user?.name ?? "User"}
										fallback={initials(user?.name)}
										className="size-10"
									/>
									<div className="flex min-w-0 flex-col">
										<DrawerTitle className="truncate text-base">
											{isPrivacy ? "••••••••" : (user?.name ?? "Account")}
										</DrawerTitle>
										<DrawerDescription className="truncate text-xs">
											{isPrivacy ? "••••••••••••" : user?.email}
										</DrawerDescription>
									</div>
								</DrawerHeader>

								<div className="flex flex-col gap-1 px-2 pb-6">
									<Button
										variant="ghost"
										className="justify-start"
										{...pressProfile}
									>
										<User className="size-4" /> Profile
									</Button>
									<Button
										variant="ghost"
										className="justify-start"
										{...pressPrivacy}
									>
										{isPrivacy ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
										{isPrivacy ? "Show amounts" : "Hide amounts"}
									</Button>
									<Button
										variant="ghost"
										className="justify-start"
										{...pressTheme}
									>
										{theme === "dark" ? <Sun className="size-4" /> : <Moon className="size-4" />}
										{theme === "dark" ? "Light theme" : "Dark theme"}
									</Button>
									<Separator className="my-2" />
									<Button
										variant="ghost"
										className="text-danger hover:text-danger justify-start"
										{...pressLogout}
									>
										<LogOut className="size-4" /> Log out
									</Button>
								</div>
							</DrawerContent>
						</Drawer>
					</div>
				</div>
				<div
					aria-hidden
					className="bg-background pointer-events-none absolute inset-x-0 bottom-0 h-[var(--safe-area-bottom,0px)]"
				/>
			</nav>
			{/* Sibling of the nav on purpose: the nav is a z-40 stacking context,
			    so a z-50 child of it could never cover later page content. */}
			{closeBarrierVisible ? (
				<div
					aria-hidden
					className="fixed inset-0 z-50"
				/>
			) : null}
		</>
	);
}
