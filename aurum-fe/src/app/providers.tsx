import { Auth0Provider, useAuth0 } from "@auth0/auth0-react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useEffect, useMemo, type ReactNode } from "react";
import { RouterProvider } from "react-router";
import { CircleCheck, CircleX, Info, TriangleAlert } from "lucide-react";
import { Toaster } from "sonner";
import { ApiProvider } from "@/lib/api/api-context";
import { registerUnauthorizedHandler } from "@/lib/api/client";
import { env } from "@/lib/env";
import { useThemeStore } from "@/stores/theme";
import { TooltipProvider } from "@/components/ui/tooltip";
import { ErrorBoundary } from "./error-boundary";
import { router } from "./router";

const queryClient = new QueryClient({
	defaultOptions: {
		queries: {
			staleTime: 30_000,
			retry: 1,
			refetchOnWindowFocus: false
		},
		mutations: {
			retry: 0
		}
	}
});

interface AppState {
	returnTo?: string;
}

function ApiBridge({ children }: { children: ReactNode }) {
	const { getAccessTokenSilently, isAuthenticated, logout } = useAuth0();

	useEffect(() => {
		registerUnauthorizedHandler(() => {
			void logout({ logoutParams: { returnTo: window.location.origin } });
		});
		return () => registerUnauthorizedHandler(() => undefined);
	}, [logout]);

	const getToken = useMemo(
		() => (isAuthenticated ? () => getAccessTokenSilently() : undefined),
		[isAuthenticated, getAccessTokenSilently]
	);

	return <ApiProvider getToken={getToken}>{children}</ApiProvider>;
}

function AppToaster() {
	const theme = useThemeStore(state => state.theme);

	return (
		<Toaster
			theme={theme}
			position="top-right"
			closeButton={false}
			icons={{
				success: <CircleCheck className="text-success size-4" />,
				error: <CircleX className="text-danger size-4" />,
				warning: <TriangleAlert className="text-primary size-4" />,
				info: <Info className="text-foreground size-4" />
			}}
			toastOptions={{
				classNames: {
					toast: "border bg-card text-card-foreground",
					success: "border-success/40",
					error: "border-danger/40",
					warning: "border-primary/50",
					info: "border-border"
				}
			}}
		/>
	);
}

export function Providers() {
	return (
		<QueryClientProvider client={queryClient}>
			<Auth0Provider
				domain={env.auth0Domain}
				clientId={env.auth0ClientId}
				cacheLocation="localstorage"
				useRefreshTokens
				useRefreshTokensFallback
				authorizationParams={{
					redirect_uri: window.location.origin,
					audience: env.auth0Audience
				}}
				onRedirectCallback={(appState: AppState | undefined) => {
					// A full reload lets the data router pick up the callback URL;
					// replaceState alone leaves the router on the landing route.
					window.location.replace(appState?.returnTo ?? "/dashboard");
				}}
			>
				<ApiBridge>
					<TooltipProvider delayDuration={200}>
						<ErrorBoundary>
							<RouterProvider router={router} />
						</ErrorBoundary>
						<AppToaster />
					</TooltipProvider>
				</ApiBridge>
			</Auth0Provider>
		</QueryClientProvider>
	);
}
