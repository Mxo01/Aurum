import { Plus, Search, Tag, TrendingUp, TrendingDown, Wallet } from "lucide-react";
import { useState } from "react";
import { useSearchParams } from "react-router";
import { EmptyState } from "@/components/app/empty-state";
import { PageHeader } from "@/components/app/page-header";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
	AlertDialog,
	AlertDialogAction,
	AlertDialogCancel,
	AlertDialogContent,
	AlertDialogDescription,
	AlertDialogFooter,
	AlertDialogHeader,
	AlertDialogTitle
} from "@/components/ui/alert-dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { Skeleton } from "@/components/ui/skeleton";
import { useCategories } from "@/features/categories/api";
import { CategoriesSheet } from "@/features/categories/categories-sheet";
import { assetValue } from "@/lib/format/asset";
import { usePreferences } from "@/lib/format/use-preferences";
import type { AssetDTO } from "@/lib/api/types";
import { AssetFormSheet } from "./asset-form-sheet";
import { AssetList } from "./asset-list";
import { portfolioTotals } from "./assets.utils";
import {
	useAssets,
	useDeleteAsset,
	useRenameAsset,
	useToggleFavorite,
	useUpdateCurrentValue
} from "./api";
import { SnapshotsSheet } from "@/features/snapshots/snapshots-sheet";

type TypeFilter = "ALL" | "ASSET" | "LIABILITY";

const FILTERS: { value: TypeFilter; label: string }[] = [
	{ value: "ALL", label: "All" },
	{ value: "ASSET", label: "Assets" },
	{ value: "LIABILITY", label: "Liabilities" }
];

