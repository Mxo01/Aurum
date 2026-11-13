import { expect, test as base, type Page, type Route } from "@playwright/test";

const API = "http://api.test";
const CLIENT_ID = "ZHikUaUM2UOqcOHtTzdPephPVMWifRPj";
const AUDIENCE = "https://api.aurum.com";
const SCOPE = "openid profile email offline_access";

const b64 = (value: unknown) => Buffer.from(JSON.stringify(value)).toString("base64url");
const now = Math.floor(Date.now() / 1000);
const jwt = `${b64({ alg: "none", typ: "JWT" })}.${b64({
	sub: "auth0|e2e",
	name: "E2E User",
	email: "e2e@example.com",
	aud: CLIENT_ID,
	iss: "https://aurum-api.eu.auth0.com/",
	iat: now,
	exp: now + 86400
})}.sig`;

const profile = { id: "u1", currency: "EUR", locale: "en-US", picture: null };

const categories = [
	{ id: "c1", name: "Real Estate", type: "ASSET", icon: "pi-home", isDefault: true },
	{ id: "c2", name: "Investments", type: "ASSET", icon: "pi-chart-line", isDefault: true },
	{ id: "c3", name: "Cash", type: "ASSET", icon: "pi-wallet", isDefault: true },
	{ id: "c6", name: "Mortgage", type: "LIABILITY", icon: "pi-building", isDefault: true }
];

interface Asset {
	id: string;
	name: string;
	categoryId: string;
	categoryName: string;
	categoryIcon: string | null;
	type: "ASSET" | "LIABILITY";
	originalCurrency: string;
	isFavorite: boolean;
	snapshots: unknown[];
	latestValue: number;
	latestValueBase: number;
	previousValue: number;
	initialValue: number;
	referenceDate: string;
	liabilityType: "MANUAL" | "AUTOMATIC" | null;
	paymentFrequency: null;
	paymentAmount: null;
}

function buildAsset(overrides: Partial<Asset> & Pick<Asset, "id" | "name">): Asset {
	const { id, name, ...rest } = overrides;
	const category = categories.find(item => item.id === rest.categoryId) ?? categories[0];
	return {
		id,
		name,
		categoryId: rest.categoryId ?? category.id,
		categoryName: overrides.categoryName ?? category.name,
		categoryIcon: category.icon,
		type: overrides.type ?? "ASSET",
		originalCurrency: "EUR",
		isFavorite: false,
		snapshots: [],
		latestValue: 0,
		latestValueBase: 0,
		previousValue: 0,
		initialValue: 0,
		referenceDate: "2026-11-01",
		liabilityType: null,
		paymentFrequency: null,
		paymentAmount: null,
		...rest
	};
}

const entries = Array.from({ length: 12 }, (_, index) => ({
	id: `e${index}`,
	month: index + 1,
	earned: 5000 + index * 100,
	spent: 4000 + index * 50
}));

const previousEntries = Array.from({ length: 12 }, (_, index) => ({
	id: `p${index}`,
	month: index + 1,
	earned: 4500 + index * 100,
	spent: 3800 + index * 50
}));

const chartLabels = [
	"DEC 24",
	"JAN 25",
	"FEB 25",
	"MAR 25",
	"APR 25",
	"MAY 25",
	"JUN 25",
	"JUL 25",
	"AUG 25",
	"SEP 25",
	"OCT 25",
	"NOV 25",
	"DEC 25",
	"JAN 26",
	"FEB 26",
	"MAR 26",
	"APR 26",
	"MAY 26",
	"JUN 26",
	"JUL 26",
	"AUG 26",
	"SEP 26",
	"OCT 26",
	"NOV 26"
];
const netWorthSeries = chartLabels.map((_, index) => 520000 + index * 8000);

const variation = {
	oneMonth: { absolute: 4200, percentage: 0.61 },
	sixMonths: { absolute: 28400, percentage: 4.2 },
	oneYear: { absolute: 62100, percentage: 9.04 }
};

