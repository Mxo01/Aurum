import {
	ChangeDetectionStrategy,
	Component,
	computed,
	DestroyRef,
	DOCUMENT,
	inject,
	NgZone,
	OnInit
} from "@angular/core";
import { RouterOutlet, RouterLinkActive, RouterLink, Router } from "@angular/router";
import { ButtonDirective, Button } from "primeng/button";
import { Avatar } from "primeng/avatar";
import { AuthService } from "@auth0/auth0-angular";
import { takeUntilDestroyed, toSignal } from "@angular/core/rxjs-interop";
import { fromEvent } from "rxjs";
import { Toolbar } from "primeng/toolbar";
import { darkModeSelector } from "./app.utils";
import { ThemeService } from "./shared/services/theme/theme.service";
import { PrivacyService } from "./shared/services/privacy/privacy.service";
import { ConfirmDialog } from "primeng/confirmdialog";
import { Toast } from "primeng/toast";
import { paths } from "./app.routes";
import { ProfileService } from "./domain/profile/profile.service";
import { GdprConsentComponent } from "./domain/legal/gdpr-consent/gdpr-consent.component";
import { MenuItem } from "primeng/api";
import { Menu } from "primeng/menu";

@Component({
	selector: "app-root",
	standalone: true,
	imports: [
		RouterOutlet,
		ButtonDirective,
		Avatar,
		Toolbar,
		RouterLink,
		RouterLinkActive,
		ConfirmDialog,
		Toast,
		GdprConsentComponent,
		Menu,
		Button
	],
	templateUrl: "app.component.html",
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class App implements OnInit {
	private readonly authService = inject(AuthService);
	private readonly themeService = inject(ThemeService);
	private readonly profileService = inject(ProfileService);
	private readonly privacyService = inject(PrivacyService);
	private readonly document = inject(DOCUMENT);
	private readonly router = inject(Router);
	private readonly destroyRef = inject(DestroyRef);
	private readonly ngZone = inject(NgZone);

	readonly user = toSignal(this.authService.user$);
	readonly paths = Object.entries(paths).filter(
		([path]) => path !== "/profile" && path !== "/assets/categories" && path !== "/privacy"
	);
	readonly isPrivacyMode = computed(() => this.privacyService.isPrivacyMode());
	readonly profileMenuItems = computed<MenuItem[]>(() => [
		{
			label: "Profile",
			icon: "pi pi-user",
			command: () => this.onProfileClick()
		},
		{
			label: "Toggle Privacy Mode",
			icon: this.isPrivacyMode() ? "pi pi-eye-slash" : "pi pi-eye",
			command: () => this.togglePrivacy()
		},
		{
			label: "Toggle Theme",
			icon: this.themeService.isDarkMode() ? "pi pi-moon" : "pi pi-sun",
			command: () => this.themeService.toggleTheme()
		},
		{ separator: true },
		{
			label: "Logout",
			labelClass: "text-red-400",
			icon: "pi pi-sign-out",
			iconClass: "text-red-400!",
			command: () => this.logout()
		}
	]);
	readonly isAuthenticated = toSignal(this.authService.isAuthenticated$);
	readonly avatarUrl = computed(() => {
		const customPic = this.profileService.profile()?.picture;
		if (customPic) return `data:image/jpeg;base64,${customPic}`;
		return this.user()?.picture;
	});

	ngOnInit() {
		if (this.themeService.isDarkMode())
			this.document.documentElement.classList.add(darkModeSelector);

		this.profileService.getProfile().pipe(takeUntilDestroyed(this.destroyRef)).subscribe();

		this.ngZone.runOutsideAngular(() => {
			fromEvent<MouseEvent>(this.document, "mousemove")
				.pipe(takeUntilDestroyed(this.destroyRef))
				.subscribe({
					next: e => {
						this.document.documentElement.style.setProperty("--mx", `${e.clientX}px`);
						this.document.documentElement.style.setProperty("--my", `${e.clientY}px`);
					}
				});
		});
	}

	togglePrivacy() {
		this.privacyService.togglePrivacy();
	}

	onProfileClick() {
		this.router.navigate(["/profile"]);
	}

	logout() {
		this.authService.logout({ logoutParams: { returnTo: this.document.location.origin } });
	}
}
