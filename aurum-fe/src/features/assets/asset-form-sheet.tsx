import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect } from "react";
import { Controller, useForm, useWatch } from "react-hook-form";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { DatePicker } from "@/components/ui/date-picker";
import { CategoryIcon } from "@/features/categories/icon-map";
import { FavoriteToggle } from "./favorite-toggle";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
	Select,
	SelectContent,
	SelectGroup,
	SelectItem,
	SelectLabel,
	SelectTrigger,
	SelectValue
} from "@/components/ui/select";
import {
	Sheet,
	SheetContent,
	SheetDescription,
	SheetFooter,
	SheetHeader,
	SheetTitle
} from "@/components/ui/sheet";
import { todayISO } from "@/lib/format/date";
import {
	ASSET_TYPE,
	CURRENCIES,
	LIABILITY_TYPE,
	PAYMENT_FREQUENCY,
	type AssetCategoryDTO,
	type AssetDTO,
	type Currency
} from "@/lib/api/types";
import { useCreateAsset, useUpdateAsset } from "./api";

const NAME_MAX_LENGTH = 100;

const assetSchema = z
	.object({
		name: z
			.string()
			.trim()
			.min(1, "Name is required")
			.max(NAME_MAX_LENGTH, "Maximum 100 characters"),
		categoryId: z.string().min(1, "Category is required"),
		currency: z.custom<Currency>(value => CURRENCIES.includes(value as Currency), {
			message: "Select a currency"
		}),
		isFavorite: z.boolean(),
		initialValue: z.number().min(0, "Cannot be negative"),
		referenceDate: z.string().min(1, "Pick a date"),
		liabilityType: z.enum(["MANUAL", "AUTOMATIC"]).nullable(),
		paymentFrequency: z.enum(["WEEKLY", "MONTHLY", "YEARLY"]).nullable(),
		paymentAmount: z.number().min(0, "Cannot be negative").nullable()
	})
	.superRefine((values, context) => {
		if (values.liabilityType !== LIABILITY_TYPE.AUTOMATIC) return;

		if (!values.paymentFrequency) {
			context.addIssue({
				code: "custom",
				path: ["paymentFrequency"],
				message: "Select a frequency"
			});
		}
		if (!values.paymentAmount || values.paymentAmount <= 0) {
			context.addIssue({
				code: "custom",
				path: ["paymentAmount"],
				message: "Enter the payment amount"
			});
		}
	});

type AssetFormValues = z.infer<typeof assetSchema>;

function emptyValues(): AssetFormValues {
	return {
		name: "",
		categoryId: "",
		currency: "EUR",
		isFavorite: false,
		initialValue: 0,
		referenceDate: todayISO(),
		liabilityType: null,
		paymentFrequency: null,
		paymentAmount: null
	};
}

function valuesFrom(asset: AssetDTO | null): AssetFormValues {
	if (!asset) return emptyValues();

	return {
		name: asset.name,
		categoryId: asset.categoryId,
		currency: asset.originalCurrency,
		isFavorite: asset.isFavorite,
		initialValue: 0,
		referenceDate: todayISO(),
		liabilityType: asset.liabilityType ?? null,
		paymentFrequency: asset.paymentFrequency ?? null,
		paymentAmount: asset.paymentAmount ?? null
	};
}

interface AssetFormSheetProps {
	open: boolean;
	onOpenChange: (open: boolean) => void;
	asset: AssetDTO | null;
	categories: AssetCategoryDTO[];
}

