import { inject, Injectable } from "@angular/core";
import { Router, NavigationEnd } from "@angular/router";
import { filter } from "rxjs/operators";

@Injectable({ providedIn: "root" })
export class NavigationService {
	private readonly router = inject(Router);

	private history: string[] = [];

	constructor() {
		this.router.events.pipe(filter(event => event instanceof NavigationEnd)).subscribe({
			next: (event: NavigationEnd) => this.history.push(event.urlAfterRedirects)
		});
	}

	get previousRoute(): string {
		return this.history.length ? this.history[this.history.length - 1] : "/dashboard";
	}
}
