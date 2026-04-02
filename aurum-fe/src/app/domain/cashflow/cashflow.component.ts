import {
	ChangeDetectionStrategy,
	Component,
	computed,
	DestroyRef,
	inject,
	OnInit,
	signal
} from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { catchError, EMPTY, finalize, switchMap, tap } from "rxjs";
import { Button } from "primeng/button";
import { Message } from "primeng/message";
import { PrivacyCurrencyPipe } from "../../shared/pipes/privacy-currency.pipe";

import { CashFlowService } from "./cashflow.service";
import { CashFlowEntry, CashFlowRow, CashFlowYear, MONTH_LABELS } from "./model/cashflow.model";
import { ProfileService } from "../profile/profile.service";
import { Currency } from "../profile/model/currency.model";
import { Locale } from "../profile/model/locale.model";
import { ThemeService } from "../../shared/services/theme/theme.service";
import { PrivacyService } from "../../shared/services/privacy/privacy.service";
import { getCurrencySymbol } from "../profile/profile.utils";
import { CashflowKpiCardComponent } from "./components/cashflow-kpi-card/cashflow-kpi-card.component";
import { CashflowChartComponent } from "./components/cashflow-chart/cashflow-chart.component";
import { CashflowTableComponent } from "./components/cashflow-table/cashflow-table.component";
import { paths } from "../../app.routes";
import { NavigationService } from "../../shared/services/navigation/navigation.service";
import { RouterLink } from "@angular/router";

