import { useAuth0 } from "@auth0/auth0-react";
import { Eye, EyeOff, LogOut, Moon, Search, Sun, User } from "lucide-react";
import { NavLink, useNavigate } from "react-router";
import { Avatar } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import {
	DropdownMenu,
	DropdownMenuContent,
	DropdownMenuItem,
	DropdownMenuLabel,
	DropdownMenuSeparator,
	DropdownMenuTrigger
} from "@/components/ui/dropdown-menu";
import { usePrivacyStore } from "@/stores/privacy";
import { useThemeStore } from "@/stores/theme";
import { useUiStore } from "@/stores/ui";
import { cn } from "@/lib/utils";
import { Brand } from "./brand";
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

export function TopBar() {
	const { user, logout } = useAuth0();
	const { theme, toggleTheme } = useThemeStore();
	const { isPrivacy, togglePrivacy } = usePrivacyStore();
	const setCommandOpen = useUiStore(state => state.setCommandOpen);
	const navigate = useNavigate();

	return (
		<header className="bg-background sticky top-0 z-40 border-b">
			<div className="mx-auto flex h-14 max-w-[1440px] items-center gap-6 px-4">
				<Brand />

				<nav
					aria-label="Main"
					className="hidden items-center gap-1 md:flex"
				>
					{NAV_ITEMS.map(item => (
						<NavLink
							key={item.to}
							to={item.to}
							className={({ isActive }) =>
								cn(
									"focus-ring flex items-center gap-2 rounded-md px-3 py-1.5 text-sm font-medium transition-colors",
									isActive
										? "bg-accent text-accent-foreground"
										: "text-muted-foreground hover:text-foreground hover:bg-accent/60"
								)
							}
						>
							<item.icon
								className="size-4"
								aria-hidden
							/>
							{item.label}
						</NavLink>
					))}
				</nav>

				<div className="ml-auto flex items-center gap-1">
					<Button
						variant="ghost"
						size="icon-sm"
						className="hidden sm:inline-flex"
						onClick={() => setCommandOpen(true)}
						aria-label="Open command palette"
					>
						<Search className="size-4" />
					</Button>
					<Button
						variant="ghost"
						size="icon-sm"
						onClick={togglePrivacy}
						aria-label={isPrivacy ? "Show amounts" : "Hide amounts"}
						aria-pressed={isPrivacy}
					>
						{isPrivacy ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
					</Button>
					<Button
						variant="ghost"
						size="icon-sm"
						onClick={toggleTheme}
						aria-label={theme === "dark" ? "Switch to light theme" : "Switch to dark theme"}
					>
						{theme === "dark" ? <Sun className="size-4" /> : <Moon className="size-4" />}
					</Button>

					<DropdownMenu>
						<DropdownMenuTrigger asChild>
							<Button
								variant="ghost"
								size="icon-sm"
								className="ml-1 rounded-full"
								aria-label="Account menu"
							>
								<Avatar
									src={user?.picture}
									alt={user?.name ?? "User"}
									fallback={initials(user?.name)}
								/>
							</Button>
						</DropdownMenuTrigger>
						<DropdownMenuContent
							align="end"
							className="w-60"
						>
							<DropdownMenuLabel className="flex flex-col gap-0.5 px-2 py-2">
								<span className="text-foreground truncate text-sm font-medium">
									{isPrivacy ? "••••••••" : (user?.name ?? "Account")}
								</span>
								<span className="truncate text-[11px] font-normal">
									{isPrivacy ? "••••••••••••" : user?.email}
								</span>
							</DropdownMenuLabel>
							<DropdownMenuSeparator />
							<DropdownMenuItem onSelect={() => navigate("/profile")}>
								<User className="size-4" />
								Profile
							</DropdownMenuItem>
							<DropdownMenuItem onSelect={togglePrivacy}>
								{isPrivacy ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
								{isPrivacy ? "Show amounts" : "Hide amounts"}
							</DropdownMenuItem>
							<DropdownMenuItem onSelect={toggleTheme}>
								{theme === "dark" ? <Sun className="size-4" /> : <Moon className="size-4" />}
								{theme === "dark" ? "Light theme" : "Dark theme"}
							</DropdownMenuItem>
							<DropdownMenuSeparator />
							<DropdownMenuItem
								className="text-danger focus:text-danger"
								onSelect={() => logout({ logoutParams: { returnTo: window.location.origin } })}
							>
								<LogOut className="size-4" />
								Log out
							</DropdownMenuItem>
						</DropdownMenuContent>
					</DropdownMenu>
				</div>
			</div>
		</header>
	);
}