export class ApiMock {
	assets: Asset[] = [
		buildAsset({
			id: "a1",
			name: "Apartment Berlin",
			categoryId: "c1",
			latestValue: 430000,
			latestValueBase: 430000,
			previousValue: 412000
		}),
		buildAsset({
			id: "a2",
			name: "Global Equity ETF",
			categoryId: "c2",
			latestValue: 128450,
			latestValueBase: 128450,
			previousValue: 119300,
			isFavorite: true
		}),
		buildAsset({
			id: "a3",
			name: "Mortgage",
			categoryId: "c6",
			type: "LIABILITY",
			latestValue: 268000,
			latestValueBase: 268000,
			previousValue: 273500,
			liabilityType: "MANUAL"
		})
	];

	cashflow = {
		year: 2026,
		entries,
		previousYearEntries: previousEntries,
		availableYears: [2025, 2026]
	};

	lastCashflowYear: number | null = null;

	async handle(route: Route) {
		const request = route.request();
		const url = new URL(request.url());
		const path = url.pathname;
		const method = request.method();

		if (method === "OPTIONS") {
			return route.fulfill({
				status: 204,
				headers: {
					"access-control-allow-origin": "*",
					"access-control-allow-headers": "authorization, content-type",
					"access-control-allow-methods": "GET, POST, PUT, DELETE, OPTIONS"
				}
			});
		}

		if (path === "/users" && method === "GET") return this.json(route, profile);
		if (path === "/users/currency" || path === "/users/locale") {
			return route.fulfill({ status: 200, headers: { "access-control-allow-origin": "*" } });
		}
		if (path === "/users/export") return this.json(route, { assets: this.assets });

		if (path === "/analytics/summary") {
			const assetTotal = this.assets
				.filter(asset => asset.type === "ASSET")
				.reduce((sum, asset) => sum + asset.latestValueBase, 0);
			const liabilityTotal = this.assets
				.filter(asset => asset.type === "LIABILITY")
				.reduce((sum, asset) => sum + asset.latestValueBase, 0);

			return this.json(route, {
				totalNetWorth: assetTotal - liabilityTotal,
				totalGrossAssets: assetTotal,
				variations: variation,
				assetVariations: variation,
				assetAllocation: { "Real Estate": 60.1, Investments: 17.9, Mortgage: 22.0 },
				currencyImpact: 1830,
				totalLiabilities: liabilityTotal,
				debtToAssetRatio: 37.4,
				topAssets: this.assets.slice(0, 5),
				oldestSnapshotYear: 2024
			});
		}

		if (path === "/analytics/chart") {
			return this.json(route, {
				labels: chartLabels,
				totalNetWorth: netWorthSeries,
				totalAssetsOnly: netWorthSeries.map(value => value + 268000),
				favoriteAssetsValues: { "Apartment Berlin": netWorthSeries },
				favoriteAssetsTypes: { "Apartment Berlin": "ASSET" }
			});
		}

		if (path === "/analytics/projections") return this.json(route, { 1: 620500 });

		if (path === "/targets") {
			return this.json(route, [
				{
					id: "t1",
					name: "Financial independence",
					targetAmount: 1000000,
					currentAmount: 570000,
					completionPercentage: 57,
					deadline: "2035-12-31",
					isCompleted: false,
					type: "NET_WORTH",
					status: "IN_PROGRESS"
				}
			]);
		}

		if (path === "/assets" && method === "GET") return this.json(route, this.assets);
		if (path === "/assets" && method === "POST") {
			const body = await request.postDataJSON();
			const category = categories.find(item => item.id === body.categoryId) ?? categories[0];
			const asset = buildAsset({
				id: `a${this.assets.length + 1}`,
				name: body.name,
				categoryId: category.id,
				latestValue: body.initialValue ?? 0,
				latestValueBase: body.initialValue ?? 0,
				isFavorite: Boolean(body.isFavorite)
			});
			this.assets.push(asset);
			return this.json(route, asset, 200);
		}
		if (path.startsWith("/assets/") && method === "PUT") {
			const body = await request.postDataJSON();
			const id = path.split("/").pop();
			this.assets = this.assets.map(asset => (asset.id === id ? { ...asset, ...body } : asset));
			return this.json(
				route,
				this.assets.find(asset => asset.id === id)
			);
		}
		if (path.startsWith("/assets/") && method === "DELETE") {
			const id = path.split("/").pop();
			this.assets = this.assets.filter(asset => asset.id !== id);
			return route.fulfill({ status: 200, headers: { "access-control-allow-origin": "*" } });
		}
		if (path === "/categories") return this.json(route, categories);
		if (path === "/snapshots" && method === "GET") return this.json(route, []);
		if (path === "/snapshots" && method === "POST") {
			const body = await request.postDataJSON();
			this.assets = this.assets.map(asset =>
				asset.id === body.assetId
					? {
							...asset,
							latestValue: body.amountOriginalCurrency,
							latestValueBase: body.amountOriginalCurrency
						}
					: asset
			);
			return this.json(route, { id: "snapshot" });
		}

		if (path.startsWith("/cashflows/")) {
			const year = Number(path.split("/")[2]);
			this.lastCashflowYear = year;
			if (method === "PUT") {
				const body = await request.postDataJSON();
				const month = Number(path.split("/")[3]);
				this.cashflow = {
					...this.cashflow,
					entries: this.cashflow.entries.map(entry =>
						entry.month === month ? { ...entry, ...body } : entry
					)
				};
				return this.json(route, { id: `e${month}`, month, ...body });
			}
			return this.json(route, { ...this.cashflow, year });
		}

		return this.json(route, {});
	}

