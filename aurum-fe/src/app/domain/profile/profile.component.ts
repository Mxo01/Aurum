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
import { RouterLink } from "@angular/router";
import { Card } from "primeng/card";
import { Avatar } from "primeng/avatar";
import { InputText } from "primeng/inputtext";
import { Button } from "primeng/button";
import { Tag } from "primeng/tag";
import { SelectButton } from "primeng/selectbutton";
import { FormsModule } from "@angular/forms";
import { ThemeService } from "../../shared/services/theme/theme.service";
import { ProfileService } from "./profile.service";
import { ConfirmationService, MessageService } from "primeng/api";
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

@Component({
	selector: "app-profile",
	standalone: true,
	imports: [
		Button,
		RouterLink,
		IconField,
		InputIcon,
		Card,
		Avatar,
		FormsModule,
		InputText,
		Tag,
		SelectButton
	],
	templateUrl: "./profile.component.html",
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProfileComponent implements OnInit {
	private readonly authService = inject(AuthService);
	private readonly themeService = inject(ThemeService);
	private readonly profileService = inject(ProfileService);
	private readonly messageService = inject(MessageService);
	private readonly confirmationService = inject(ConfirmationService);

	themeOptions = signal([
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
	user = toSignal(this.authService.user$);
	theme = computed(() => this.themeService.isDarkMode());
	hasGoogleProfile = computed(() => this.user()?.sub?.includes("google"));
	isUpdatingName = signal(false);
	isDeletingProfile = signal(false);

	private readonly nameTrigger$ = new Subject<string>();

	ngOnInit() {
		this.authService.user$.subscribe(user => {
			console.log(user);
		});

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
