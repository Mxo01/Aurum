import { HttpClient } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { environment } from "../../../environments/environment";
import { AnalyticsSummary, ChartData, Target } from "./model/dashboard.model";
import { Observable } from "rxjs";

@Injectable({
	providedIn: "root"
})
export class DashboardService {
	private readonly http = inject(HttpClient);

	private readonly analyticsUrl = `${environment.apiUrl}/analytics`;
	private readonly targetUrl = `${environment.apiUrl}/targets`;

	getSummary(): Observable<AnalyticsSummary> {
		return this.http.get<AnalyticsSummary>(`${this.analyticsUrl}/summary`);
	}

	getChartData(): Observable<ChartData> {
		return this.http.get<ChartData>(`${this.analyticsUrl}/chart`);
	}

	getTargets(): Observable<Target[]> {
		return this.http.get<Target[]>(this.targetUrl);
	}

	saveTarget(target: Target): Observable<Target> {
		if (target.id) {
			return this.http.put<Target>(`${this.targetUrl}/${target.id}`, target);
		}
		return this.http.post<Target>(this.targetUrl, target);
	}
}