	private json(route: Route, body: unknown, status = 200) {
		return route.fulfill({
			status,
			contentType: "application/json",
			headers: { "access-control-allow-origin": "*" },
			body: JSON.stringify(body)
		});
	}
}

export async function setupPage(page: Page, api: ApiMock) {
	await page.addInitScript(
		({ clientId, audience, scope, token }) => {
			const decoded = {
				user: { name: "E2E User", email: "e2e@example.com", picture: "" },
				claims: { sub: "auth0|e2e" }
			};
			localStorage.setItem(
				`@@auth0spajs@@::${clientId}::@@user@@`,
				JSON.stringify({ id_token: token, decodedToken: decoded })
			);
			localStorage.setItem(
				`@@auth0spajs@@::${clientId}::${audience}::${scope}`,
				JSON.stringify({
					body: {
						client_id: clientId,
						access_token: "e2e-token",
						id_token: token,
						scope,
						expires_in: 86400,
						audience,
						token_type: "Bearer",
						decodedToken: decoded
					},
					expiresAt: Math.floor(Date.now() / 1000) + 86400
				})
			);
			localStorage.setItem("gdpr_consent_accepted", "true");
		},
		{ clientId: CLIENT_ID, audience: AUDIENCE, scope: SCOPE, token: jwt }
	);

	await page.route("**/oauth/token", route =>
		route.fulfill({
			status: 200,
			contentType: "application/json",
			headers: { "access-control-allow-origin": "*" },
			body: JSON.stringify({
				access_token: "e2e-token",
				id_token: jwt,
				expires_in: 86400,
				token_type: "Bearer",
				scope: SCOPE
			})
		})
	);

	await page.route(`${API}/**`, route => api.handle(route));
}

type Fixtures = { api: ApiMock };

export const test = base.extend<Fixtures>({
	api: [
		async ({ page }, use) => {
			const api = new ApiMock();
			await setupPage(page, api);
			await use(api);
		},
		{ auto: true }
	]
});

export { expect };
