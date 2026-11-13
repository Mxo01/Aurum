import createClient, { type Client } from "openapi-fetch";
import { env } from "../env";
import type { paths } from "./schema";

export class ApiError extends Error {
	readonly status: number;
	readonly body: unknown;

	constructor(status: number, body: unknown) {
		super(`Request failed with status ${status}`);
		this.name = "ApiError";
		this.status = status;
		this.body = body;
	}
}

export type ApiClient = Client<paths>;

let onUnauthorized: (() => void) | undefined;

export function registerUnauthorizedHandler(handler: () => void) {
	onUnauthorized = handler;
}

export function createApiClient(getToken: () => Promise<string | undefined>): ApiClient {
	return createClient<paths>({
		baseUrl: env.apiUrl,
		fetch: async request => {
			const token = await getToken().catch(() => undefined);
			const headers = new Headers(request.headers);
			if (token) headers.set("Authorization", `Bearer ${token}`);

			const response = await fetch(new Request(request, { headers }));
			if (response.status === 401) onUnauthorized?.();
			return response;
		}
	});
}

interface ApiResult<T> {
	data?: T;
	error?: unknown;
	response: Response;
}

export function unwrap<T>(result: ApiResult<T>): T {
	if (!result.response.ok || result.error !== undefined) {
		throw new ApiError(result.response.status, result.error);
	}
	return result.data as T;
}

export function isApiError(error: unknown): error is ApiError {
	return error instanceof ApiError;
}
