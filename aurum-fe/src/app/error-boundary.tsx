import { AlertTriangle } from "lucide-react";
import { Component, type ErrorInfo, type ReactNode } from "react";
import { Button } from "@/components/ui/button";

interface ErrorBoundaryProps {
	children: ReactNode;
}

interface ErrorBoundaryState {
	error: Error | null;
}

export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
	state: ErrorBoundaryState = { error: null };

	static getDerivedStateFromError(error: Error): ErrorBoundaryState {
		return { error };
	}

	componentDidCatch(_error: Error, _info: ErrorInfo) {
		// The boundary is the user-facing fallback; no logging sink is configured.
	}

	private readonly retry = () => {
		this.setState({ error: null });
	};

	render() {
		const { error } = this.state;

		if (error) {
			return (
				<div className="bg-background flex min-h-dvh flex-col items-center justify-center gap-4 p-6 text-center">
					<div className="bg-danger/10 text-danger flex size-12 items-center justify-center rounded-full">
						<AlertTriangle className="size-6" />
					</div>
					<div className="flex max-w-md flex-col gap-1">
						<h1 className="text-lg font-semibold">Something went wrong</h1>
						<p className="text-muted-foreground text-sm">
							{error.message || "An unexpected error interrupted the page."}
						</p>
					</div>
					<Button onClick={this.retry}>Try again</Button>
					<a
						href="/"
						className="text-muted-foreground hover:text-foreground text-xs underline underline-offset-2"
					>
						Back to Aurum
					</a>
				</div>
			);
		}

		return this.props.children;
	}
}
