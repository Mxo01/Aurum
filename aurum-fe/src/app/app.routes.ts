import { Routes } from "@angular/router";
import { authGuardFn } from "@auth0/auth0-angular";

interface RouteInfo {
	label: string;
	icon: string;
}

export const routes: Routes = [
	{
		path: "",
		pathMatch: "full",
		loadComponent: () => import("./domain/landing/landing.component").then(m => m.LandingComponent)
	},
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
		children: [
			{
				path: "",
				loadComponent: () => import("./domain/asset/asset.component").then(m => m.AssetComponent)
			},
			{
				path: "categories",
				data: {
					label: "Categories",
					icon: "pi pi-tags"
				},
				loadComponent: () =>
					import("./domain/asset/components/categories/categories.component").then(
						m => m.CategoriesComponent
					)
			}
		]
	},
	{
		path: "privacy",
		data: {
			label: "Privacy Policy",
			icon: "pi pi-shield"
		},
		loadComponent: () =>
			import("./domain/legal/privacy-policy/privacy-policy.component").then(
				m => m.PrivacyPolicyComponent
			)
	},
	{
		path: "**",
		redirectTo: "dashboard"
	}
];

export const paths: Record<string, RouteInfo> = routes.reduce(
	(acc, route) => {
		if (route.path === "**" || !route.path || !route.data?.["label"]) return acc;

		acc[`/${route.path}`] = {
			label: route.data["label"] as string,
			icon: route.data["icon"] as string
		};

		route.children?.forEach(child => {
			if (!child.path || !child.data?.["label"]) return;
			acc[`/${route.path}/${child.path}`] = {
				label: child.data["label"] as string,
				icon: child.data["icon"] as string
			};
		});

		return acc;
	},
	{} as Record<string, RouteInfo>
);
