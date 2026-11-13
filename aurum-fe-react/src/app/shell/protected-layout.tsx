import { useAuth0 } from "@auth0/auth0-react";
import { useEffect } from "react";
import { useLocation } from "react-router";
import { AppShell } from "./app-shell";
import { Splash } from "./splash";

export function ProtectedLayout() {
	const { isLoading, isAuthenticated, loginWithRedirect } = useAuth0();
	const location = useLocation();

	useEffect(() => {
		if (isLoading || isAuthenticated) return;

		void loginWithRedirect({
			appState: { returnTo: `${location.pathname}${location.search}` }
		});
	}, [isLoading, isAuthenticated, loginWithRedirect, location.pathname, location.search]);

	if (isLoading || !isAuthenticated) return <Splash />;

	return <AppShell />;
}
