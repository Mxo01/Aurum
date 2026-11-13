import {
	ChevronDown,
	ChevronRight,
	History,
	LineChart,
	MoreHorizontal,
	Pencil,
	Trash2
} from "lucide-react";
import { Delta } from "@/components/app/delta";
import { EditableText } from "@/components/app/editable-text";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Toggle } from "@/components/ui/toggle";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import {
	DropdownMenu,
	DropdownMenuContent,
	DropdownMenuItem,
	DropdownMenuSeparator,
	DropdownMenuTrigger
} from "@/components/ui/dropdown-menu";
import { CategoryIcon } from "@/features/categories/icon-map";
import type { AssetDTO } from "@/lib/api/types";
import { ASSET_TYPE } from "@/lib/api/types";
import { assetTrend } from "@/lib/format/asset";
import { usePreferences } from "@/lib/format/use-preferences";
import { cn } from "@/lib/utils";
import { groupAssets } from "./assets.utils";
import { EditableValue } from "@/components/app/editable-value";

interface AssetListProps {
	assets: AssetDTO[];
	collapsed: Set<string>;
	onToggle: (category: string) => void;
	onEdit: (asset: AssetDTO) => void;
	onHistory: (asset: AssetDTO) => void;
	onDelete: (asset: AssetDTO) => void;
	onRename: (asset: AssetDTO, name: string) => void;
	onToggleFavorite: (asset: AssetDTO) => void;
	onValueSave: (asset: AssetDTO, value: number) => void;
}

function RowActions({
	asset,
	onEdit,
	onHistory,
	onDelete
}: Pick<AssetListProps, "onEdit" | "onHistory" | "onDelete"> & { asset: AssetDTO }) {
	return (
		<div className="flex w-full items-center justify-end">
			<DropdownMenu>
				<DropdownMenuTrigger asChild>
					<Button
						variant="ghost"
						size="icon-sm"
						aria-label={`Actions for ${asset.name}`}
					>
						<MoreHorizontal className="size-4" />
					</Button>
				</DropdownMenuTrigger>
				<DropdownMenuContent align="end">
					<DropdownMenuItem onSelect={() => onEdit(asset)}>
						<Pencil className="size-4" /> Edit
					</DropdownMenuItem>
					<DropdownMenuItem onSelect={() => onHistory(asset)}>
						<History className="size-4" /> History
					</DropdownMenuItem>
					<DropdownMenuSeparator />
					<DropdownMenuItem
						className="text-danger focus:text-danger"
						onSelect={() => onDelete(asset)}
					>
						<Trash2 className="size-4" /> Delete
					</DropdownMenuItem>
				</DropdownMenuContent>
			</DropdownMenu>
		</div>
	);
}

