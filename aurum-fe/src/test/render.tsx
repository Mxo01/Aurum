import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, type RenderResult } from "@testing-library/react";
import { useState, type ReactElement, type ReactNode } from "react";
import { MemoryRouter } from "react-router";
import { TooltipProvider } from "@/components/ui/tooltip";
import { ApiProvider } from "@/lib/api/api-context";

export function createTestQueryClient() {
	return new QueryClient({
		defaultOptions: {
			queries: { retry: false, gcTime: 0 },
			mutations: { retry: false }
		}
	});
}

interface TestProvidersProps {
	children: ReactNode;
	queryClient?: QueryClient;
	route?: string;
	withRouter?: boolean;
}

export function TestProviders({
	children,
	queryClient,
	route = "/",
	withRouter = true
}: TestProvidersProps) {
	const [fallbackClient] = useState(createTestQueryClient);
	const client = queryClient ?? fallbackClient;
	const content = (
		<QueryClientProvider client={client}>
			<ApiProvider>
				<TooltipProvider>{children}</TooltipProvider>
			</ApiProvider>
		</QueryClientProvider>
	);

	return withRouter ? <MemoryRouter initialEntries={[route]}>{content}</MemoryRouter> : content;
}

interface RenderWithProvidersOptions {
	queryClient?: QueryClient;
	route?: string;
	withRouter?: boolean;
}

export function renderWithProviders(
	ui: ReactElement,
	{ queryClient, route, withRouter }: RenderWithProvidersOptions = {}
): RenderResult {
	return render(ui, {
		wrapper: ({ children }) => (
			<TestProviders
				queryClient={queryClient}
				route={route}
				withRouter={withRouter}
			>
				{children}
			</TestProviders>
		)
	});
}
