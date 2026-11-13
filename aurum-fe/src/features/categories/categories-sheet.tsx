import { Lock, Pencil, Plus, Trash2 } from "lucide-react";
import { createElement, useState, type FormEvent } from "react";
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
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
	Sheet,
	SheetContent,
	SheetDescription,
	SheetHeader,
	SheetTitle
} from "@/components/ui/sheet";
import { Skeleton } from "@/components/ui/skeleton";
import type { AssetCategoryDTO, AssetType } from "@/lib/api/types";
import { ASSET_TYPE } from "@/lib/api/types";
import { cn } from "@/lib/utils";
import { CategoryIcon } from "./icon-map";
import { CATEGORY_ICON_OPTIONS } from "./icon-options";
import { useCategories, useCreateCategory, useDeleteCategory, useUpdateCategory } from "./api";

const NAME_MAX_LENGTH = 100;

interface CategoriesSheetProps {
	open: boolean;
	onOpenChange: (open: boolean) => void;
}

export function CategoriesSheet({ open, onOpenChange }: CategoriesSheetProps) {
	return (
		<Sheet
			open={open}
			onOpenChange={onOpenChange}
		>
			<SheetContent className="sm:max-w-lg">
				<SheetHeader>
					<SheetTitle>Categories</SheetTitle>
					<SheetDescription>
						Default categories are fixed. Custom categories can be renamed, re-iconed or deleted
						when empty.
					</SheetDescription>
				</SheetHeader>
				{open ? <CategoriesBody /> : null}
			</SheetContent>
		</Sheet>
	);
}

function CategoriesBody() {
	const categories = useCategories();
	const deleteCategory = useDeleteCategory();

	const [dialogOpen, setDialogOpen] = useState(false);
	const [editing, setEditing] = useState<AssetCategoryDTO | null>(null);

	const defaultCategories = (categories.data ?? []).filter(category => category.isDefault);
	const customCategories = (categories.data ?? []).filter(category => !category.isDefault);

	const openCreate = () => {
		setEditing(null);
		setDialogOpen(true);
	};

	const openEdit = (category: AssetCategoryDTO) => {
		setEditing(category);
		setDialogOpen(true);
	};

	return (
		<>
			<div className="flex justify-end">
				<Button
					size="sm"
					onClick={openCreate}
				>
					<Plus className="size-4" /> Add category
				</Button>
			</div>

			<div className="flex flex-1 flex-col gap-5 overflow-y-auto">
				<section className="flex flex-col gap-2">
					<span className="text-muted-foreground text-[11px] font-medium">Default</span>
					{categories.isLoading ? (
						<Skeleton className="h-10 w-full" />
					) : (
						<div className="grid grid-cols-2 gap-2">
							{defaultCategories.map(category => (
								<div
									key={category.id}
									className="border-border flex items-center gap-2 rounded-lg border px-3 py-2"
								>
									<CategoryIcon name={category.icon} />
									<span className="truncate text-sm">{category.name}</span>
									<Lock className="text-muted-foreground ml-auto size-3.5" />
								</div>
							))}
						</div>
					)}
				</section>

				<section className="flex flex-col gap-2">
					<span className="text-muted-foreground text-[11px] font-medium">Custom</span>
					{customCategories.length === 0 ? (
						<p className="text-muted-foreground text-sm">
							No custom categories yet. Add one to group assets your way.
						</p>
					) : (
						<div className="flex flex-col gap-1">
							{customCategories.map(category => (
								<div
									key={category.id}
									className="border-border flex items-center gap-2 rounded-lg border px-3 py-2"
								>
									<CategoryIcon name={category.icon} />
									<span className="min-w-0 flex-1 truncate text-sm">{category.name}</span>
									<Badge variant="outline">
										{category.type === ASSET_TYPE.LIABILITY ? "Liability" : "Asset"}
									</Badge>
									<Button
										variant="ghost"
										size="icon-sm"
										aria-label={`Edit ${category.name}`}
										onClick={() => openEdit(category)}
									>
										<Pencil className="size-3.5" />
									</Button>
									<AlertDialog>
										<AlertDialogTrigger asChild>
											<Button
												variant="ghost"
												size="icon-sm"
												className="hover:text-danger"
												aria-label={`Delete ${category.name}`}
											>
												<Trash2 className="size-3.5" />
											</Button>
										</AlertDialogTrigger>
										<AlertDialogContent>
											<AlertDialogHeader>
												<AlertDialogTitle>Delete “{category.name}”?</AlertDialogTitle>
												<AlertDialogDescription>
													Categories with assets cannot be deleted. Move those assets first.
												</AlertDialogDescription>
											</AlertDialogHeader>
											<AlertDialogFooter>
												<AlertDialogCancel>Cancel</AlertDialogCancel>
												<AlertDialogAction
													onClick={() => category.id && deleteCategory.mutate(category.id)}
												>
													Delete
												</AlertDialogAction>
											</AlertDialogFooter>
										</AlertDialogContent>
									</AlertDialog>
								</div>
							))}
						</div>
					)}
				</section>
			</div>

			<Dialog
				open={dialogOpen}
				onOpenChange={setDialogOpen}
			>
				<DialogContent>
					<DialogHeader>
						<DialogTitle>{editing ? "Edit category" : "New category"}</DialogTitle>
					</DialogHeader>
					<CategoryForm
						key={editing?.id ?? "new"}
						editing={editing}
						onDone={() => setDialogOpen(false)}
					/>
				</DialogContent>
			</Dialog>
		</>
	);
}

