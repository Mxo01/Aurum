import { Divider } from "primeng/divider";
import {
	ChangeDetectionStrategy,
	Component,
	computed,
	DestroyRef,
	inject,
	signal,
	OnInit
} from "@angular/core";
import { McpSetupComponent } from "./mcp/mcp-setup.component";
import { takeUntilDestroyed, toSignal } from "@angular/core/rxjs-interop";
import { AuthService } from "@auth0/auth0-angular";
import { InputText } from "primeng/inputtext";
import { Button } from "primeng/button";
import { SelectButton } from "primeng/selectbutton";
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
import { currencyOptions, localeOptions } from "./profile.utils";

@Component({
	selector: "app-profile",
	standalone: true,
	imports: [
		Button,
		IconField,
		InputIcon,
		FormsModule,
		InputText,
		SelectButton,
		Select,
		RouterLink,
		Divider,
		McpSetupComponent
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
	private readonly destroyRef = inject(DestroyRef);

	readonly paths = paths;
	readonly previousRoute = computed(() => this.navigationService.previousRoute());
	readonly themeOptions = signal([
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
	readonly user = toSignal(this.authService.user$);
	readonly theme = computed(() => this.themeService.isDarkMode());
	readonly profile = this.profileService.profile;
	readonly hasGoogleProfile = computed(() => this.user()?.sub?.includes("google"));
	readonly isUpdatingName = signal(false);
	readonly isUploadingPicture = signal(false);
	readonly currencyOptions = signal(currencyOptions);
	readonly localeOptions = signal(localeOptions);
	readonly isDeletingProfile = signal(false);
	readonly isExportingData = signal(false);

	readonly pictureUrl = computed(() => {
		const customPic = this.profile()?.picture;
		if (customPic) return `data:image/jpeg;base64,${customPic}`;
		return this.user()?.picture ?? null;
	});

	readonly hasPicture = computed(() => !!this.profile()?.picture);

	private readonly nameTrigger$ = new Subject<string>();

	ngOnInit() {
		this.profileService.getProfile().pipe(takeUntilDestroyed(this.destroyRef)).subscribe();

		this.nameTrigger$
			.pipe(
				debounceTime(1000),
				distinctUntilChanged(),
				switchMap(name => {
					this.isUpdatingName.set(true);

					return this.profileService.updateName(name).pipe(
						finalize(() => this.isUpdatingName.set(false)),
						catchError(() => EMPTY)
					);
				}),
				takeUntilDestroyed(this.destroyRef)
			)
			.subscribe();
	}

	updateName(name: string) {
		this.nameTrigger$.next(name);
	}

	chooseCurrency(event: SelectChangeEvent) {
		this.profileService.updateCurrency(event.value).subscribe();
	}

	chooseLocale(event: SelectChangeEvent) {
		this.profileService.updateLocale(event.value).subscribe();
	}

	toggleTheme() {
		this.themeService.toggleTheme();
	}

	onFileSelected(event: Event) {
		const file = (event.target as HTMLInputElement).files?.[0];
		if (!file) return;

		this.isUploadingPicture.set(true);
		this.profileService
			.updatePicture(file)
			.pipe(finalize(() => this.isUploadingPicture.set(false)))
			.subscribe();
	}

	clearPicture() {
		this.profileService.deletePicture().subscribe();
	}

	exportData() {
		this.isExportingData.set(true);
		this.profileService
			.exportData()
			.pipe(finalize(() => this.isExportingData.set(false)))
			.subscribe();
	}

	deleteProfile(event: Event) {
		this.confirmationService.confirm({
			target: event.target as EventTarget,
			message: "Are you sure you want to delete your profile?",
			header: "Danger Zone",
			icon: "pi pi-info-circle",
			rejectVisible: false,
			acceptButtonProps: {
				size: "small",
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
