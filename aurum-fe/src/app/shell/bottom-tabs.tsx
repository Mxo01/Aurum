import { useAuth0 } from "@auth0/auth0-react";
import { Eye, EyeOff, LogOut, Moon, Sun, User } from "lucide-react";
import { useState } from "react";
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
import { usePrivacyStore } from "@/stores/privacy";
import { useThemeStore } from "@/stores/theme";
import { cn } from "@/lib/utils";
import { NAV_ITEMS } from "./nav-items";

function initials(name?: string) {
	if (!name) return "AU";
	return name
		.split(" ")
		.filter(Boolean)
		.slice(0, 2)
		.map(part => part[0]?.toUpperCase() ?? "")
		.join("");
}

export function BottomTabs() {
	const { theme, toggleTheme } = useThemeStore();
	const { isPrivacy, togglePrivacy } = usePrivacyStore();
	const { user, logout } = useAuth0();
	const navigate = useNavigate();
	const [accountOpen, setAccountOpen] = useState(false);

	return (
		<nav
			aria-label="Main"
			className="bg-background/80 supports-[backdrop-filter]:bg-background/60 fixed inset-x-0 bottom-0 z-40 border-t pb-[env(safe-area-inset-bottom)] backdrop-blur-md md:hidden"
		>
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
					onOpenChange={setAccountOpen}
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
					<DrawerContent>
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
								onClick={() => {
									setAccountOpen(false);
									navigate("/profile");
								}}
							>
								<User className="size-4" /> Profile
							</Button>
							<Button
								variant="ghost"
								className="justify-start"
								onClick={togglePrivacy}
							>
								{isPrivacy ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
								{isPrivacy ? "Show amounts" : "Hide amounts"}
							</Button>
							<Button
								variant="ghost"
								className="justify-start"
								onClick={toggleTheme}
							>
								{theme === "dark" ? <Sun className="size-4" /> : <Moon className="size-4" />}
								{theme === "dark" ? "Light theme" : "Dark theme"}
							</Button>
							<Separator className="my-2" />
							<Button
								variant="ghost"
								className="text-danger hover:text-danger justify-start"
								onClick={() => logout({ logoutParams: { returnTo: window.location.origin } })}
							>
								<LogOut className="size-4" /> Log out
							</Button>
						</div>
					</DrawerContent>
				</Drawer>
			</div>
		</nav>
	);
}