@Component({
	selector: "app-cashflow",
	standalone: true,
	imports: [
		Button,
		RouterLink,
		Message,
		PrivacyCurrencyPipe,
		CashflowKpiCardComponent,
		CashflowChartComponent,
		CashflowTableComponent
	],
	templateUrl: "./cashflow.component.html",
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class CashFlowComponent implements OnInit {
	private readonly cashFlowService = inject(CashFlowService);
	private readonly profileService = inject(ProfileService);
	private readonly themeService = inject(ThemeService);
	private readonly privacyService = inject(PrivacyService);
	private readonly navigationService = inject(NavigationService);
	private readonly destroyRef = inject(DestroyRef);

	readonly paths = paths;
	readonly previousRoute = computed(() => this.navigationService.previousRoute());
	readonly isDarkMode = computed(() => this.themeService.isDarkMode());
	readonly isPrivacyMode = computed(() => this.privacyService.isPrivacyMode());
	readonly isLoading = signal(false);
	readonly currentYear = signal(new Date().getFullYear());
	readonly yearData = signal<CashFlowYear | null>(null);
	readonly currency = signal<Currency>(Currency.EUR);
	readonly locale = signal<Locale>(Locale.EN_US);
	readonly hasPrevYearData = computed(() => this.yearData()?.previousYearEntries !== null);
	readonly availableYears = computed(() => this.yearData()?.availableYears ?? []);
	readonly rows = computed<CashFlowRow[]>(() => {
		const data = this.yearData();
		if (!data) return [];

		return data.entries.map((entry, i) => {
			const prev = i > 0 ? data.entries[i - 1] : null;
			const prevBalance = prev ? prev.earned - prev.spent : null;
			const balance = entry.earned - entry.spent;
			const deltaAbsolute = prevBalance !== null ? balance - prevBalance : null;
			const deltaPercent =
				deltaAbsolute !== null && prevBalance !== null && prevBalance !== 0
					? (deltaAbsolute / Math.abs(prevBalance)) * 100
					: null;

			const prevYearEntry = data.previousYearEntries?.[i] ?? null;
			const prevYearBalance = prevYearEntry ? prevYearEntry.earned - prevYearEntry.spent : null;
			const prevYearDeltaAbsolute = prevYearBalance !== null ? balance - prevYearBalance : null;
			const prevYearDeltaPercent =
				prevYearDeltaAbsolute !== null && prevYearBalance !== null && prevYearBalance !== 0
					? (prevYearDeltaAbsolute / Math.abs(prevYearBalance)) * 100
					: null;

			return {
				month: entry.month,
				monthLabel: MONTH_LABELS[entry.month - 1],
				earned: entry.earned,
				spent: entry.spent,
				balance,
				prevMonthBalance: prevBalance,
				deltaAbsolute,
				deltaPercent,
				prevYearEarned: prevYearEntry?.earned ?? null,
				prevYearSpent: prevYearEntry?.spent ?? null,
				prevYearBalance,
				prevYearDeltaAbsolute,
				prevYearDeltaPercent
			};
		});
	});
	readonly totalEarned = computed(() => this.rows().reduce((sum, r) => sum + r.earned, 0));
	readonly totalSpent = computed(() => this.rows().reduce((sum, r) => sum + r.spent, 0));
	readonly totalBalance = computed(() => this.totalEarned() - this.totalSpent());
	readonly avgEarned = computed(() => this.totalEarned() / 12);
	readonly avgSpent = computed(() => this.totalSpent() / 12);
	readonly prevYearTotalEarned = computed(() => {
		const prev = this.yearData()?.previousYearEntries;
		if (!prev) return null;
		return prev.reduce((sum, e) => sum + e.earned, 0);
	});
	readonly prevYearTotalSpent = computed(() => {
		const prev = this.yearData()?.previousYearEntries;
		if (!prev) return null;
		return prev.reduce((sum, e) => sum + e.spent, 0);
	});
	readonly yoyEarnedDelta = computed(() => {
		const prev = this.prevYearTotalEarned();
		return prev !== null ? this.totalEarned() - prev : null;
	});
	readonly yoyEarnedPercent = computed(() => {
		const delta = this.yoyEarnedDelta();
		const prev = this.prevYearTotalEarned();
		if (delta === null || !prev) return null;
		return (delta / Math.abs(prev)) * 100;
	});
	readonly yoySpentDelta = computed(() => {
		const prev = this.prevYearTotalSpent();
		return prev !== null ? this.totalSpent() - prev : null;
	});
	readonly yoySpentPercent = computed(() => {
		const delta = this.yoySpentDelta();
		const prev = this.prevYearTotalSpent();
		if (delta === null || !prev) return null;
		return (delta / Math.abs(prev)) * 100;
	});
	readonly totalBalanceClass = computed(() => {
		const balance = this.totalBalance();
		return balance > 0 ? "text-green-500" : balance < 0 ? "text-red-400" : "";
	});
	readonly activeMonths = computed(
		() => this.rows().filter(r => r.earned > 0 || r.spent > 0).length
	);
	readonly savingsRatePercent = computed(() => {
		if (this.totalEarned() <= 0) return null;
		return Math.min(100, Math.max(0, (this.totalBalance() / this.totalEarned()) * 100));
	});
	readonly savingsRateDisplay = computed(() => {
		if (this.totalEarned() <= 0) return "—";
		const rate = (this.totalBalance() / this.totalEarned()) * 100;
		return (
			rate.toLocaleString(this.locale(), { minimumFractionDigits: 1, maximumFractionDigits: 1 }) +
			"%"
		);
	});
	readonly currencySymbol = computed(() => getCurrencySymbol(this.currency()));

	ngOnInit() {
		this.profileService
			.getProfile()
			.pipe(
				tap({
					next: profile => {
						if (profile) {
							this.currency.set(profile.currency);
							this.locale.set(profile.locale);
						}
					}
				}),
				switchMap(() => this.loadYear(this.currentYear())),
				catchError(() => EMPTY),
				takeUntilDestroyed(this.destroyRef)
			)
			.subscribe({ next: data => this.yearData.set(data) });
	}

	private loadYear(year: number) {
		this.currentYear.set(year);
		this.isLoading.set(true);

		return this.cashFlowService.getYear(year).pipe(
			takeUntilDestroyed(this.destroyRef),
			finalize(() => this.isLoading.set(false))
		);
	}

	prevYear() {
		this.loadYear(this.currentYear() - 1).subscribe({ next: data => this.yearData.set(data) });
	}

	nextYear() {
		if (this.currentYear() < new Date().getFullYear()) {
			this.loadYear(this.currentYear() + 1).subscribe({ next: data => this.yearData.set(data) });
		}
	}

	onEntryUpdated(updatedEntry: CashFlowEntry) {
		this.yearData.update(yearData => {
			if (!yearData) return yearData;

			return {
				...yearData,
				entries: yearData.entries.map(entry =>
					entry.month === updatedEntry.month
						? {
								...entry,
								earned: updatedEntry.earned,
								spent: updatedEntry.spent,
								id: updatedEntry.id
							}
						: entry
				)
			};
		});
	}

	isCurrentYearMax() {
		return this.currentYear() >= new Date().getFullYear();
	}
}
