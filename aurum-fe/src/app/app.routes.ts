import { Routes } from "@angular/router";
import { authGuardFn } from "@auth0/auth0-angular";

export const routes: Routes = [
	{
		path: "dashboard",
		canActivate: [authGuardFn],
		loadComponent: () =>
			import("./domain/dashboard/dashboard.component").then(m => m.DashboardComponent)
	},
	{
		path: "profile",
		canActivate: [authGuardFn],
		loadComponent: () => import("./domain/profile/profile.component").then(m => m.ProfileComponent)
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
