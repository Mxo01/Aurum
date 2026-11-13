import { AlertTriangle } from "lucide-react";
import { isRouteErrorResponse, Link, useRouteError } from "react-router";
import { Button } from "@/components/ui/button";

export function RouteError() {
	const error = useRouteError();

	const message = isRouteErrorResponse(error)
		? `${error.status} ${error.statusText}`
		: error instanceof Error
			? error.message
			: "An unexpected error interrupted the page.";

	return (
		<div className="bg-background flex min-h-dvh flex-col items-center justify-center gap-4 p-6 text-center">
			<div className="bg-danger/10 text-danger flex size-12 items-center justify-center rounded-full">
				<AlertTriangle className="size-6" />
			</div>
			<div className="flex max-w-md flex-col gap-1">
				<h1 className="text-lg font-semibold">This page could not load</h1>
				<p className="text-muted-foreground text-sm">{message}</p>
			</div>
			<Button asChild>
				<Link to="/dashboard">Back to dashboard</Link>
			</Button>
		</div>
	);
}
