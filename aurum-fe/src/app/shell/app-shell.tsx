import { Outlet } from "react-router";
import { BottomTabs } from "./bottom-tabs";
import { CommandPalette } from "./command-palette";
import { ShortcutHintCard } from "./shortcut-hint";
import { TopBar } from "./top-bar";
import { useShortcuts } from "./use-shortcuts";

export function AppShell() {
	useShortcuts();

	return (
		<div className="flex min-h-dvh flex-col">
			<TopBar />
			<main className="mx-auto w-full max-w-[1440px] flex-1 px-4 pt-[calc(1.5rem_+_env(safe-area-inset-top))] pb-24 md:pb-10">
				<Outlet />
			</main>
			<BottomTabs />
			<CommandPalette />
			<ShortcutHintCard />
		</div>
	);
}
