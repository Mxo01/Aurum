import { format, parseISO } from "date-fns";

/** A date-only string (`YYYY-MM-DD`) is parsed at local midnight, never shifted by UTC. */
export function parseDateOnly(value: string) {
	return parseISO(`${value.slice(0, 10)}T00:00:00`);
}

export function formatDate(value: string | Date, locale: string) {
	const date = typeof value === "string" ? parseDateOnly(value) : value;
	return new Intl.DateTimeFormat(locale, { dateStyle: "medium" }).format(date);
}

export function formatDatePattern(value: string | Date, pattern: string) {
	const date = typeof value === "string" ? parseDateOnly(value) : value;
	return format(date, pattern);
}

export function toDateOnly(value: Date) {
	const year = value.getFullYear();
	const month = String(value.getMonth() + 1).padStart(2, "0");
	const day = String(value.getDate()).padStart(2, "0");
	return `${year}-${month}-${day}`;
}

export function todayISO() {
	return toDateOnly(new Date());
}
