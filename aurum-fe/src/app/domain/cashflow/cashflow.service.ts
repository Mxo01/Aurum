import { HttpClient } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { environment } from "../../../environments/environment";
import { CashFlowEntry, CashFlowYear, UpdateCashFlowEntry } from "./model/cashflow.model";

@Injectable({
	providedIn: "root"
})
export class CashFlowService {
	private readonly http = inject(HttpClient);
	private readonly url = environment.apiUrl + "/cashflows";

	getYear(year: number) {
		return this.http.get<CashFlowYear>(`${this.url}/${year}`);
	}

	updateEntry(year: number, month: number, dto: UpdateCashFlowEntry) {
		return this.http.put<CashFlowEntry>(`${this.url}/${year}/${month}`, dto);
	}
}
