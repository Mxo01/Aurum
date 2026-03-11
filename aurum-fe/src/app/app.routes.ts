import { Routes } from "@angular/router";
import { authGuardFn } from "@auth0/auth0-angular";

export const routes: Routes = [
	{
		path: "dashboard",
		canActivate: [authGuardFn],
		loadComponent: () =>
			import("./features/dashboard/dashboard.component").then(m => m.DashboardComponent)
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
