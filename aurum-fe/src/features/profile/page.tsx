import { useAuth0 } from "@auth0/auth0-react";
import { Camera, Download, Moon, Plug, Shield, Sun, Trash2, User, X } from "lucide-react";
import { useRef, useState } from "react";
import { Link } from "react-router";
import { toast } from "sonner";
import { PageHeader } from "@/components/app/page-header";
import {
	AlertDialog,
	AlertDialogAction,
	AlertDialogCancel,
	AlertDialogContent,
	AlertDialogDescription,
	AlertDialogFooter,
	AlertDialogHeader,
	AlertDialogTitle,
	AlertDialogTrigger
} from "@/components/ui/alert-dialog";
import { Avatar } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Separator } from "@/components/ui/separator";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { CURRENCIES, LOCALES, type Currency, type Locale } from "@/lib/api/types";
import { todayISO } from "@/lib/format/date";
import { useThemeStore } from "@/stores/theme";
import {
	useDeletePicture,
	useDeleteProfile,
	useExportData,
	useProfile,
	useUpdateCurrency,
	useUpdateLocale,
	useUpdateName,
	useUploadPicture
} from "./api";
import { McpSetup } from "./mcp-setup";

const MAX_PICTURE_BYTES = 5 * 1024 * 1024;
const NAME_MAX_LENGTH = 100;
const DELETE_CONFIRMATION = "DELETE";

export function ProfilePage() {
	return (
		<div className="mx-auto flex w-full max-w-4xl flex-col gap-5">
			<PageHeader
				title="Profile"
				description="Your account, preferences and integrations."
			/>

			<Tabs defaultValue="preferences">
				<TabsList className="grid h-auto w-full grid-cols-3 p-1 sm:inline-flex sm:h-9 sm:w-auto">
					<TabsTrigger
						value="preferences"
						className="px-2 py-1.5"
					>
						<User className="hidden size-4 sm:block" /> Preferences
					</TabsTrigger>
					<TabsTrigger
						value="integrations"
						className="px-2 py-1.5"
					>
						<Plug className="hidden size-4 sm:block" /> Integrations
					</TabsTrigger>
					<TabsTrigger
						value="data"
						className="px-2 py-1.5"
					>
						<Shield className="hidden size-4 sm:block" /> Data
						<span className="hidden sm:inline">&nbsp;& privacy</span>
					</TabsTrigger>
				</TabsList>

				<TabsContent value="preferences">
					<PreferencesTab />
				</TabsContent>
				<TabsContent value="integrations">
					<Card>
						<CardHeader>
							<CardTitle className="flex items-center gap-2">
								<Plug className="size-4" /> MCP integration
							</CardTitle>
							<CardDescription>
								Read and write Aurum data from an MCP client such as Claude.
							</CardDescription>
						</CardHeader>
						<CardContent>
							<McpSetup />
						</CardContent>
					</Card>
				</TabsContent>
				<TabsContent value="data">
					<DataTab />
				</TabsContent>
			</Tabs>
		</div>
	);
}

