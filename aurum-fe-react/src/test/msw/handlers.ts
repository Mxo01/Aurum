import { http, HttpResponse } from "msw";
import { env } from "@/lib/env";
import {
	buildAsset,
	buildCashflowYear,
	buildCategory,
	buildChart,
	buildProfile,
	buildSnapshot,
	buildSummary,
	buildTarget
} from "@/test/fixtures/portfolio";

export const baseUrl = env.apiUrl;

export const handlers = [
	http.get(`${baseUrl}/users`, () => HttpResponse.json(buildProfile())),
	http.put(`${baseUrl}/users`, () => new HttpResponse(null, { status: 200 })),
	http.put(`${baseUrl}/users/currency`, () => new HttpResponse(null, { status: 200 })),
	http.put(`${baseUrl}/users/locale`, () => new HttpResponse(null, { status: 200 })),
	http.post(`${baseUrl}/users/picture`, () => new HttpResponse(null, { status: 200 })),
	http.delete(`${baseUrl}/users/picture`, () => new HttpResponse(null, { status: 200 })),
	http.delete(`${baseUrl}/users`, () => new HttpResponse(null, { status: 200 })),

	http.get(`${baseUrl}/assets`, () => HttpResponse.json([buildAsset()])),
	http.post(`${baseUrl}/assets`, () => HttpResponse.json(buildAsset())),
	http.put(`${baseUrl}/assets/:id`, () => HttpResponse.json(buildAsset())),
	http.delete(`${baseUrl}/assets/:id`, () => new HttpResponse(null, { status: 200 })),

	http.get(`${baseUrl}/categories`, () => HttpResponse.json([buildCategory()])),
	http.post(`${baseUrl}/categories`, () => HttpResponse.json(buildCategory())),
	http.put(`${baseUrl}/categories/:id`, () => HttpResponse.json(buildCategory())),
	http.delete(`${baseUrl}/categories/:id`, () => new HttpResponse(null, { status: 200 })),

	http.get(`${baseUrl}/snapshots`, () => HttpResponse.json([buildSnapshot()])),
	http.post(`${baseUrl}/snapshots`, () => HttpResponse.json(buildSnapshot())),
	http.delete(`${baseUrl}/snapshots/bulk`, () => new HttpResponse(null, { status: 200 })),

	http.get(`${baseUrl}/targets`, () => HttpResponse.json([buildTarget()])),
	http.post(`${baseUrl}/targets`, () => HttpResponse.json(buildTarget())),
	http.put(`${baseUrl}/targets/:id`, () => HttpResponse.json(buildTarget())),
	http.delete(`${baseUrl}/targets/:id`, () => new HttpResponse(null, { status: 200 })),

	http.get(`${baseUrl}/cashflows/:year`, ({ params }) =>
		HttpResponse.json(buildCashflowYear({ year: Number(params.year) }))
	),
	http.put(`${baseUrl}/cashflows/:year/:month`, ({ params }) =>
		HttpResponse.json({
			id: "entry",
			month: Number(params.month),
			earned: 0,
			spent: 0
		})
	),

	http.get(`${baseUrl}/analytics/summary`, () => HttpResponse.json(buildSummary())),
	http.get(`${baseUrl}/analytics/chart`, () => HttpResponse.json(buildChart())),
	http.get(`${baseUrl}/analytics/projections`, () => HttpResponse.json({ "1": 620500 }))
];
