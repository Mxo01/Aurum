import { zodResolver } from "@hookform/resolvers/zod";
import { MoreHorizontal, Pencil, Plus, Target as TargetIcon, Trash2 } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { Controller, useForm, useWatch } from "react-hook-form";
import { useSearchParams } from "react-router";
import { z } from "zod";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { DatePicker } from "@/components/ui/date-picker";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
	Select,
	SelectContent,
	SelectItem,
	SelectTrigger,
	SelectValue
} from "@/components/ui/select";
import { Progress } from "@/components/ui/progress";
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
import {
	DropdownMenu,
	DropdownMenuContent,
	DropdownMenuItem,
	DropdownMenuTrigger
} from "@/components/ui/dropdown-menu";
import {
	Sheet,
	SheetContent,
	SheetDescription,
	SheetFooter,
	SheetHeader,
	SheetTitle
} from "@/components/ui/sheet";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";
import { tomorrowISO } from "@/lib/format/date";
import { usePreferences } from "@/lib/format/use-preferences";
import type { TargetDTO, TargetStatus } from "@/lib/api/types";
import { useCreateTarget, useDeleteTarget, useUpdateTarget } from "@/features/targets/api";

const targetSchema = z.object({
	name: z.string().min(1, "Name is required").max(100, "Maximum 100 characters"),
	type: z.enum(["NET_WORTH", "MANUAL"]),
	targetAmount: z.number().min(1, "Enter an amount greater than zero"),
	currentAmount: z.number().min(0, "Cannot be negative"),
	deadline: z.string().min(1, "Pick a deadline")
});

type TargetFormValues = z.infer<typeof targetSchema>;

const STATUS_LABEL: Record<TargetStatus, string> = {
	COMPLETED: "Completed",
	IN_PROGRESS: "In progress",
	NOT_REACHED: "Not reached"
};

function emptyValues(): TargetFormValues {
	return {
		name: "",
		type: "MANUAL",
		targetAmount: 0,
		currentAmount: 0,
		deadline: tomorrowISO()
	};
}

interface TargetsCardProps {
	targets: TargetDTO[] | undefined;
	isLoading: boolean;
	currency: string;
}

