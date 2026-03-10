import { Component } from "@angular/core";

@Component({
	selector: "app-dashboard",
	standalone: true,
	template: `
		<div class="p-6">
			<h2 class="text-2xl font-bold mb-4">Dashboard</h2>
			<p>This is a protected route. Only authenticated users can see this.</p>
			<div class="mt-4 p-4 bg-blue-50 border border-blue-200 rounded-lg">
				<p class="text-blue-700">Welcome to Aurum!</p>
			</div>
		</div>
	`
})
export class DashboardComponent {}
