import { Routes } from "@angular/router";
import { authGuardFn } from "@auth0/auth0-angular";

interface RouteInfo {
	label: string;
	icon: string;
}

export const routes: Routes = [
	{
		path: "dashboard",
		data: {
			label: "Dashboard",
			icon: "pi pi-objects-column"
		},
		canActivate: [authGuardFn],
		loadComponent: () =>
			import("./domain/dashboard/dashboard.component").then(m => m.DashboardComponent)
	},
	{
		path: "profile",
		data: {
			label: "Profile",
			icon: "pi pi-user"
		},
		canActivate: [authGuardFn],
		loadComponent: () => import("./domain/profile/profile.component").then(m => m.ProfileComponent)
	},
	{
		path: "assets",
		data: {
			label: "Assets",
			icon: "pi pi-wallet"
		},
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

export const paths: Record<string, RouteInfo> = routes.reduce(
	(acc, route) => {
		if (route.path === "**" || !route.path) return acc;

		acc[`/${route.path}`] = {
			label: route.data?.["label"] as string,
			icon: route.data?.["icon"] as string
		};

		return acc;
	},
	{} as Record<string, RouteInfo>
);
