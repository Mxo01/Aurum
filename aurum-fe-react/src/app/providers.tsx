import { Auth0Provider, useAuth0 } from "@auth0/auth0-react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useEffect, useMemo, type ReactNode } from "react";
import { RouterProvider } from "react-router";
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
	return <Toaster theme={theme} position="top-right" closeButton richColors={false} />;
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
					const returnTo = appState?.returnTo;
					if (returnTo) window.history.replaceState({}, "", returnTo);
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
