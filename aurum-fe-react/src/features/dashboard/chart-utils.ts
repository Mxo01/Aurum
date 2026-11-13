import type { ChartDataDTO } from "@/lib/api/types";

export interface ChartRow {
	label: string;
	netWorth: number;
	[key: string]: number | string;
}

export interface FavoriteSeries {
	key: string;
	name: string;
	isLiability: boolean;
}

export function hasChartData(chart: ChartDataDTO | undefined) {
	return Boolean(chart && chart.totalAssetsOnly.some(value => value !== 0));
}

export function buildChartRows(chart: ChartDataDTO | undefined): {
	rows: ChartRow[];
	favorites: FavoriteSeries[];
} {
	if (!chart) return { rows: [], favorites: [] };

	const favorites = Object.entries(chart.favoriteAssetsValues).map(([name, values], index) => ({
		key: `favorite${index}`,
		name,
		isLiability: chart.favoriteAssetsTypes[name] === "LIABILITY",
		values
	}));

	const rows = chart.labels.map((label, index) => {
		const row: ChartRow = {
			label,
			netWorth: chart.totalNetWorth[index] ?? 0
		};
		favorites.forEach(favorite => {
			row[favorite.key] = favorite.values[index] ?? 0;
		});
		return row;
	});

	return {
		rows,
		favorites: favorites.map(({ key, name, isLiability }) => ({ key, name, isLiability }))
	};
}

export function liabilitiesSparkline(chart: ChartDataDTO | undefined) {
	if (!chart) return [];

	const series = chart.totalAssetsOnly.map((assets, index) =>
		Math.abs(assets - Math.abs(chart.totalNetWorth[index] ?? 0))
	);

	return series.filter((value, index, all) => index === 0 || value !== all[index - 1]);
}

export function projectedGrowth(projected: number | undefined, current: number | undefined) {
	if (!projected || !current) return undefined;
	return ((projected - current) / current) * 100;
}

export function monthlyAverage(absolute: number | undefined) {
	if (absolute === undefined) return undefined;
	return absolute / 12;
}

export function monthlyAveragePercent(percentage: number | undefined) {
	if (percentage === undefined) return undefined;
	return (Math.pow(1 + percentage / 100, 1 / 12) - 1) * 100;
}

export function yearOptions(oldestYear: number | null | undefined, currentYear: number) {
	const start = oldestYear ?? currentYear;
	const years: number[] = [];
	for (let year = currentYear; year >= start; year -= 1) years.push(year);
	return years;
}