export function AssetFormSheet({ open, onOpenChange, asset, categories }: AssetFormSheetProps) {
	const createAsset = useCreateAsset();
	const updateAsset = useUpdateAsset();

	const form = useForm<AssetFormValues>({
		resolver: zodResolver(assetSchema),
		mode: "onChange",
		defaultValues: emptyValues()
	});

	const categoryId = useWatch({ control: form.control, name: "categoryId" });
	const liabilityType = useWatch({ control: form.control, name: "liabilityType" });
	const isFavorite = useWatch({ control: form.control, name: "isFavorite" });
	const category = categories.find(item => item.id === categoryId);
	const type = category?.type ?? ASSET_TYPE.ASSET;
	const isEditing = Boolean(asset?.id);
	const isSaving = createAsset.isPending || updateAsset.isPending;

	useEffect(() => {
		if (open) form.reset(valuesFrom(asset));
	}, [open, asset, form]);

	const submit = form.handleSubmit(values => {
		const selectedCategory = categories.find(item => item.id === values.categoryId);
		const isLiability = selectedCategory?.type === ASSET_TYPE.LIABILITY;

		const body = {
			name: values.name.trim(),
			categoryId: values.categoryId,
			originalCurrency: values.currency,
			isFavorite: isLiability ? false : values.isFavorite,
			liabilityType: isLiability ? (values.liabilityType ?? LIABILITY_TYPE.MANUAL) : undefined,
			paymentFrequency:
				isLiability && values.liabilityType === LIABILITY_TYPE.AUTOMATIC
					? values.paymentFrequency!
					: undefined,
			paymentAmount:
				isLiability && values.liabilityType === LIABILITY_TYPE.AUTOMATIC
					? values.paymentAmount!
					: undefined
		};

		const onSuccess = () => onOpenChange(false);

		if (asset?.id) {
			updateAsset.mutate({ id: asset.id, body }, { onSuccess });
			return;
		}

		createAsset.mutate(
			{
				...body,
				initialValue: values.initialValue,
				referenceDate: values.referenceDate
			},
			{ onSuccess }
		);
	});

	const assets = categories.filter(item => item.type === ASSET_TYPE.ASSET);
	const liabilities = categories.filter(item => item.type === ASSET_TYPE.LIABILITY);

	return (
		<Sheet
			open={open}
			onOpenChange={onOpenChange}
		>
			<SheetContent>
				<SheetHeader>
					<SheetTitle>{isEditing ? "Edit asset" : "New asset"}</SheetTitle>
					<SheetDescription>
						{isEditing
							? "Update the asset details. Values are recorded from the history sheet."
							: "Add the asset and, if you know it, its current value."}
					</SheetDescription>
				</SheetHeader>

				<form
					onSubmit={submit}
					className="flex flex-1 flex-col gap-4 overflow-y-auto overscroll-contain px-1.5 pt-1 pb-1.5"
				>
					<div className="flex flex-col gap-2">
						<Label htmlFor="asset-name">Name</Label>
						<Input
							id="asset-name"
							maxLength={NAME_MAX_LENGTH}
							{...form.register("name")}
						/>
						{form.formState.errors.name ? (
							<p className="text-danger text-xs">{form.formState.errors.name.message}</p>
						) : null}
					</div>

					<div className="flex flex-col gap-2">
						<Label htmlFor="asset-category">Category</Label>
						<Controller
							control={form.control}
							name="categoryId"
							render={({ field }) => (
								<Select
									value={field.value}
									onValueChange={field.onChange}
								>
									<SelectTrigger
										id="asset-category"
										aria-label="Category"
										onBlur={field.onBlur}
										ref={field.ref}
									>
										<SelectValue placeholder="Select a category">
											{category ? (
												<span className="flex items-center gap-1.5">
													<CategoryIcon name={category.icon} />
													{category.name}
												</span>
											) : undefined}
										</SelectValue>
									</SelectTrigger>
									<SelectContent>
										{assets.length ? (
											<SelectGroup>
												<SelectLabel>Assets</SelectLabel>
												{assets.map(item => (
													<SelectItem
														key={item.id}
														value={item.id}
													>
														<span className="flex items-center gap-1.5">
															<CategoryIcon name={item.icon} />
															{item.name}
														</span>
													</SelectItem>
												))}
											</SelectGroup>
										) : null}
										{liabilities.length ? (
											<SelectGroup>
												<SelectLabel>Liabilities</SelectLabel>
												{liabilities.map(item => (
													<SelectItem
														key={item.id}
														value={item.id}
													>
														<span className="flex items-center gap-1.5">
															<CategoryIcon name={item.icon} />
															{item.name}
														</span>
													</SelectItem>
												))}
											</SelectGroup>
										) : null}
									</SelectContent>
								</Select>
							)}
						/>
						{form.formState.errors.categoryId ? (
							<p className="text-danger text-xs">{form.formState.errors.categoryId.message}</p>
						) : null}
					</div>

					<div className="flex flex-col gap-2">
						<Label htmlFor="asset-currency">Currency</Label>
						<Controller
							control={form.control}
							name="currency"
							render={({ field }) => (
								<Select
									value={field.value}
									onValueChange={value => field.onChange(value as Currency)}
									disabled={isEditing}
								>
									<SelectTrigger
										id="asset-currency"
										aria-label="Currency"
										onBlur={field.onBlur}
										ref={field.ref}
									>
										<SelectValue />
									</SelectTrigger>
									<SelectContent>
										{CURRENCIES.map(code => (
											<SelectItem
												key={code}
												value={code}
											>
												{code}
											</SelectItem>
										))}
									</SelectContent>
								</Select>
							)}
						/>
					</div>

					{type === ASSET_TYPE.LIABILITY ? (
						<>
							<div className="flex flex-col gap-2">
								<Label htmlFor="asset-liability-type">Liability type</Label>
								<Controller
									control={form.control}
									name="liabilityType"
									render={({ field }) => (
										<Select
											value={field.value ?? undefined}
											onValueChange={value => field.onChange(value)}
										>
											<SelectTrigger
												id="asset-liability-type"
												aria-label="Liability type"
												onBlur={field.onBlur}
												ref={field.ref}
											>
												<SelectValue placeholder="Select a type" />
											</SelectTrigger>
											<SelectContent>
												<SelectItem value={LIABILITY_TYPE.MANUAL}>Manual</SelectItem>
												<SelectItem value={LIABILITY_TYPE.AUTOMATIC}>Automatic</SelectItem>
											</SelectContent>
										</Select>
									)}
								/>
							</div>

							{liabilityType === LIABILITY_TYPE.AUTOMATIC ? (
								<>
									<div className="flex flex-col gap-2">
										<Label htmlFor="asset-frequency">Payment frequency</Label>
										<Controller
											control={form.control}
											name="paymentFrequency"
											render={({ field }) => (
												<Select
													value={field.value ?? undefined}
													onValueChange={value => field.onChange(value)}
												>
													<SelectTrigger
														id="asset-frequency"
														aria-label="Payment frequency"
														onBlur={field.onBlur}
														ref={field.ref}
													>
														<SelectValue placeholder="Select a frequency" />
													</SelectTrigger>
													<SelectContent>
														<SelectItem value={PAYMENT_FREQUENCY.WEEKLY}>Weekly</SelectItem>
														<SelectItem value={PAYMENT_FREQUENCY.MONTHLY}>Monthly</SelectItem>
														<SelectItem value={PAYMENT_FREQUENCY.YEARLY}>Yearly</SelectItem>
													</SelectContent>
												</Select>
											)}
										/>
									</div>
									<div className="flex flex-col gap-2">
										<Label htmlFor="asset-payment">Payment amount</Label>
										<Input
											id="asset-payment"
											type="number"
											step="0.01"
											min={0}
											{...form.register("paymentAmount", { valueAsNumber: true })}
										/>
									</div>
								</>
							) : null}
						</>
					) : (
						<div className="flex items-center justify-between gap-3 rounded-lg border p-3">
							<div className="flex flex-col">
								<Label>Mark as favorite</Label>
								<span className="text-muted-foreground text-[11px]">
									Favorite assets are plotted on the dashboard&apos;s net worth chart.
								</span>
							</div>
							<FavoriteToggle
								pressed={isFavorite}
								onPressedChange={checked => form.setValue("isFavorite", checked)}
								label="Mark as favorite"
							/>
						</div>
					)}

					{!isEditing ? (
						<>
							<div className="flex flex-col gap-2">
								<Label htmlFor="asset-initial">Initial value</Label>
								<Input
									id="asset-initial"
									type="number"
									step="0.01"
									min={0}
									{...form.register("initialValue", { valueAsNumber: true })}
								/>
							</div>
							<div className="flex flex-col gap-2">
								<Label htmlFor="asset-date">Value date</Label>
								<Controller
									control={form.control}
									name="referenceDate"
									render={({ field }) => (
										<DatePicker
											id="asset-date"
											value={field.value}
											onChange={field.onChange}
											label="Value date"
											max={todayISO()}
											toYear={new Date().getFullYear()}
										/>
									)}
								/>
							</div>
						</>
					) : null}

					<SheetFooter>
						<Button
							type="button"
							variant="outline"
							onClick={() => onOpenChange(false)}
						>
							Cancel
						</Button>
						<Button
							type="submit"
							disabled={isSaving || !form.formState.isValid}
						>
							{isEditing ? "Save changes" : "Add asset"}
						</Button>
					</SheetFooter>
				</form>
			</SheetContent>
		</Sheet>
	);
}
