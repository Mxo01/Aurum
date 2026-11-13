import { cva, type VariantProps } from "class-variance-authority";
import { Check, Copy } from "lucide-react";
import { useEffect, useState, type ComponentProps } from "react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

const codeBlockVariants = cva(
	"group/code-block relative flex w-full min-w-0 flex-col overflow-hidden rounded-lg text-sm data-[size=sm]:text-xs",
	{
		variants: {
			variant: {
				default: "border-input text-foreground dark:bg-input/30 border",
				destructive: "border-destructive/30 text-destructive dark:border-destructive/40 border"
			}
		},
		defaultVariants: {
			variant: "default"
		}
	}
);

export function CodeBlock({
	className,
	variant = "default",
	size = "default",
	...props
}: ComponentProps<"div"> & VariantProps<typeof codeBlockVariants> & { size?: "default" | "sm" }) {
	return (
		<div
			data-slot="code-block"
			data-variant={variant}
			data-size={size}
			className={cn(codeBlockVariants({ variant }), className)}
			{...props}
		/>
	);
}

export function CodeBlockHeader({ className, ...props }: ComponentProps<"div">) {
	return (
		<div
			data-slot="code-block-header"
			className={cn(
				"text-muted-foreground flex min-w-0 shrink-0 items-center gap-2 px-4 pt-4 group-data-[size=sm]/code-block:px-3 group-data-[size=sm]/code-block:pt-3",
				className
			)}
			{...props}
		/>
	);
}

export function CodeBlockTitle({ className, ...props }: ComponentProps<"div">) {
	return (
		<div
			data-slot="code-block-title"
			className={cn(
				"text-foreground min-w-0 truncate font-sans text-sm font-medium [&_svg]:pointer-events-none [&_svg]:inline-block [&_svg]:align-middle [&_svg:not([class*='size-'])]:size-4 group-data-[size=sm]/code-block:text-xs group-data-[size=sm]/code-block:[&_svg:not([class*='size-'])]:size-3.5",
				className
			)}
			{...props}
		/>
	);
}

export function CodeBlockActions({ className, ...props }: ComponentProps<"div">) {
	return (
		<div
			data-slot="code-block-actions"
			className={cn(
				"ms-auto flex shrink-0 items-center gap-1",
				"[[data-slot=code-block]>&]:absolute [[data-slot=code-block]>&]:end-3 [[data-slot=code-block]>&]:top-3 [[data-slot=code-block]>&]:z-10 [[data-slot=code-block]>&]:opacity-0 [[data-slot=code-block]>&]:transition-opacity [[data-slot=code-block]>&]:group-focus-within/code-block:opacity-100 [[data-slot=code-block]>&]:group-hover/code-block:opacity-100 [[data-slot=code-block]>&]:pointer-coarse:opacity-100",
				"[[data-slot=code-block]>&]:*:[button]:bg-background [[data-slot=code-block]>&]:*:[button]:ring-1 [[data-slot=code-block]>&]:*:[button]:ring-foreground/10",
				className
			)}
			{...props}
		/>
	);
}

export function CodeBlockCopyButton({
	value,
	variant = "ghost",
	size = "icon-xs",
	className,
	onClick,
	...props
}: ComponentProps<typeof Button> & { value: string }) {
	const [copied, setCopied] = useState(false);

	useEffect(() => {
		if (!copied) return;
		const timeout = window.setTimeout(() => setCopied(false), 2000);
		return () => window.clearTimeout(timeout);
	}, [copied]);

	return (
		<>
			<Button
				data-slot="code-block-copy-button"
				data-copied={copied || undefined}
				variant={variant}
				size={size}
				aria-label={copied ? "Copied" : "Copy"}
				className={className}
				onClick={event => {
					onClick?.(event);
					navigator.clipboard
						.writeText(value)
						.then(() => setCopied(true))
						.catch(() => {});
				}}
				{...props}
			>
				{copied ? <Check /> : <Copy />}
			</Button>
			<span
				aria-live="polite"
				className="sr-only"
			>
				{copied ? "Copied" : ""}
			</span>
		</>
	);
}

export function CodeBlockContent({
	className,
	children,
	style,
	wrap = false,
	lineNumbers = false,
	startLine = 1,
	...props
}: ComponentProps<"pre"> & {
	wrap?: boolean;
	lineNumbers?: boolean;
	startLine?: number;
}) {
	return (
		<pre
			data-slot="code-block-content"
			data-wrap={wrap || undefined}
			data-numbered={lineNumbers || undefined}
			dir="ltr"
			// Focusable so keyboard users can scroll long lines.
			// eslint-disable-next-line jsx-a11y/no-noninteractive-tabindex
			tabIndex={0}
			style={{ counterReset: `line ${startLine - 1}`, ...style }}
			className={cn(
				"min-h-0 min-w-0 overflow-auto overscroll-contain px-4 py-4 text-start font-mono leading-relaxed outline-none",
				"group-has-data-[slot=code-block-header]/code-block:pt-2 group-has-data-[slot=code-block-footer]/code-block:pb-2 group-data-[size=sm]/code-block:px-3 group-data-[size=sm]/code-block:py-3",
				"data-[wrap]:whitespace-pre-wrap data-[wrap]:break-words",
				className
			)}
			{...props}
		>
			<code
				data-slot="code-block-code"
				className="grid min-h-[1lh] min-w-max content-start [[data-numbered]_&]:grid-cols-[auto_1fr] [[data-wrap]_&]:min-w-0"
			>
				{lineNumbers && typeof children === "string"
					? children
							.replace(/\n$/, "")
							.split("\n")
							.map((line, index) => <CodeBlockLine key={index}>{line}</CodeBlockLine>)
					: children}
			</code>
		</pre>
	);
}

const codeBlockLineVariants = cva(
	"relative block min-h-[1lh] [[data-numbered]_&]:col-span-2 [[data-numbered]_&]:grid [[data-numbered]_&]:grid-cols-subgrid [[data-numbered]_&]:[counter-increment:line] [[data-numbered]_&]:before:min-w-6 [[data-numbered]_&]:before:text-end [[data-numbered]_&]:before:text-muted-foreground [[data-numbered]_&]:before:content-[counter(line)]",
	{
		variants: {
			variant: {
				default: "",
				added: "bg-muted after:content-['+']",
				removed: "bg-destructive/10 after:text-destructive after:content-['−']",
				highlighted: "bg-muted"
			}
		},
		defaultVariants: {
			variant: "default"
		}
	}
);

export function CodeBlockLine({
	className,
	variant = "default",
	children,
	...props
}: ComponentProps<"span"> & VariantProps<typeof codeBlockLineVariants>) {
	// An empty line holds a newline so copying a selection keeps blank lines.
	const empty =
		children === undefined ||
		children === null ||
		children === "" ||
		(Array.isArray(children) && children.length === 0);

	return (
		<span
			data-slot="code-block-line"
			data-variant={variant}
			className={cn(codeBlockLineVariants({ variant }), className)}
			{...props}
		>
			<span
				data-slot="code-block-line-content"
				className="min-w-0"
			>
				{empty ? "\n" : children}
			</span>
		</span>
	);
}

export function CodeBlockFooter({ className, ...props }: ComponentProps<"div">) {
	return (
		<div
			data-slot="code-block-footer"
			className={cn(
				"text-muted-foreground flex min-w-0 shrink-0 items-center gap-2 px-4 pb-4 group-data-[size=sm]/code-block:px-3 group-data-[size=sm]/code-block:pb-3",
				className
			)}
			{...props}
		/>
	);
}