export function AssetList({
	assets,
	collapsed,
	onToggle,
	onEdit,
	onHistory,
	onDelete,
	onRename,
	onToggleFavorite,
	onValueSave
}: AssetListProps) {
	const { money } = usePreferences();
	const groups = groupAssets(assets);

	return (
		<>
			<div className="border-border hidden overflow-hidden rounded-xl border md:block">
				<div className="bg-muted/40 text-muted-foreground grid grid-cols-[minmax(0,1fr)_92px_160px_120px_100px] gap-2 px-3 py-2 text-[11px] font-medium">
					<span>Asset</span>
					<span>Type</span>
					<span className="text-right">Value</span>
					<span className="text-right">Monthly Delta</span>
					<span className="text-right">Actions</span>
				</div>

				{groups.map(group => {
					const isCollapsed = collapsed.has(group.name);

					return (
						<div
							key={group.name}
							className="border-border border-t first:border-t-0"
						>
							<button
								type="button"
								onClick={() => onToggle(group.name)}
								aria-expanded={!isCollapsed}
								className="hover:bg-muted/40 focus-ring flex w-full items-center gap-2 px-3 py-2.5 text-left transition-colors"
							>
								{isCollapsed ? (
									<ChevronRight className="text-muted-foreground size-4" />
								) : (
									<ChevronDown className="text-muted-foreground size-4" />
								)}
								<CategoryIcon name={group.icon} />
								<span className="text-sm font-semibold">{group.name}</span>
								<Badge variant="outline">{group.assets.length}</Badge>
								{group.type === ASSET_TYPE.LIABILITY ? (
									<Badge variant="outline">Liability</Badge>
								) : null}
								<span className="num text-muted-foreground ml-auto text-xs">
									{money(group.total)}
								</span>
							</button>

							{!isCollapsed ? (
								<div>
									{group.assets.map(asset => {
										const trend = assetTrend(asset);
										const isLiability = asset.type === ASSET_TYPE.LIABILITY;

										return (
											<div
												key={asset.id}
												className="border-border/60 hover:bg-muted/30 grid grid-cols-[minmax(0,1fr)_92px_160px_120px_100px] items-center gap-2 border-t px-3 py-2 transition-colors"
											>
												<div className="flex min-w-0 items-center gap-2 pl-6">
													{asset.type === ASSET_TYPE.ASSET ? (
														<Tooltip>
															<TooltipTrigger asChild>
																<Toggle
																	pressed={asset.isFavorite}
																	onPressedChange={() => onToggleFavorite(asset)}
																	variant="outline"
																	size="sm"
																	aria-label={`Dashboard chart for ${asset.name}`}
																	className="aria-pressed:border-primary aria-pressed:bg-primary aria-pressed:text-primary-foreground shrink-0 gap-1 px-2 text-xs"
																>
																	<LineChart className="size-3.5" />
																	Chart
																</Toggle>
															</TooltipTrigger>
															<TooltipContent>
																{asset.isFavorite
																	? "Shown in the dashboard chart"
																	: "Not shown in the dashboard chart"}
															</TooltipContent>
														</Tooltip>
													) : null}
													<EditableText
														value={asset.name}
														label={asset.name}
														onSave={name => onRename(asset, name)}
														className="min-w-0"
													/>
												</div>
												<div>
													<Badge
														variant="outline"
														className="text-muted-foreground font-normal"
													>
														{isLiability ? "Liability" : "Asset"}
													</Badge>
												</div>
												<div className="text-right">
													<EditableValue
														value={asset.latestValue ?? 0}
														currency={asset.originalCurrency}
														label={asset.name}
														onSave={value => onValueSave(asset, value)}
													/>
												</div>
												<div className="text-right">
													{trend === null ? (
														<span className="text-muted-foreground text-[11px]">No data</span>
													) : (
														<Delta
															value={trend}
															invert={isLiability}
														/>
													)}
												</div>
												<RowActions
													asset={asset}
													onEdit={onEdit}
													onHistory={onHistory}
													onDelete={onDelete}
												/>
											</div>
										);
									})}
								</div>
							) : null}
						</div>
					);
				})}
			</div>

			<div className="flex flex-col gap-3 md:hidden">
				{groups.map(group => {
					const isCollapsed = collapsed.has(group.name);

					return (
						<div
							key={group.name}
							className="border-border overflow-hidden rounded-xl border"
						>
							<button
								type="button"
								onClick={() => onToggle(group.name)}
								aria-expanded={!isCollapsed}
								className="focus-ring flex w-full items-center gap-2 px-3 py-3 text-left"
							>
								{isCollapsed ? (
									<ChevronRight className="text-muted-foreground size-4" />
								) : (
									<ChevronDown className="text-muted-foreground size-4" />
								)}
								<CategoryIcon name={group.icon} />
								<span className="text-sm font-semibold">{group.name}</span>
								<Badge variant="outline">{group.assets.length}</Badge>
								<span className="num text-muted-foreground ml-auto text-xs">
									{money(group.total)}
								</span>
							</button>

							{!isCollapsed
								? group.assets.map(asset => {
										const trend = assetTrend(asset);
										const isLiability = asset.type === ASSET_TYPE.LIABILITY;

										return (
											<div
												key={asset.id}
												className={cn("border-border/60 flex flex-col gap-2 border-t px-3 py-3")}
											>
												<div className="flex items-start justify-between gap-2">
													<div className="flex min-w-0 items-center gap-1.5">
														{asset.type === ASSET_TYPE.ASSET ? (
															<Toggle
																pressed={asset.isFavorite}
																onPressedChange={() => onToggleFavorite(asset)}
																variant="outline"
																size="sm"
																aria-label={`Dashboard chart for ${asset.name}`}
																className="aria-pressed:border-primary aria-pressed:bg-primary aria-pressed:text-primary-foreground gap-1 px-2 text-[11px]"
															>
																<LineChart className="size-3.5" />
																Chart
															</Toggle>
														) : null}
														<EditableText
															value={asset.name}
															label={asset.name}
															onSave={name => onRename(asset, name)}
															className="min-w-0"
														/>
													</div>
													{trend === null ? (
														<span className="text-muted-foreground text-[11px]">No data</span>
													) : (
														<Delta
															value={trend}
															invert={isLiability}
														/>
													)}
												</div>
												<div className="flex items-center justify-between">
													<EditableValue
														value={asset.latestValue ?? 0}
														currency={asset.originalCurrency}
														label={asset.name}
														onSave={value => onValueSave(asset, value)}
														className="text-sm"
													/>
													<RowActions
														asset={asset}
														onEdit={onEdit}
														onHistory={onHistory}
														onDelete={onDelete}
													/>
												</div>
											</div>
										);
									})
								: null}
						</div>
					);
				})}
			</div>
		</>
	);
}
