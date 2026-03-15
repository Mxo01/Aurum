type Truthy<T> = Exclude<T, false | "" | null | undefined>;

export function isTruthy<T>(value: T): value is Truthy<T> {
	return value !== false && value !== "" && value !== null && value !== undefined;
}
