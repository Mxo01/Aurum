import {
	ArrowLeftRight,
	Eye,
	EyeOff,
	LayoutDashboard,
	Moon,
	Plus,
	Search,
	Sun,
	User,
	Wallet,
	Target
} from "lucide-react";
import { useNavigate } from "react-router";
import {
	CommandDialog,
	CommandEmpty,
	CommandGroup,
	CommandInput,
	CommandItem,
	CommandList,
	CommandSeparator
} from "@/components/ui/command";
import { usePrivacyStore } from "@/stores/privacy";
import { useThemeStore } from "@/stores/theme";
import { useUiStore } from "@/stores/ui";

export function CommandPalette() {
	const open = useUiStore(state => state.commandOpen);
	const setOpen = useUiStore(state => state.setCommandOpen);
	const { theme, toggleTheme } = useThemeStore();
	const { isPrivacy, togglePrivacy } = usePrivacyStore();
	const navigate = useNavigate();

	const run = (action: () => void) => {
		setOpen(false);
		action();
	};

	return (
		<CommandDialog open={open} onOpenChange={setOpen}>
			<CommandInput placeholder="Type a command or search…" />
			<CommandList>
				<CommandEmpty>No results.</CommandEmpty>
				<CommandGroup heading="Navigate">
					<CommandItem onSelect={() => run(() => navigate("/dashboard"))}>
						<LayoutDashboard className="size-4" />
						Dashboard
					</CommandItem>
					<CommandItem onSelect={() => run(() => navigate("/assets"))}>
						<Wallet className="size-4" />
						Assets
					</CommandItem>
					<CommandItem onSelect={() => run(() => navigate("/cashflow"))}>
						<ArrowLeftRight className="size-4" />
						Cash flow
					</CommandItem>
					<CommandItem onSelect={() => run(() => navigate("/profile"))}>
						<User className="size-4" />
						Profile
					</CommandItem>
				</CommandGroup>
				<CommandSeparator />
				<CommandGroup heading="Actions">
					<CommandItem onSelect={() => run(() => navigate("/assets?new=1"))}>
						<Plus className="size-4" />
						New asset
					</CommandItem>
					<CommandItem onSelect={() => run(() => navigate("/dashboard?target=new"))}>
						<Target className="size-4" />
						New target
					</CommandItem>
					<CommandItem onSelect={() => run(togglePrivacy)}>
						{isPrivacy ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
						{isPrivacy ? "Show amounts" : "Hide amounts"}
					</CommandItem>
					<CommandItem onSelect={() => run(toggleTheme)}>
						{theme === "dark" ? <Sun className="size-4" /> : <Moon className="size-4" />}
						{theme === "dark" ? "Switch to light theme" : "Switch to dark theme"}
					</CommandItem>
				</CommandGroup>
				<CommandSeparator />
				<CommandGroup heading="Help">
					<CommandItem disabled>
						<Search className="size-4" />
						Shortcuts: g d, g a, g c, n, p, /
					</CommandItem>
				</CommandGroup>
			</CommandList>
		</CommandDialog>
	);
}
