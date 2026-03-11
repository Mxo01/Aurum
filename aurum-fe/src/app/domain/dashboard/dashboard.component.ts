import { HttpClient } from "@angular/common/http";
import { Component, inject, OnInit } from "@angular/core";
import { AuthService } from "@auth0/auth0-angular";
import { Button } from "primeng/button";

@Component({
	selector: "app-dashboard",
	standalone: true,
	imports: [Button],
	templateUrl: "./dashboard.component.html"
})
export class DashboardComponent implements OnInit {
	authService = inject(AuthService);
	http = inject(HttpClient);

	ngOnInit() {
		this.http.get("http://localhost:8080/api/assets").subscribe(res => {
			console.log(res);
		});
	}

	logout() {
		this.authService.logout();
	}
}