function PreferencesTab() {
	const { user } = useAuth0();
	const profile = useProfile();
	const { theme, setTheme } = useThemeStore();

	const updateName = useUpdateName();
	const updateCurrency = useUpdateCurrency();
	const updateLocale = useUpdateLocale();
	const uploadPicture = useUploadPicture();
	const deletePicture = useDeletePicture();

	const fileRef = useRef<HTMLInputElement>(null);
	const isGoogleProfile = user?.sub?.includes("google") ?? false;
	const customPicture = profile.data?.picture;
	const pictureUrl = customPicture
		? `data:image/jpeg;base64,${customPicture}`
		: (user?.picture ?? null);
	const fallback = (user?.name ?? "A")
		.split(" ")
		.filter(Boolean)
		.slice(0, 2)
		.map(part => part[0]?.toUpperCase() ?? "")
		.join("");

	const onFileSelected = (file: File | undefined) => {
		if (!file) return;
		if (file.size > MAX_PICTURE_BYTES) {
			toast.error("The picture must be 5 MB or smaller");
			return;
		}
		uploadPicture.mutate(file, {
			onSuccess: () => {
				if (fileRef.current) fileRef.current.value = "";
			}
		});
	};

	return (
		<div className="flex flex-col gap-4">
			<Card>
				<CardHeader>
					<CardTitle>Personal details</CardTitle>
					<CardDescription>
						{isGoogleProfile
							? "Managed by your Google account."
							: "Your name and picture, shown across the app."}
					</CardDescription>
				</CardHeader>
				<CardContent className="flex flex-col gap-5">
					<div className="flex flex-wrap items-center gap-4">
						<Avatar
							src={pictureUrl}
							alt="Profile picture"
							fallback={fallback}
							className="size-16"
						/>
						{!isGoogleProfile ? (
							<div className="flex flex-wrap items-center gap-2">
								<input
									ref={fileRef}
									type="file"
									accept="image/jpeg,image/png"
									className="hidden"
									onChange={event => onFileSelected(event.target.files?.[0])}
									aria-label="Upload profile picture"
								/>
								<Button
									variant="outline"
									size="sm"
									onClick={() => fileRef.current?.click()}
									disabled={uploadPicture.isPending}
								>
									<Camera className="size-4" /> Upload picture
								</Button>
								{customPicture ? (
									<Button
										variant="ghost"
										size="sm"
										onClick={() => deletePicture.mutate()}
										disabled={deletePicture.isPending}
									>
										<X className="size-4" /> Remove
									</Button>
								) : null}
								<span className="text-muted-foreground text-xs">JPG or PNG · max 5 MB</span>
							</div>
						) : null}
					</div>

					<Separator />

					<div className="flex flex-col gap-2">
						<Label htmlFor="profile-name">Name</Label>
						<Input
							id="profile-name"
							defaultValue={user?.name ?? ""}
							maxLength={NAME_MAX_LENGTH}
							disabled={isGoogleProfile}
							onBlur={event => {
								const value = event.target.value.trim();
								if (value && value !== user?.name) updateName.mutate(value);
							}}
						/>
						<span className="text-muted-foreground text-xs">{user?.email}</span>
					</div>
				</CardContent>
			</Card>

			<Card>
				<CardHeader>
					<CardTitle>Preferences</CardTitle>
					<CardDescription>Currency, locale and theme apply everywhere.</CardDescription>
				</CardHeader>
				<CardContent className="flex flex-col gap-4">
					<div className="flex flex-col justify-between gap-2 sm:flex-row sm:items-center">
						<div className="flex flex-col">
							<span className="text-sm">Currency</span>
							<span className="text-muted-foreground text-xs">
								Every amount is converted to this currency.
							</span>
						</div>
						<select
							aria-label="Currency"
							value={profile.data?.currency ?? "EUR"}
							onChange={event => updateCurrency.mutate(event.target.value as Currency)}
							disabled={updateCurrency.isPending}
							className="border-input bg-background focus-visible:ring-ring h-9 rounded-md border px-3 text-sm focus-visible:ring-2 focus-visible:outline-none disabled:opacity-60 sm:w-40"
						>
							{CURRENCIES.map(code => (
								<option
									key={code}
									value={code}
								>
									{code}
								</option>
							))}
						</select>
					</div>

					<Separator />

					<div className="flex flex-col justify-between gap-2 sm:flex-row sm:items-center">
						<div className="flex flex-col">
							<span className="text-sm">Locale</span>
							<span className="text-muted-foreground text-xs">Number and date formatting.</span>
						</div>
						<select
							aria-label="Locale"
							value={profile.data?.locale ?? "en-US"}
							onChange={event => updateLocale.mutate(event.target.value as Locale)}
							disabled={updateLocale.isPending}
							className="border-input bg-background focus-visible:ring-ring h-9 rounded-md border px-3 text-sm focus-visible:ring-2 focus-visible:outline-none disabled:opacity-60 sm:w-48"
						>
							{LOCALES.map(locale => (
								<option
									key={locale.value}
									value={locale.value}
								>
									{locale.label}
								</option>
							))}
						</select>
					</div>

					<Separator />

					<div className="flex flex-col justify-between gap-2 sm:flex-row sm:items-center">
						<div className="flex flex-col">
							<span className="text-sm">Theme</span>
							<span className="text-muted-foreground text-xs">Light or dark, your call.</span>
						</div>
						<div className="bg-muted flex w-fit items-center gap-0.5 rounded-md p-0.5">
							<button
								type="button"
								onClick={() => setTheme("light")}
								aria-pressed={theme === "light"}
								className={`focus-ring flex items-center gap-1.5 rounded px-2.5 py-1 text-[11px] font-medium transition-colors ${
									theme === "light"
										? "bg-card text-foreground shadow-sm"
										: "text-muted-foreground hover:text-foreground"
								}`}
							>
								<Sun className="size-3.5" /> Light
							</button>
							<button
								type="button"
								onClick={() => setTheme("dark")}
								aria-pressed={theme === "dark"}
								className={`focus-ring flex items-center gap-1.5 rounded px-2.5 py-1 text-[11px] font-medium transition-colors ${
									theme === "dark"
										? "bg-card text-foreground shadow-sm"
										: "text-muted-foreground hover:text-foreground"
								}`}
							>
								<Moon className="size-3.5" /> Dark
							</button>
						</div>
					</div>
				</CardContent>
			</Card>
		</div>
	);
}