export function TargetsCard({ targets, isLoading, currency }: TargetsCardProps) {
	const { money, date } = usePreferences();
	const [formOpen, setFormOpen] = useState(false);
	const [listOpen, setListOpen] = useState(false);
	const [editing, setEditing] = useState<TargetDTO | null>(null);
	const listRef = useRef<HTMLDivElement>(null);
	const [isClipped, setIsClipped] = useState(false);
	const [searchParams, setSearchParams] = useSearchParams();
	const createFromUrl = searchParams.get("target") === "new";
	const isFormOpen = formOpen || createFromUrl;

	const createTarget = useCreateTarget();
	const updateTarget = useUpdateTarget();
	const deleteTarget = useDeleteTarget();

	const form = useForm<TargetFormValues>({
		resolver: zodResolver(targetSchema),
		mode: "onChange",
		defaultValues: emptyValues()
	});

	const active = targets?.[0];

	useEffect(() => {
		const element = listRef.current;
		if (!element) return;
		const update = () => setIsClipped(element.scrollHeight > element.clientHeight + 1);
		update();
		if (typeof ResizeObserver === "undefined") return;
		const observer = new ResizeObserver(update);
		observer.observe(element);
		return () => observer.disconnect();
	}, [targets]);

	useEffect(() => {
		form.reset(
			editing
				? {
						name: editing.name,
						type: editing.type,
						targetAmount: editing.targetAmount,
						currentAmount: editing.currentAmount ?? 0,
						deadline: editing.deadline
					}
				: emptyValues()
		);
	}, [editing, form]);

	const clearCreateParam = () => {
		if (!createFromUrl) return;
		const next = new URLSearchParams(searchParams);
		next.delete("target");
		setSearchParams(next, { replace: true });
	};

	const closeForm = () => {
		setFormOpen(false);
		setEditing(null);
		form.reset(emptyValues());
		clearCreateParam();
	};

	const onFormOpenChange = (open: boolean) => {
		if (open) setFormOpen(true);
		else closeForm();
	};

	const openCreate = () => {
		setEditing(null);
		setFormOpen(true);
	};

	const openEdit = (target: TargetDTO) => {
		setEditing(target);
		setFormOpen(true);
	};

	const submit = form.handleSubmit(values => {
		const body = {
			name: values.name,
			type: values.type,
			targetAmount: values.targetAmount,
			currentAmount: values.type === "MANUAL" ? values.currentAmount : undefined,
			deadline: values.deadline
		};

		const onSuccess = () => closeForm();

		if (editing?.id) {
			updateTarget.mutate({ id: editing.id, body }, { onSuccess });
		} else {
			createTarget.mutate(body, { onSuccess });
		}
	});

	const type = useWatch({ control: form.control, name: "type" });
	const isSaving = createTarget.isPending || updateTarget.isPending;

	return (
		<section className="bg-card border-border relative flex h-full max-h-[32rem] flex-col overflow-hidden rounded-xl border p-4 sm:p-5 lg:max-h-none">
			<div className="flex items-center justify-between gap-2">
				<span className="text-muted-foreground flex items-center gap-2 text-[11px] font-medium">
					<TargetIcon className="size-3.5" /> Financial target
				</span>
				<DropdownMenu>
					<DropdownMenuTrigger asChild>
						<Button
							variant="ghost"
							size="icon-sm"
							aria-label="Target actions"
						>
							<MoreHorizontal className="size-4" />
						</Button>
					</DropdownMenuTrigger>
					<DropdownMenuContent align="end">
						{active ? (
							<DropdownMenuItem onSelect={() => openEdit(active)}>
								<Pencil className="size-4" /> Edit target
							</DropdownMenuItem>
						) : null}
						<DropdownMenuItem onSelect={openCreate}>
							<Plus className="size-4" /> Add target
						</DropdownMenuItem>
						<DropdownMenuItem onSelect={() => setListOpen(true)}>
							<TargetIcon className="size-4" /> View all targets
						</DropdownMenuItem>
					</DropdownMenuContent>
				</DropdownMenu>
			</div>

			{isLoading ? (
				<div className="mt-5 flex flex-col gap-3">
					<Skeleton className="h-5 w-32" />
					<Skeleton className="h-2 w-full" />
					<Skeleton className="h-4 w-40" />
				</div>
			) : targets?.length ? (
				<div className="relative mt-4 flex min-h-0 flex-1 flex-col">
					<div
						ref={listRef}
						className="flex min-h-0 flex-1 flex-col gap-3 lg:absolute lg:inset-0 lg:overflow-hidden"
					>
						{targets.map((target, index) => {
							const completion = Math.max(0, Math.min(100, target.completionPercentage ?? 0));
							const remaining = Math.max(target.targetAmount - (target.currentAmount ?? 0), 0);

							return (
								<div
									key={target.id ?? target.name}
									className={cn("flex shrink-0 flex-col gap-1.5", index > 0 && "border-t pt-3")}
								>
									<div className="flex items-start justify-between gap-2">
										<div className="min-w-0">
											<p className="truncate text-sm font-semibold">{target.name}</p>
											<p className="text-muted-foreground num mt-0.5 text-[11px]">
												{money(target.currentAmount ?? 0, { digits: 0 })} /{" "}
												{money(target.targetAmount, { digits: 0 })}
											</p>
										</div>
										<Badge
											variant={
												target.status === "COMPLETED"
													? "success"
													: target.status === "NOT_REACHED"
														? "danger"
														: "outline"
											}
										>
											{target.status ? STATUS_LABEL[target.status] : "In progress"}
										</Badge>
									</div>

									<Progress
										value={completion}
										label={`${target.name} progress`}
									/>
									<div className="text-muted-foreground flex justify-between text-[11px]">
										<span className="num">{completion.toFixed(0)}%</span>
										<span className="num">{money(remaining, { digits: 0 })} to go</span>
									</div>
								</div>
							);
						})}
					</div>
				</div>
			) : (
				<div className="mt-5 flex flex-1 flex-col items-center justify-center gap-3 py-6 text-center">
					<TargetIcon className="text-muted-foreground/60 size-7" />
					<div>
						<p className="text-sm font-medium">No target yet</p>
						<p className="text-muted-foreground mt-0.5 text-[11px]">Set a number worth reaching.</p>
					</div>
					<Button
						size="sm"
						variant="outline"
						onClick={openCreate}
					>
						<Plus className="size-4" /> Set a target
					</Button>
				</div>
			)}

			{isClipped ? (
				<>
					<div className="pointer-events-none absolute inset-x-0 bottom-0 h-48 bg-gradient-to-t from-black/25 via-black/10 to-transparent dark:from-black/70 dark:via-black/30" />
					<div className="absolute inset-x-0 bottom-0 flex justify-center pb-4">
						<Button
							size="sm"
							variant="secondary"
							className="shadow-lg"
							onClick={() => setListOpen(true)}
						>
							View all
						</Button>
					</div>
				</>
			) : null}

			<Sheet
				open={isFormOpen}
				onOpenChange={onFormOpenChange}
			>
				<SheetContent>
					<SheetHeader>
						<SheetTitle>{editing ? "Edit target" : "New target"}</SheetTitle>
						<SheetDescription>
							Targets track your progress towards a number, in {currency}.
						</SheetDescription>
					</SheetHeader>

					<form
						onSubmit={submit}
						className="flex flex-1 flex-col gap-4 overflow-y-auto"
					>
						<div className="flex flex-col gap-2">
							<Label htmlFor="target-type">Type</Label>
							<Controller
								control={form.control}
								name="type"
								render={({ field }) => (
									<Select
										value={field.value}
										onValueChange={field.onChange}
										disabled={Boolean(editing)}
									>
										<SelectTrigger
											id="target-type"
											aria-label="Type"
										>
											<SelectValue />
										</SelectTrigger>
										<SelectContent>
											<SelectItem value="MANUAL">Manual</SelectItem>
											<SelectItem value="NET_WORTH">Net worth</SelectItem>
										</SelectContent>
									</Select>
								)}
							/>
						</div>

						<div className="flex flex-col gap-2">
							<Label htmlFor="target-name">Name</Label>
							<Input
								id="target-name"
								placeholder="e.g. New car"
								maxLength={100}
								{...form.register("name")}
							/>
							{form.formState.errors.name ? (
								<p className="text-danger text-xs">{form.formState.errors.name.message}</p>
							) : null}
						</div>

						<div className="flex flex-col gap-2">
							<Label htmlFor="target-amount">Target amount</Label>
							<Input
								id="target-amount"
								type="number"
								min={0}
								step="0.01"
								{...form.register("targetAmount", { valueAsNumber: true })}
							/>
							{form.formState.errors.targetAmount ? (
								<p className="text-danger text-xs">{form.formState.errors.targetAmount.message}</p>
							) : null}
						</div>

						{type === "MANUAL" ? (
							<div className="flex flex-col gap-2">
								<Label htmlFor="target-current">Current saved amount</Label>
								<Input
									id="target-current"
									type="number"
									min={0}
									step="0.01"
									{...form.register("currentAmount", { valueAsNumber: true })}
								/>
								{form.formState.errors.currentAmount ? (
									<p className="text-danger text-xs">
										{form.formState.errors.currentAmount.message}
									</p>
								) : null}
							</div>
						) : null}

						<div className="flex flex-col gap-2">
							<Label htmlFor="target-deadline">Deadline</Label>
							<Controller
								control={form.control}
								name="deadline"
								render={({ field }) => (
									<DatePicker
										id="target-deadline"
										value={field.value}
										onChange={field.onChange}
										label="Deadline"
										min={tomorrowISO()}
										toYear={new Date().getFullYear() + 30}
									/>
								)}
							/>
							{form.formState.errors.deadline ? (
								<p className="text-danger text-xs">{form.formState.errors.deadline.message}</p>
							) : null}
						</div>

						<SheetFooter>
							<Button
								type="submit"
								disabled={isSaving || !form.formState.isValid}
							>
								{editing ? "Save changes" : "Create target"}
							</Button>
						</SheetFooter>
					</form>
				</SheetContent>
			</Sheet>

			<Sheet
				open={listOpen}
				onOpenChange={setListOpen}
			>
				<SheetContent>
					<SheetHeader>
						<SheetTitle>All targets</SheetTitle>
						<SheetDescription>Every target in one place.</SheetDescription>
					</SheetHeader>

					<div className="flex flex-1 flex-col gap-2 overflow-y-auto">
						{targets?.length ? (
							targets.map(target => (
								<div
									key={target.id ?? target.name}
									className="border-border flex flex-col gap-2 rounded-lg border p-3"
								>
									<div className="flex items-start justify-between gap-2">
										<div className="min-w-0">
											<p className="truncate text-sm font-medium">{target.name}</p>
											<p className="text-muted-foreground num text-[11px]">
												{money(target.currentAmount ?? 0, { digits: 0 })} /{" "}
												{money(target.targetAmount, { digits: 0 })}
											</p>
										</div>
										<Badge
											variant={
												target.status === "COMPLETED"
													? "success"
													: target.status === "NOT_REACHED"
														? "danger"
														: "outline"
											}
										>
											{target.status ? STATUS_LABEL[target.status] : "In progress"}
										</Badge>
									</div>
									<Progress
										value={target.completionPercentage ?? 0}
										label={`${target.name} progress`}
									/>
									<div className="flex items-center justify-between">
										<span className="text-muted-foreground text-[11px]">
											{date(target.deadline)}
										</span>
										<div className="flex gap-1">
											<Button
												variant="ghost"
												size="icon-sm"
												aria-label={`Edit ${target.name}`}
												onClick={() => {
													setListOpen(false);
													openEdit(target);
												}}
											>
												<Pencil className="size-4" />
											</Button>
											<DeleteTargetButton
												target={target}
												onConfirm={() => target.id && deleteTarget.mutate(target.id)}
											/>
										</div>
									</div>
								</div>
							))
						) : (
							<p className="text-muted-foreground py-8 text-center text-sm">No targets yet.</p>
						)}
					</div>

					<SheetFooter>
						<Button
							variant="outline"
							onClick={() => {
								setListOpen(false);
								openCreate();
							}}
						>
							<Plus className="size-4" /> Add target
						</Button>
					</SheetFooter>
				</SheetContent>
			</Sheet>
		</section>
	);
}

function DeleteTargetButton({ target, onConfirm }: { target: TargetDTO; onConfirm: () => void }) {
	return (
		<AlertDialog>
			<AlertDialogTrigger asChild>
				<Button
					variant="ghost"
					size="icon-sm"
					className="text-muted-foreground hover:text-danger"
					aria-label={`Delete ${target.name}`}
				>
					<Trash2 className="size-4" />
				</Button>
			</AlertDialogTrigger>
			<AlertDialogContent>
				<AlertDialogHeader>
					<AlertDialogTitle>Delete this target?</AlertDialogTitle>
					<AlertDialogDescription>
						“{target.name}” will be removed permanently. This cannot be undone.
					</AlertDialogDescription>
				</AlertDialogHeader>
				<AlertDialogFooter>
					<AlertDialogCancel>Cancel</AlertDialogCancel>
					<AlertDialogAction onClick={onConfirm}>Delete</AlertDialogAction>
				</AlertDialogFooter>
			</AlertDialogContent>
		</AlertDialog>
	);
}
