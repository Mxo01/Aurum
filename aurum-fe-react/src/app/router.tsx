import { createBrowserRouter, Navigate } from "react-router";
import { PlaceholderPage } from "./shell/placeholder-page";
import { ProtectedLayout } from "./shell/protected-layout";

export const router = createBrowserRouter([
	{
		path: "/",
		lazy: async () => {
			const module = await import("@/features/landing/page");
			return { Component: module.LandingPage };
		}
	},
	{
		path: "/privacy",
		lazy: async () => {
			const module = await import("@/features/legal/privacy-policy-page");
			return { Component: module.PrivacyPolicyPage };
		}
	},
	{
		element: <ProtectedLayout />,
		children: [
			{
				path: "dashboard",
				element: <PlaceholderPage title="Dashboard" description="Net worth, targets and top assets." />
			},
			{
				path: "assets",
				element: <PlaceholderPage title="Assets" description="Assets, liabilities and categories." />
			},
			{
				path: "assets/categories",
				element: <Navigate to="/assets?categories=1" replace />
			},
			{
				path: "cashflow",
				element: <PlaceholderPage title="Cash flow" description="Earned, spent and savings rate." />
			},
			{
				path: "profile",
				element: <PlaceholderPage title="Profile" description="Preferences, integrations and data." />
			}
		]
	},
	{ path: "*", element: <Navigate to="/dashboard" replace /> }
]);
