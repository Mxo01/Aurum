import { Divider } from "primeng/divider";
import {
	ChangeDetectionStrategy,
	Component,
	computed,
	inject,
	signal,
	OnInit
} from "@angular/core";
import { toSignal } from "@angular/core/rxjs-interop";
import { AuthService } from "@auth0/auth0-angular";
import { Card } from "primeng/card";
import { InputText } from "primeng/inputtext";
import { Button } from "primeng/button";
import { SelectButton, SelectButtonChangeEvent } from "primeng/selectbutton";
import { Select, SelectChangeEvent } from "primeng/select";
import { FormsModule } from "@angular/forms";
import { ThemeService } from "../../shared/services/theme/theme.service";
import { ProfileService } from "./profile.service";
import { ConfirmationService } from "primeng/api";
import {
	catchError,
	debounceTime,
	distinctUntilChanged,
	EMPTY,
	finalize,
	Subject,
	switchMap
} from "rxjs";
import { IconField } from "primeng/iconfield";
import { InputIcon } from "primeng/inputicon";
import { paths } from "../../app.routes";
import { NavigationService } from "../../shared/services/navigation/navigation.service";
import { RouterLink } from "@angular/router";
import { currencyOptions } from "../asset/components/asset-form/asset-form.utils";
import { localeOptions } from "./profile.utils";

@Component({
	selector: "app-profile",
	standalone: true,
	imports: [
		Button,
		IconField,
		InputIcon,
		Card,
		FormsModule,
		InputText,
		SelectButton,
		Select,
		RouterLink,
		Divider
	],
	templateUrl: "./profile.component.html",
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProfileComponent implements OnInit {
	private readonly authService = inject(AuthService);
	private readonly themeService = inject(ThemeService);
	private readonly profileService = inject(ProfileService);
	private readonly confirmationService = inject(ConfirmationService);
	private readonly navigationService = inject(NavigationService);

	protected readonly paths = paths;
	protected readonly previousRoute = this.navigationService.previousRoute;
	protected readonly themeOptions = signal([
		{
			label: "Dark",
			value: true,
			icon: "pi pi-moon"
		},
		{
			label: "Light",
			value: false,
			icon: "pi pi-sun"
		}
	]);
	protected readonly user = toSignal(this.authService.user$);
	protected readonly theme = computed(() => this.themeService.isDarkMode());
	protected readonly profile = toSignal(this.profileService.getProfile());
	protected readonly hasGoogleProfile = computed(() => this.user()?.sub?.includes("google"));
	protected readonly isUpdatingName = signal(false);
	protected readonly currencyOptions = signal(currencyOptions);
	protected readonly localeOptions = signal(localeOptions);
	protected readonly isDeletingProfile = signal(false);

	private readonly nameTrigger$ = new Subject<string>();

	ngOnInit() {
		this.nameTrigger$
			.pipe(debounceTime(500), distinctUntilChanged())
			.pipe(
				switchMap(name => {
					this.isUpdatingName.set(true);

					return this.profileService.updateName(name).pipe(
						finalize(() => this.isUpdatingName.set(false)),
						catchError(() => EMPTY)
					);
				})
			)
			.subscribe();
	}

	updateName(name: string) {
		this.nameTrigger$.next(name);
	}

	chooseCurrency(event: SelectButtonChangeEvent) {
		this.profileService.updateCurrency(event.value).subscribe();
	}

	chooseLocale(event: SelectChangeEvent) {
		this.profileService.updateLocale(event.value).subscribe();
	}

	toggleTheme() {
		this.themeService.toggleTheme();
	}

	deleteProfile(event: Event) {
		this.confirmationService.confirm({
			target: event.target as EventTarget,
			message: "Are you sure you want to delete your profile?",
			header: "Danger Zone",
			icon: "pi pi-info-circle",
			rejectLabel: "Cancel",
			rejectButtonProps: {
				label: "Cancel",
				severity: "secondary",
				outlined: true
			},
			acceptButtonProps: {
				label: "Delete",
				severity: "danger",
				loading: this.isDeletingProfile()
			},

			accept: () => {
				this.isDeletingProfile.set(true);

				this.profileService
					.deleteProfile()
					.pipe(finalize(() => this.isDeletingProfile.set(false)))
					.subscribe();
			}
		});
	}
}
