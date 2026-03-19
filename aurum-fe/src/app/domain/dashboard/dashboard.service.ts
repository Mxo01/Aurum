import { HttpClient } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { environment } from "../../../environments/environment";
import { AnalyticsSummary, ChartData } from "./model/dashboard.model";
import { Observable } from "rxjs";

@Injectable({
	providedIn: "root"
})
export class DashboardService {
	private readonly http = inject(HttpClient);

	private readonly analyticsUrl = `${environment.apiUrl}/analytics`;

	getSummary(): Observable<AnalyticsSummary> {
		return this.http.get<AnalyticsSummary>(`${this.analyticsUrl}/summary`);
	}

	getChartData(allHistory: boolean = false): Observable<ChartData> {
		const params = allHistory ? "?allHistory=true" : "";
		return this.http.get<ChartData>(`${this.analyticsUrl}/chart${params}`);
	}

	getChartDataForYear(year: number): Observable<ChartData> {
		return this.http.get<ChartData>(`${this.analyticsUrl}/chart?year=${year}`);
	}
}
