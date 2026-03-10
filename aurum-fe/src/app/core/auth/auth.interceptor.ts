import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpEvent } from "@angular/common/http";
import { inject } from "@angular/core";
import { AuthService } from "./auth.service";
import { Observable, switchMap, take } from "rxjs";

export const authInterceptor: HttpInterceptorFn = (
	req: HttpRequest<unknown>,
	next: HttpHandlerFn
): Observable<HttpEvent<unknown>> => {
	const authService = inject(AuthService);
	const isApiRequest = req.url.includes("/api/");

	if (!isApiRequest) return next(req);

	return authService.getAccessToken().pipe(
		take(1),
		switchMap(token => {
			if (token) {
				return next(
					req.clone({
						setHeaders: {
							Authorization: `Bearer ${token}`
						}
					})
				);
			}

			return next(req);
		})
	);
};
