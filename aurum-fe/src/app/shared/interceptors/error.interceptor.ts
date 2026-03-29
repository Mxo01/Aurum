import { HttpInterceptorFn } from "@angular/common/http";
import { inject } from "@angular/core";
import { AuthService } from "@auth0/auth0-angular";
import { MessageService } from "primeng/api";
import { catchError, throwError } from "rxjs";

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
	const authService = inject(AuthService);
	const messageService = inject(MessageService);

	return next(req).pipe(
		catchError(error => {
			if (error.status === 401) {
				authService.logout();
			} else if (error.status === 403) {
				messageService.add({ severity: "error", summary: "Access denied", life: 5000 });
			} else if (error.status >= 500) {
				messageService.add({
					severity: "error",
					summary: "Something went wrong. Please try again later.",
					life: 5000
				});
			}
			return throwError(() => error);
		})
	);
};