function DataTab() {
	const exportData = useExportData();

	const download = () => {
		exportData.mutate(undefined, {
			onSuccess: data => {
				const blob = new Blob([JSON.stringify(data, null, 2)], { type: "application/json" });
				const url = URL.createObjectURL(blob);
				const anchor = document.createElement("a");
				anchor.href = url;
				anchor.download = `aurum-export-${todayISO()}.json`;
				anchor.click();
				URL.revokeObjectURL(url);
				toast.success("Export ready");
			}
		});
	};

	return (
		<div className="flex flex-col gap-4">
			<Card>
				<CardHeader>
					<CardTitle>Export my data</CardTitle>
					<CardDescription>
						Download every asset, snapshot, category and target as JSON. See the{" "}
						<Link
							to="/privacy"
							className="text-foreground underline underline-offset-2"
						>
							Privacy Policy
						</Link>
						.
					</CardDescription>
				</CardHeader>
				<CardContent>
					<Button
						variant="outline"
						onClick={download}
						disabled={exportData.isPending}
					>
						<Download className="size-4" /> Export JSON
					</Button>
				</CardContent>
			</Card>

			<Card className="border-danger/40">
				<CardHeader>
					<CardTitle className="text-danger flex items-center gap-2">
						<Trash2 className="size-4" /> Danger zone
					</CardTitle>
					<CardDescription>
						Deleting your account permanently removes your data and your Auth0 identity.
					</CardDescription>
				</CardHeader>
				<CardContent>
					<DeleteAccount />
				</CardContent>
			</Card>
		</div>
	);
}

function DeleteAccount() {
	const { logout } = useAuth0();
	const deleteProfile = useDeleteProfile();
	const [open, setOpen] = useState(false);
	const [confirmation, setConfirmation] = useState("");

	const close = (next: boolean) => {
		setOpen(next);
		if (!next) setConfirmation("");
	};

	return (
		<AlertDialog
			open={open}
			onOpenChange={close}
		>
			<AlertDialogTrigger asChild>
				<Button
					variant="destructive"
					size="sm"
				>
					<Trash2 className="size-4" /> Delete account
				</Button>
			</AlertDialogTrigger>
			<AlertDialogContent>
				<AlertDialogHeader>
					<AlertDialogTitle>Delete your account?</AlertDialogTitle>
					<AlertDialogDescription>
						This cannot be undone. Type {DELETE_CONFIRMATION} to confirm.
					</AlertDialogDescription>
				</AlertDialogHeader>
				<Input
					value={confirmation}
					onChange={event => setConfirmation(event.target.value)}
					placeholder={DELETE_CONFIRMATION}
					aria-label="Type DELETE to confirm"
				/>
				<AlertDialogFooter>
					<AlertDialogCancel>Cancel</AlertDialogCancel>
					<AlertDialogAction
						disabled={confirmation !== DELETE_CONFIRMATION || deleteProfile.isPending}
						onClick={() =>
							deleteProfile.mutate(undefined, {
								onSuccess: () => logout({ logoutParams: { returnTo: window.location.origin } })
							})
						}
					>
						Delete account
					</AlertDialogAction>
				</AlertDialogFooter>
			</AlertDialogContent>
		</AlertDialog>
	);
}
