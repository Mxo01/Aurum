import { computed, inject, Injectable, signal } from "@angular/core";
import { Router, NavigationEnd } from "@angular/router";
import { filter } from "rxjs/operators";

@Injectable({ providedIn: "root" })
export class NavigationService {
	private readonly router = inject(Router);

	private readonly previous = signal<string | null>(null);
	private current: string | null = null;

	readonly previousRoute = computed(() => this.previous() ?? "/dashboard");

	constructor() {
		this.router.events.pipe(filter(event => event instanceof NavigationEnd)).subscribe({
			next: (event: NavigationEnd) => {
				this.previous.set(this.current);
				this.current = event.urlAfterRedirects;
			}
		});
	}
}
