import { ChangeDetectionStrategy, Component, inject, signal } from "@angular/core";
import { RouterLink, RouterOutlet } from "@angular/router";
import { Menu } from "primeng/menu";
import { ButtonModule } from "primeng/button";
import { Avatar } from "primeng/avatar";
import { AuthService } from "@auth0/auth0-angular";
import { toSignal } from "@angular/core/rxjs-interop";
import { MenuItem } from "primeng/api";
import { Divider } from "primeng/divider";
@Component({
	selector: "app-root",
	standalone: true,
	imports: [RouterOutlet, Menu, ButtonModule, Avatar, Divider, RouterLink],
	templateUrl: "app.html",
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class App {
	private readonly authService = inject(AuthService);

	user = toSignal(this.authService.user$);
	sidebarMenuItems = signal<MenuItem[]>([
		{
			label: "Dashboard",
			icon: "pi pi-chart-line",
			routerLink: ["/dashboard"]
		}
	]);

	logout() {
		this.authService.logout();
	}
}
