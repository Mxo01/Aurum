import {
	ChangeDetectionStrategy,
	Component,
	computed,
	DOCUMENT,
	inject,
	OnInit
} from "@angular/core";
import { RouterOutlet, RouterLinkActive, RouterLink } from "@angular/router";
import { ButtonModule } from "primeng/button";
import { Avatar } from "primeng/avatar";
import { AuthService } from "@auth0/auth0-angular";
import { toSignal } from "@angular/core/rxjs-interop";
import { Toolbar } from "primeng/toolbar";
import { darkModeSelector } from "./app.utils";
import { ThemeService } from "./shared/services/theme/theme.service";
import { ConfirmDialog } from "primeng/confirmdialog";
import { Toast } from "primeng/toast";
import { paths } from "./app.routes";
import { ProfileService } from "./domain/profile/profile.service";
import { GdprConsentComponent } from "./shared/components/gdpr-consent/gdpr-consent.component";

@Component({
	selector: "app-root",
	standalone: true,
	imports: [
		RouterOutlet,
		ButtonModule,
		Avatar,
		Toolbar,
		RouterLink,
		RouterLinkActive,
		ConfirmDialog,
		Toast,
		GdprConsentComponent
	],
	templateUrl: "app.component.html",
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class App implements OnInit {
	private readonly authService = inject(AuthService);
	private readonly themeService = inject(ThemeService);
	private readonly profileService = inject(ProfileService);
	private readonly document = inject(DOCUMENT);

	protected readonly user = toSignal(this.authService.user$);
	protected readonly paths = Object.entries(paths).filter(
		([path]) => path !== "/profile" && path !== "/assets/categories"
	);

	protected readonly avatarUrl = computed(() => {
		const customPic = this.profileService.profile()?.picture;
		if (customPic) return `data:image/jpeg;base64,${customPic}`;
		return this.user()?.picture;
	});

	ngOnInit() {
		if (this.themeService.isDarkMode())
			this.document.documentElement.classList.add(darkModeSelector);

		this.profileService.getProfile().subscribe();
	}

	logout() {
		this.authService.logout({ logoutParams: { returnTo: this.document.location.origin } });
	}
}