function CategoryForm({
	editing,
	onDone
}: {
	editing: AssetCategoryDTO | null;
	onDone: () => void;
}) {
	const createCategory = useCreateCategory();
	const updateCategory = useUpdateCategory();

	const [name, setName] = useState(editing?.name ?? "");
	const [type, setType] = useState<AssetType>(editing?.type ?? ASSET_TYPE.ASSET);
	const [icon, setIcon] = useState<string | null>(editing?.icon ?? null);
	const [error, setError] = useState<string | null>(null);

	const isSaving = createCategory.isPending || updateCategory.isPending;

	const submit = (event: FormEvent) => {
		event.preventDefault();
		const trimmed = name.trim();
		if (!trimmed) {
			setError("Name is required");
			return;
		}

		const body = { name: trimmed, type, icon: icon ?? undefined };
		const onSuccess = () => onDone();

		if (editing?.id) {
			updateCategory.mutate({ id: editing.id, body }, { onSuccess });
			return;
		}
		createCategory.mutate(body, { onSuccess });
	};

	return (
		<form
			onSubmit={submit}
			className="flex flex-col gap-4"
		>
			<div className="flex flex-col gap-2">
				<Label htmlFor="category-name">Name</Label>
				<Input
					id="category-name"
					value={name}
					onChange={event => {
						setName(event.target.value);
						setError(null);
					}}
					maxLength={NAME_MAX_LENGTH}
					placeholder="e.g. Stocks"
				/>
				{error ? <p className="text-danger text-xs">{error}</p> : null}
			</div>

			<div className="flex flex-col gap-2">
				<Label htmlFor="category-type">Type</Label>
				<select
					id="category-type"
					value={type}
					onChange={event => setType(event.target.value as AssetType)}
					className="border-input bg-background focus-visible:ring-ring h-9 rounded-md border px-3 text-sm focus-visible:ring-2 focus-visible:outline-none"
				>
					<option value={ASSET_TYPE.ASSET}>Asset</option>
					<option value={ASSET_TYPE.LIABILITY}>Liability</option>
				</select>
			</div>

			<div className="flex flex-col gap-2">
				<span className="text-sm font-medium">Icon</span>
				<div
					className="grid max-h-44 grid-cols-7 gap-1 overflow-y-auto p-0.5"
					role="group"
					aria-label="Category icon"
				>
					{CATEGORY_ICON_OPTIONS.map(option => (
						<button
							key={option.value}
							type="button"
							title={option.label}
							aria-label={option.label}
							aria-pressed={icon === option.value}
							onClick={() => setIcon(icon === option.value ? null : option.value)}
							className={cn(
								"focus-ring flex size-8 items-center justify-center rounded-md border transition-colors",
								icon === option.value
									? "border-primary bg-primary/10 text-foreground"
									: "border-transparent text-muted-foreground hover:bg-accent"
							)}
						>
							{createElement(option.icon, { className: "size-4" })}
						</button>
					))}
				</div>
			</div>

			<Button
				type="submit"
				disabled={isSaving}
				className="self-end"
			>
				{editing ? "Save changes" : "Create category"}
			</Button>
		</form>
	);
}
