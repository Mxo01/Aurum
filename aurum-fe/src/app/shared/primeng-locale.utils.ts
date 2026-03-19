import { Translation } from "primeng/api";
import { Locale } from "../domain/profile/model/locale.model";

const EN: Translation = {
	firstDayOfWeek: 0,
	dayNames: ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"],
	dayNamesShort: ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"],
	dayNamesMin: ["Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"],
	monthNames: [
		"January",
		"February",
		"March",
		"April",
		"May",
		"June",
		"July",
		"August",
		"September",
		"October",
		"November",
		"December"
	],
	monthNamesShort: [
		"Jan",
		"Feb",
		"Mar",
		"Apr",
		"May",
		"Jun",
		"Jul",
		"Aug",
		"Sep",
		"Oct",
		"Nov",
		"Dec"
	],
	today: "Today",
	clear: "Clear"
};

const FR: Translation = {
	firstDayOfWeek: 1,
	dayNames: ["Dimanche", "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi"],
	dayNamesShort: ["Dim", "Lun", "Mar", "Mer", "Jeu", "Ven", "Sam"],
	dayNamesMin: ["Di", "Lu", "Ma", "Me", "Je", "Ve", "Sa"],
	monthNames: [
		"Janvier",
		"Février",
		"Mars",
		"Avril",
		"Mai",
		"Juin",
		"Juillet",
		"Août",
		"Septembre",
		"Octobre",
		"Novembre",
		"Décembre"
	],
	monthNamesShort: [
		"Jan",
		"Fév",
		"Mar",
		"Avr",
		"Mai",
		"Jun",
		"Jul",
		"Aoû",
		"Sep",
		"Oct",
		"Nov",
		"Déc"
	],
	today: "Aujourd'hui",
	clear: "Effacer"
};

const IT: Translation = {
	firstDayOfWeek: 1,
	dayNames: ["Domenica", "Lunedì", "Martedì", "Mercoledì", "Giovedì", "Venerdì", "Sabato"],
	dayNamesShort: ["Dom", "Lun", "Mar", "Mer", "Gio", "Ven", "Sab"],
	dayNamesMin: ["Do", "Lu", "Ma", "Me", "Gi", "Ve", "Sa"],
	monthNames: [
		"Gennaio",
		"Febbraio",
		"Marzo",
		"Aprile",
		"Maggio",
		"Giugno",
		"Luglio",
		"Agosto",
		"Settembre",
		"Ottobre",
		"Novembre",
		"Dicembre"
	],
	monthNamesShort: [
		"Gen",
		"Feb",
		"Mar",
		"Apr",
		"Mag",
		"Giu",
		"Lug",
		"Ago",
		"Set",
		"Ott",
		"Nov",
		"Dic"
	],
	today: "Oggi",
	clear: "Cancella"
};

export const primengLocaleMap: Record<Locale, Translation> = {
	[Locale.EN_US]: EN,
	[Locale.FR]: FR,
	[Locale.IT]: IT
};