export function AssetsPage() {
	const [searchParams, setSearchParams] = useSearchParams();
	const { money } = usePreferences();
	const assets = useAssets();
	const categories = useCategories();
	const deleteAsset = useDeleteAsset();
	const updateValue = useUpdateCurrentValue();
	const renameAsset = useRenameAsset();
	const toggleFavorite = useToggleFavorite();

	const query = searchParams.get("search") ?? "";
	const [filter, setFilter] = useState<TypeFilter>("ALL");
	const [hideZero, setHideZero] = useState(true);
	const [collapsed, setCollapsed] = useState<Set<string>>(new Set());
	const [formOpen, setFormOpen] = useState(false);
	const [editing, setEditing] = useState<AssetDTO | null>(null);
	const [historyOpen, setHistoryOpen] = useState(false);
	const [historyAsset, setHistoryAsset] = useState<AssetDTO | null>(null);
	const [categoriesOpen, setCategoriesOpen] = useState(false);
	const [deleteTarget, setDeleteTarget] = useState<AssetDTO | null>(null);

	const createFromUrl = searchParams.get("new") === "1";
	const categoriesFromUrl = searchParams.get("categories") === "1";
	const isFormOpen = formOpen || createFromUrl;
	const isCategoriesOpen = categoriesOpen || categoriesFromUrl;

	const allAssets = assets.data ?? [];
	const totals = portfolioTotals(allAssets);
	const normalizedQuery = query.trim().toLowerCase();
	const filtered = allAssets.filter(asset => {
		const matchesType = filter === "ALL" || asset.type === filter;
		const matchesQuery =
			normalizedQuery.length === 0 ||
			asset.name.toLowerCase().includes(normalizedQuery) ||
			asset.categoryName.toLowerCase().includes(normalizedQuery);
		const matchesValue = !hideZero || assetValue(asset) !== 0;

		return matchesType && matchesQuery && matchesValue;
	});

	const setQuery = (value: string) => {
		const next = new URLSearchParams(searchParams);
		if (value) next.set("search", value);
		else next.delete("search");
		setSearchParams(next, { replace: true });
	};

	const clearParam = (key: string) => {
		if (!searchParams.has(key)) return;
		const next = new URLSearchParams(searchParams);
		next.delete(key);
		setSearchParams(next, { replace: true });
	};

	const openCreate = () => {
		setEditing(null);
		setFormOpen(true);
	};

	const closeForm = () => {
		setFormOpen(false);
		setEditing(null);
		clearParam("new");
	};

	const openEdit = (asset: AssetDTO) => {
		setEditing(asset);
		setFormOpen(true);
	};

	const openHistory = (asset: AssetDTO) => {
		setHistoryAsset(asset);
		setHistoryOpen(true);
	};

	const closeCategories = () => {
		setCategoriesOpen(false);
		clearParam("categories");
	};

	const toggleGroup = (category: string) => {
		setCollapsed(current => {
			const next = new Set(current);
			if (next.has(category)) next.delete(category);
			else next.add(category);
			return next;
		});
	};

	return (
		<div className="flex flex-col gap-5">
			<PageHeader
				title="Assets"
				description="Everything you own and owe, in one list."
				actions={
					<>
						<Button
							variant="outline"
							onClick={() => setCategoriesOpen(true)}
						>
							<Tag className="size-4" /> Categories
						</Button>
						<Button onClick={openCreate}>
							<Plus className="size-4" /> Add asset
						</Button>
					</>
				}
			/>

			<div className="bg-card border-border grid grid-cols-3 divide-x rounded-xl border">
				<div className="flex flex-col gap-0.5 p-4">
					<span className="text-muted-foreground flex items-center gap-1 text-[11px]">
						<TrendingUp className="size-3" /> Assets
					</span>
					<span className="num text-base font-semibold sm:text-lg">{money(totals.assetTotal)}</span>
				</div>
				<div className="flex flex-col gap-0.5 p-4">
					<span className="text-muted-foreground flex items-center gap-1 text-[11px]">
						<TrendingDown className="size-3" /> Liabilities
					</span>
					<span className="num text-base font-semibold sm:text-lg">
						{money(totals.liabilityTotal)}
					</span>
				</div>
				<div className="flex flex-col gap-0.5 p-4">
					<span className="text-muted-foreground flex items-center gap-1 text-[11px]">
						<Wallet className="size-3" /> Net
					</span>
					<span className="num text-base font-semibold sm:text-lg">{money(totals.netTotal)}</span>
				</div>
			</div>

			<div className="flex flex-wrap items-center gap-2">
				<div className="relative min-w-48 flex-1">
					<Search className="text-muted-foreground pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2" />
					<Input
						value={query}
						onChange={event => setQuery(event.target.value)}
						placeholder="Search assets or categories"
						className="pl-8"
						aria-label="Search assets"
					/>
				</div>
				<Tabs
					value={filter}
					onValueChange={value => setFilter(value as TypeFilter)}
				>
					<TabsList aria-label="Filter by type">
						{FILTERS.map(item => (
							<TabsTrigger
								key={item.value}
								value={item.value}
								aria-controls={undefined}
							>
								{item.label}
							</TabsTrigger>
						))}
					</TabsList>
				</Tabs>
				<div className="flex items-center gap-2">
					<Switch
						id="hide-zero"
						checked={hideZero}
						onCheckedChange={setHideZero}
					/>
					<Label
						htmlFor="hide-zero"
						className="text-muted-foreground text-xs font-normal"
					>
						Hide zero values
					</Label>
				</div>
			</div>

			{assets.isLoading ? (
				<div className="flex flex-col gap-2">
					<Skeleton className="h-12 w-full" />
					<Skeleton className="h-12 w-full" />
					<Skeleton className="h-12 w-full" />
				</div>
			) : allAssets.length === 0 ? (
				<EmptyState
					icon={<Wallet className="size-8" />}
					title="No assets yet"
					description="Add your first asset to start tracking your net worth."
					action={
						<Button
							size="sm"
							onClick={openCreate}
						>
							<Plus className="size-4" /> Add asset
						</Button>
					}
				/>
			) : filtered.length === 0 ? (
				<EmptyState
					icon={<Search className="size-8" />}
					title="No assets match"
					description="Try a different search or filter."
				/>
			) : (
				<AssetList
					assets={filtered}
					collapsed={collapsed}
					onToggle={toggleGroup}
					onEdit={openEdit}
					onHistory={openHistory}
					onDelete={setDeleteTarget}
					onRename={(asset, name) => {
						if (asset.id) renameAsset.mutate({ id: asset.id, name });
					}}
					onToggleFavorite={asset => {
						if (asset.id) toggleFavorite.mutate({ id: asset.id, isFavorite: !asset.isFavorite });
					}}
					onValueSave={(asset, value) => {
						if (asset.id) updateValue.mutate({ assetId: asset.id, value });
					}}
				/>
			)}

			<AssetFormSheet
				open={isFormOpen}
				onOpenChange={open => (open ? setFormOpen(true) : closeForm())}
				asset={editing}
				categories={categories.data ?? []}
			/>

			<SnapshotsSheet
				open={historyOpen}
				onOpenChange={open => {
					setHistoryOpen(open);
					if (!open) setHistoryAsset(null);
				}}
				asset={historyAsset}
			/>

			<CategoriesSheet
				open={isCategoriesOpen}
				onOpenChange={open => (open ? setCategoriesOpen(true) : closeCategories())}
			/>

			<AlertDialog
				open={Boolean(deleteTarget)}
				onOpenChange={open => {
					if (!open) setDeleteTarget(null);
				}}
			>
				<AlertDialogContent>
					<AlertDialogHeader>
						<AlertDialogTitle>Delete this asset?</AlertDialogTitle>
						<AlertDialogDescription>
							“{deleteTarget?.name}” and its entire history will be removed permanently. This cannot
							be undone.
						</AlertDialogDescription>
					</AlertDialogHeader>
					<AlertDialogFooter>
						<AlertDialogCancel>Cancel</AlertDialogCancel>
						<AlertDialogAction
							onClick={() => {
								if (!deleteTarget?.id) return;
								deleteAsset.mutate(deleteTarget.id, { onSuccess: () => setDeleteTarget(null) });
							}}
						>
							Delete forever
						</AlertDialogAction>
					</AlertDialogFooter>
				</AlertDialogContent>
			</AlertDialog>
		</div>
	);
}
