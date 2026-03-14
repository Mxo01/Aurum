import { Routes } from "@angular/router";
import { authGuardFn } from "@auth0/auth0-angular";

export const routes: Routes = [
	{
		path: "dashboard",
		title: "Dashboard",
		canActivate: [authGuardFn],
		loadComponent: () =>
			import("./domain/dashboard/dashboard.component").then(m => m.DashboardComponent)
	},
	{
		path: "profile",
		title: "Profile",
		canActivate: [authGuardFn],
		loadComponent: () => import("./domain/profile/profile.component").then(m => m.ProfileComponent)
	},
	{
		path: "assets",
		title: "Assets",
		canActivate: [authGuardFn],
		loadComponent: () => import("./domain/asset/asset.component").then(m => m.AssetComponent)
	},
	{
		path: "",
		redirectTo: "dashboard",
		pathMatch: "full"
	},
	{
		path: "**",
		redirectTo: "dashboard"
	}
];

export const paths: Record<string, string> = routes.reduce(
	(acc, route) => {
		if (route.path === "**" || route.path === "profile" || !route.path) return acc;

		acc[`/${route.path}`] = route.title as string;

		return acc;
	},
	{} as Record<string, string>
);
