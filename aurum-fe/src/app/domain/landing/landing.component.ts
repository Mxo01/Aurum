import {
	AfterViewInit,
	ChangeDetectionStrategy,
	Component,
	DestroyRef,
	ElementRef,
	inject,
	OnInit,
	signal,
	ViewChild,
	NgZone
} from "@angular/core";
import { Router, RouterLink } from "@angular/router";
import { AuthService } from "@auth0/auth0-angular";
import { Meta, Title } from "@angular/platform-browser";
import { Button } from "primeng/button";
import { ThemeService } from "../../shared/services/theme/theme.service";
import { filter, switchMap, take } from "rxjs";
import { takeUntilDestroyed, toSignal } from "@angular/core/rxjs-interop";

@Component({
	selector: "app-landing",
	standalone: true,
	imports: [RouterLink, Button],
	templateUrl: "./landing.component.html",
	styleUrl: "./landing.component.scss",
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class LandingComponent implements OnInit, AfterViewInit {
	private readonly authService = inject(AuthService);
	private readonly router = inject(Router);
	private readonly titleService = inject(Title);
	private readonly metaService = inject(Meta);
	private readonly destroyRef = inject(DestroyRef);
	private readonly hostRef = inject(ElementRef);
	private readonly ngZone = inject(NgZone);
	readonly themeService = inject(ThemeService);

	readonly isLoading = toSignal(this.authService.isLoading$, { initialValue: true });

	readonly dashboardFeatures = [
		"Net worth over time — interactive charts",
		"Asset growth vs. 1 year ago",
		"Top assets ranked by value & variation"
	];

	readonly currencies = ["EUR", "USD", "GBP", "CHF", "JPY"];

	// Animated stat values
	readonly stat100 = signal("0%");
	readonly stat170 = signal("0+");
	readonly statInf = signal("0");

	// Hero dashboard animated values
	readonly heroNetWorth = signal("€0");
	readonly heroTotalAssets = signal("€0");
	readonly heroGrowth = signal("+0.00%");
	readonly heroSavingsRate = signal("0.0%");

	@ViewChild("statsSection") private statsSection!: ElementRef<HTMLElement>;
	private statsAnimated = false;

	ngOnInit() {
		this.titleService.setTitle("Aurum");
		this.metaService.updateTag({
			name: "description",
			content:
				"Track your assets, monitor net worth over time, set financial targets, and get multi-currency analytics — all in one place."
		});
		this.metaService.updateTag({
			property: "og:title",
			content: "Aurum"
		});
		this.metaService.updateTag({
			property: "og:url",
			content: "https://aurum-networth.com/"
		});

		this.authService.isLoading$
			.pipe(
				filter(isLoading => !isLoading),
				take(1),
				switchMap(() => this.authService.isAuthenticated$.pipe(take(1))),
				takeUntilDestroyed(this.destroyRef)
			)
			.subscribe({
				next: isAuthenticated => {
					if (isAuthenticated) this.router.navigate(["/dashboard"], { replaceUrl: true });
				}
			});
	}

	ngAfterViewInit() {
		// Hero dashboard count-up — always visible, start immediately
		this.runHeroDashboardAnimation();

		if (typeof IntersectionObserver === "undefined") {
			this.runStatsAnimation();
			return;
		}

		// Stats count-up
		const statsObserver = new IntersectionObserver(
			entries => {
				if (entries[0].isIntersecting && !this.statsAnimated) {
					this.statsAnimated = true;
					this.runStatsAnimation();
					statsObserver.disconnect();
				}
			},
			{ threshold: 0.4 }
		);
		statsObserver.observe(this.statsSection.nativeElement);

		// Scroll reveal — run outside Angular zone for performance
		this.ngZone.runOutsideAngular(() => this.setupScrollReveal());
	}

	login() {
		this.authService.loginWithRedirect({ appState: { target: "/dashboard" } });
	}

	private runHeroDashboardAnimation() {
		// Delay slightly so the fade-up animation plays first
		setTimeout(() => {
			this.countUp(124580, v => this.heroNetWorth.set(`€${v.toLocaleString("en")}`), 1800);
			this.countUp(138400, v => this.heroTotalAssets.set(`€${v.toLocaleString("en")}`), 1800);
			this.countUpDecimal(86.81, v => this.heroGrowth.set(`+${v.toFixed(2)}%`), 1800);
			this.countUpDecimal(35.2, v => this.heroSavingsRate.set(`${v.toFixed(1)}%`), 1800);
		}, 600);
	}

	private runStatsAnimation() {
		this.countUp(100, v => this.stat100.set(`${v}%`), 1800);
		this.countUp(170, v => this.stat170.set(`${v}+`), 2000);
		setTimeout(() => this.statInf.set("∞"), 300);
	}

	private countUp(target: number, update: (v: number) => void, duration: number) {
		const start = performance.now();
		const tick = (now: number) => {
			const progress = Math.min((now - start) / duration, 1);
			const eased = 1 - Math.pow(1 - progress, 4);
			update(Math.round(eased * target));
			if (progress < 1) requestAnimationFrame(tick);
		};
		requestAnimationFrame(tick);
	}

	private countUpDecimal(target: number, update: (v: number) => void, duration: number) {
		const start = performance.now();
		const tick = (now: number) => {
			const progress = Math.min((now - start) / duration, 1);
			const eased = 1 - Math.pow(1 - progress, 4);
			update(eased * target);
			if (progress < 1) requestAnimationFrame(tick);
		};
		requestAnimationFrame(tick);
	}

	private setupScrollReveal() {
		const observer = new IntersectionObserver(
			entries => {
				entries.forEach(entry => {
					if (!entry.isIntersecting) return;
					const el = entry.target as HTMLElement;
					const delay = el.dataset["delay"] ?? "0";
					el.style.transitionDelay = `${delay}ms`;
					el.classList.add("is-revealed");
					observer.unobserve(el);
				});
			},
			{ threshold: 0.12, rootMargin: "0px 0px -60px 0px" }
		);

		this.hostRef.nativeElement
			.querySelectorAll("[data-reveal]")
			.forEach((el: Element) => observer.observe(el));
	}
}
