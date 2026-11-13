import { createBrowserRouter, Navigate } from "react-router";
import { RootLayout } from "./root-layout";
import { RouteError } from "./route-error";
import { ProtectedLayout } from "./shell/protected-layout";

export const router = createBrowserRouter([
	{
		element: <RootLayout />,
		errorElement: <RouteError />,
		children: [
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
						lazy: async () => {
							const module = await import("@/features/dashboard/page");
							return { Component: module.DashboardPage };
						}
					},
					{
						path: "assets",
						lazy: async () => {
							const module = await import("@/features/assets/page");
							return { Component: module.AssetsPage };
						}
					},
					{
						path: "assets/categories",
						element: (
							<Navigate
								to="/assets?categories=1"
								replace
							/>
						)
					},
					{
						path: "cashflow",
						lazy: async () => {
							const module = await import("@/features/cashflow/page");
							return { Component: module.CashFlowPage };
						}
					},
					{
						path: "profile",
						lazy: async () => {
							const module = await import("@/features/profile/page");
							return { Component: module.ProfilePage };
						}
					}
				]
			},
			{
				path: "*",
				element: (
					<Navigate
						to="/dashboard"
						replace
					/>
				)
			}
		]
	}
]);
