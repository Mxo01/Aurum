import { useAuth0 } from "@auth0/auth0-react";
import { Eye, EyeOff, LogOut, Menu, Moon, Sun, User } from "lucide-react";
import { useState } from "react";
import { NavLink, useNavigate } from "react-router";
import { Button } from "@/components/ui/button";
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetTrigger } from "@/components/ui/sheet";
import { Separator } from "@/components/ui/separator";
import { usePrivacyStore } from "@/stores/privacy";
import { useThemeStore } from "@/stores/theme";
import { cn } from "@/lib/utils";
import { NAV_ITEMS } from "./nav-items";

export function BottomTabs() {
	const { theme, toggleTheme } = useThemeStore();
	const { isPrivacy, togglePrivacy } = usePrivacyStore();
	const { logout } = useAuth0();
	const navigate = useNavigate();
	const [moreOpen, setMoreOpen] = useState(false);

	return (
		<nav
			aria-label="Main"
			className="bg-background fixed inset-x-0 bottom-0 z-40 border-t pb-[env(safe-area-inset-bottom)] md:hidden"
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

				<Sheet
					open={moreOpen}
					onOpenChange={setMoreOpen}
				>
					<SheetTrigger asChild>
						<button
							type="button"
							className="text-muted-foreground flex flex-col items-center gap-1 py-2.5 text-[11px] font-medium"
						>
							<Menu
								className="size-5"
								aria-hidden
							/>
							More
						</button>
					</SheetTrigger>
					<SheetContent
						side="right"
						className="md:hidden"
					>
						<SheetHeader>
							<SheetTitle>More</SheetTitle>
						</SheetHeader>
						<div className="flex flex-col gap-1">
							<Button
								variant="ghost"
								className="justify-start"
								onClick={() => {
									setMoreOpen(false);
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
					</SheetContent>
				</Sheet>
			</div>
		</nav>
	);
}
