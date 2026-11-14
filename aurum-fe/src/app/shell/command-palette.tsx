import {
	ArrowLeftRight,
	Eye,
	EyeOff,
	LayoutDashboard,
	Moon,
	Plus,
	Search,
	Sun,
	Target,
	User,
	Wallet
} from "lucide-react";
import { useState } from "react";
import { useNavigate } from "react-router";
import { Kbd } from "@/components/app/kbd";
import { optionLabel } from "@/lib/platform";
import {
	CommandDialog,
	CommandEmpty,
	CommandGroup,
	CommandInput,
	CommandItem,
	CommandList,
	CommandSeparator
} from "@/components/ui/command";
import { useAssets } from "@/features/assets/api";
import { usePrivacyStore } from "@/stores/privacy";
import { useThemeStore } from "@/stores/theme";
import { useUiStore } from "@/stores/ui";

function CommandHint({ keys }: { keys: string[] }) {
	return (
		<span className="ml-auto hidden items-center gap-1 pl-2 md:inline-flex">
			{keys.map(key => (
				<Kbd key={key}>{key}</Kbd>
			))}
		</span>
	);
}

export function CommandPalette() {
	const open = useUiStore(state => state.commandOpen);
	const setOpen = useUiStore(state => state.setCommandOpen);
	const { theme, toggleTheme } = useThemeStore();
	const { isPrivacy, togglePrivacy } = usePrivacyStore();
	const navigate = useNavigate();
	const [search, setSearch] = useState("");
	const assets = useAssets(open);

	const query = search.trim().toLowerCase();
	const matches = query
		? (assets.data ?? [])
				.filter(
					asset =>
						asset.name.toLowerCase().includes(query) ||
						asset.categoryName.toLowerCase().includes(query)
				)
				.slice(0, 5)
		: [];

	const handleOpenChange = (next: boolean) => {
		setOpen(next);
		setSearch("");
	};

	const run = (action: () => void) => {
		handleOpenChange(false);
		action();
	};

	return (
		<CommandDialog
			open={open}
			onOpenChange={handleOpenChange}
		>
			<CommandInput
				placeholder="Type a command or search…"
				value={search}
				onValueChange={setSearch}
			/>
			<CommandList>
				<CommandEmpty>No results.</CommandEmpty>
				<CommandGroup heading="Navigate">
					<CommandItem onSelect={() => run(() => navigate("/dashboard"))}>
						<LayoutDashboard className="size-4" />
						Dashboard
						<CommandHint keys={[optionLabel, "D"]} />
					</CommandItem>
					<CommandItem onSelect={() => run(() => navigate("/assets"))}>
						<Wallet className="size-4" />
						Assets
						<CommandHint keys={[optionLabel, "A"]} />
					</CommandItem>
					<CommandItem onSelect={() => run(() => navigate("/cashflow"))}>
						<ArrowLeftRight className="size-4" />
						Cash flow
						<CommandHint keys={[optionLabel, "C"]} />
					</CommandItem>
					<CommandItem onSelect={() => run(() => navigate("/profile"))}>
						<User className="size-4" />
						Profile
						<CommandHint keys={[optionLabel, "P"]} />
					</CommandItem>
				</CommandGroup>

				<CommandSeparator />

				<CommandGroup heading="Actions">
					<CommandItem onSelect={() => run(() => navigate("/assets?new=1"))}>
						<Plus className="size-4" />
						New asset
						<CommandHint keys={[`${optionLabel}N`, "A"]} />
					</CommandItem>
					<CommandItem onSelect={() => run(() => navigate("/dashboard?target=new"))}>
						<Target className="size-4" />
						New target
						<CommandHint keys={[`${optionLabel}N`, "T"]} />
					</CommandItem>
					<CommandItem onSelect={() => run(togglePrivacy)}>
						{isPrivacy ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
						{isPrivacy ? "Show amounts" : "Hide amounts"}
						<CommandHint keys={[optionLabel, "H"]} />
					</CommandItem>
					<CommandItem onSelect={() => run(toggleTheme)}>
						{theme === "dark" ? <Sun className="size-4" /> : <Moon className="size-4" />}
						{theme === "dark" ? "Switch to light theme" : "Switch to dark theme"}
						<CommandHint keys={[optionLabel, "T"]} />
					</CommandItem>
				</CommandGroup>

				{query ? (
					<>
						<CommandSeparator />
						<CommandGroup heading="Assets">
							{matches.map(asset => (
								<CommandItem
									key={asset.id}
									onSelect={() =>
										run(() => navigate(`/assets?search=${encodeURIComponent(asset.name)}`))
									}
								>
									<Wallet className="size-4" />
									<span className="truncate">{asset.name}</span>
									<span className="text-muted-foreground ml-auto text-[11px]">
										{asset.categoryName}
									</span>
								</CommandItem>
							))}
							{matches.length === 0 ? (
								<CommandItem
									onSelect={() =>
										run(() => navigate(`/assets?search=${encodeURIComponent(search.trim())}`))
									}
								>
									<Search className="size-4" />
									Search assets for “{search.trim()}”
								</CommandItem>
							) : null}
						</CommandGroup>
					</>
				) : null}
			</CommandList>
		</CommandDialog>
	);
}
