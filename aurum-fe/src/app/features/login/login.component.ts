import { Component, inject } from "@angular/core";
import { AuthService } from "@auth0/auth0-angular";

@Component({
	selector: "app-login",
	standalone: true,
	template: `
		<div class="flex flex-col items-center justify-center min-h-[60vh]">
			<div
				class="p-8 bg-white shadow-xl rounded-2xl border border-gray-100 max-w-md w-full text-center"
			>
				<h2 class="text-3xl font-extrabold text-gray-900 mb-6">Welcome back</h2>
				<p class="text-gray-600 mb-8">Please sign in with Auth0 to access your dashboard.</p>

				<button
					(click)="login()"
					class="w-full py-3 px-4 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-lg transition duration-200 flex items-center justify-center gap-2"
				>
					<span>Sign In with Auth0</span>
				</button>
			</div>
		</div>
	`
})
export class LoginComponent {
	private readonly authService = inject(AuthService);

	login() {
		this.authService.loginWithRedirect();
	}
}
