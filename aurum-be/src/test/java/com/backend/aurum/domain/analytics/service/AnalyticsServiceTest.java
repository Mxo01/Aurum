package com.backend.aurum.domain.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.analytics.dto.AnalyticsSummaryDTO;
import com.backend.aurum.domain.analytics.dto.ChartDataDTO;
import com.backend.aurum.domain.analytics.dto.DeltaDTO;
import com.backend.aurum.domain.asset.dto.AssetDTO;
import com.backend.aurum.domain.asset.mapper.AssetMapper;
import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.AssetCategory;
import com.backend.aurum.domain.asset.model.AssetType;
import com.backend.aurum.domain.asset.model.LiabilityType;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.asset.repository.SnapshotRepository;
import com.backend.aurum.domain.shared.currency.CurrencyConversionService;
import com.backend.aurum.domain.shared.currency.RateBook;
import com.backend.aurum.domain.shared.exception.ExchangeRateUnavailableException;
import com.backend.aurum.domain.shared.exception.NotFoundException;
import com.backend.aurum.domain.shared.port.ExchangeRateProvider;
import com.backend.aurum.domain.user.enums.Currency;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);
	private static final LocalDate ONE_YEAR_AGO = TODAY.minusYears(1);
	private static final LocalDate SUMMARY_WINDOW_START = TODAY.minusYears(1).minusMonths(2);
	private static final LocalDate CURRENCY_IMPACT_START = TODAY.minusMonths(1);
	private static final LocalDate LATEST_DATA_DATE = LocalDate.of(2026, 6, 10);
	private static final LocalDate TIE_SNAPSHOT_DATE = LocalDate.of(2026, 6, 10);
	private static final LocalDate STALE_SNAPSHOT_DATE = LocalDate.of(2024, 6, 15);
	private static final LocalDate FUTURE_SNAPSHOT_DATE = LocalDate.of(2026, 6, 20);
	private static final LocalDate CHART_SNAPSHOT_DATE = LocalDate.of(2026, 1, 1);
	private static final LocalDate CURRENCY_IMPACT_SNAPSHOT_DATE = LocalDate.of(2026, 5, 10);
	private static final LocalDate HISTORY_SNAPSHOT_DATE = LocalDate.of(2024, 12, 31);
	private static final LocalDate OLDEST_SNAPSHOT_DATE = LocalDate.of(2021, 3, 1);

	private static final Currency EUR = Currency.EUR;
	private static final Currency USD = Currency.USD;
	private static final Currency GBP = Currency.GBP;

	private static final UUID SNAPSHOT_LOWEST_ID = UUID.fromString(
		"00000000-0000-4000-8000-000000000001"
	);
	private static final UUID SNAPSHOT_HIGHEST_ID = UUID.fromString(
		"00000000-0000-4000-8000-000000000002"
	);

	private static final UUID TWIN_ASSET_ID = UUID.fromString("3f7c1e2a-0000-4000-8000-000000000001");
	private static final UUID TWIN_LIABILITY_ID = UUID.fromString(
		"9b2d4c6e-0000-4000-8000-000000000002"
	);
	private static final String TWIN_NAME = "Twin";
	private static final String TWIN_ASSET_SERIES = "Twin (3f7c1e2a)";
	private static final String TWIN_LIABILITY_SERIES = "Twin (9b2d4c6e)";
	private static final String CASH_NAME = "Cash";

	private static final BigDecimal STORED_RATE = new BigDecimal("2.0000");
	private static final BigDecimal SNAPSHOT_CURRENCY_RATE = new BigDecimal("1.50");
	private static final String OTHER_CATEGORY_LABEL = "Other";
	private static final String EXCHANGE_RATE_OUTAGE_MESSAGE =
		"Exchange rate provider is unavailable";

	private static final List<LocalDate> ONE_YEAR_CHART_DATES = List.of(
		LocalDate.of(2025, 6, 30),
		LocalDate.of(2025, 7, 31),
		LocalDate.of(2025, 8, 31),
		LocalDate.of(2025, 9, 30),
		LocalDate.of(2025, 10, 31),
		LocalDate.of(2025, 11, 30),
		LocalDate.of(2025, 12, 31),
		LocalDate.of(2026, 1, 31),
		LocalDate.of(2026, 2, 28),
		LocalDate.of(2026, 3, 31),
		LocalDate.of(2026, 4, 30),
		LocalDate.of(2026, 5, 31),
		TODAY
	);

	private static final List<LocalDate> CHART_2025_DATES = List.of(
		LocalDate.of(2025, 1, 31),
		LocalDate.of(2025, 2, 28),
		LocalDate.of(2025, 3, 31),
		LocalDate.of(2025, 4, 30),
		LocalDate.of(2025, 5, 31),
		LocalDate.of(2025, 6, 30),
		LocalDate.of(2025, 7, 31),
		LocalDate.of(2025, 8, 31),
		LocalDate.of(2025, 9, 30),
		LocalDate.of(2025, 10, 31),
		LocalDate.of(2025, 11, 30),
		LocalDate.of(2025, 12, 31)
	);

	@Mock
	private SnapshotRepository snapshotRepository;

	@Mock
	private AssetRepository assetRepository;

	@Mock
	private AssetMapper assetMapper;

	@Mock
	private UserRepository userRepository;

	@Mock
	private CurrencyConversionService conversionService;

	@Mock
	private Clock clock;

	@Mock
	private ExchangeRateProvider exchangeRateProvider;

	@InjectMocks
	private AnalyticsService testSubject;

	@Test
	void getSummary_carriesInTheLastValue_whenItsOnlySnapshotPredatesTheWindow() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, EUR);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Cash"), EUR, "Stale", false);
		Snapshot mockStaleSnapshot = buildSnapshot(mockAsset, "4000", STALE_SNAPSHOT_DATE);
		List<LocalDate> expectedDates = summaryDates(STALE_SNAPSHOT_DATE);
		RateBook mockBook = stubBook(EUR, List.of(EUR), expectedDates);
		AssetDTO stubbedTopAsset = Instancio.create(AssetDTO.class);
		when(
			assetMapper.toDtoLight(eq(mockAsset), eq(List.of(mockStaleSnapshot)), eq(mockBook), eq(TODAY))
		).thenReturn(stubbedTopAsset);
		stubAssets(mockUserId, mockAsset);
		stubSummarySnapshots(mockUserId, List.of(), List.of(mockStaleSnapshot), STALE_SNAPSHOT_DATE);

		// WHEN
		AnalyticsSummaryDTO expectedSummary = testSubject.getSummary(mockUserId);

		// THEN
		assertThat(expectedSummary.getTotalNetWorth()).isEqualByComparingTo(new BigDecimal("4000"));
		assertThat(expectedSummary.getTotalGrossAssets()).isEqualByComparingTo(new BigDecimal("4000"));
		assertThat(expectedSummary.getTotalLiabilities()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(expectedSummary.getTopAssets()).containsExactly(stubbedTopAsset);
	}

	@Test
	void getSummary_dedupesTheCarryInRow_whenBothQueriesReturnIt() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, EUR);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Equities"), EUR, "Fund", false);
		Snapshot mockSnapshot = buildSnapshot(mockAsset, "4000", LATEST_DATA_DATE);
		List<LocalDate> expectedDates = summaryDates(LATEST_DATA_DATE);
		RateBook mockBook = stubBook(EUR, List.of(EUR), expectedDates);
		AssetDTO stubbedTopAsset = Instancio.create(AssetDTO.class);
		when(
			assetMapper.toDtoLight(eq(mockAsset), eq(List.of(mockSnapshot)), eq(mockBook), eq(TODAY))
		).thenReturn(stubbedTopAsset);
		stubAssets(mockUserId, mockAsset);
		stubSummarySnapshots(
			mockUserId,
			List.of(mockSnapshot),
			List.of(mockSnapshot),
			LATEST_DATA_DATE
		);

		// WHEN
		AnalyticsSummaryDTO expectedSummary = testSubject.getSummary(mockUserId);

		// THEN
		assertThat(expectedSummary.getTotalNetWorth()).isEqualByComparingTo(new BigDecimal("4000"));
		assertThat(expectedSummary.getTopAssets()).containsExactly(stubbedTopAsset);
	}

	@Test
	void getSummary_summarisesLiabilitiesOnTheirOwnPass() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, EUR);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Equities"), EUR, "Fund", false);
		Asset mockLiability = buildLiability(
			buildCategory(AssetType.LIABILITY, "Debts"),
			EUR,
			"Mortgage",
			false
		);
		Snapshot mockAssetSnapshot = buildSnapshot(mockAsset, "1000", LATEST_DATA_DATE);
		Snapshot mockLiabilitySnapshot = buildSnapshot(mockLiability, "400", LATEST_DATA_DATE);
		List<LocalDate> expectedDates = summaryDates(LATEST_DATA_DATE);
		stubBook(EUR, List.of(EUR), expectedDates);
		stubAssets(mockUserId, mockAsset, mockLiability);
		stubSummarySnapshots(
			mockUserId,
			List.of(mockAssetSnapshot, mockLiabilitySnapshot),
			List.of(),
			LATEST_DATA_DATE
		);

		// WHEN
		AnalyticsSummaryDTO expectedSummary = testSubject.getSummary(mockUserId);

		// THEN
		BigDecimal expectedNetWorth = expectedSummary.getTotalNetWorth();
		BigDecimal expectedGrossAssets = expectedSummary.getTotalGrossAssets();
		BigDecimal expectedLiabilities = expectedSummary.getTotalLiabilities();
		assertThat(expectedGrossAssets).isEqualByComparingTo(new BigDecimal("1000"));
		assertThat(expectedLiabilities).isEqualByComparingTo(new BigDecimal("400"));
		assertThat(expectedNetWorth).isEqualByComparingTo(new BigDecimal("600"));
		assertThat(expectedNetWorth).isEqualByComparingTo(
			expectedGrossAssets.subtract(expectedLiabilities)
		);
	}

	@Test
	void getSummary_followsTheLiabilityType_notTheCategory() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, EUR);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Equities"), EUR, "Fund", false);
		Asset mockDebtInAssetCategory = buildLiability(
			buildCategory(AssetType.ASSET, "Equities"),
			EUR,
			"Retyped debt",
			false
		);
		Asset mockAssetInLiabilityCategory = buildAsset(
			buildCategory(AssetType.LIABILITY, "Debts"),
			EUR,
			"Retyped asset",
			false
		);
		Snapshot mockAssetSnapshot = buildSnapshot(mockAsset, "1000", LATEST_DATA_DATE);
		Snapshot mockDebtSnapshot = buildSnapshot(mockDebtInAssetCategory, "700", LATEST_DATA_DATE);
		Snapshot mockRetypedSnapshot = buildSnapshot(
			mockAssetInLiabilityCategory,
			"400",
			LATEST_DATA_DATE
		);
		List<LocalDate> expectedDates = summaryDates(LATEST_DATA_DATE);
		RateBook mockBook = stubBook(EUR, List.of(EUR), expectedDates);
		AssetDTO stubbedTopAsset = Instancio.create(AssetDTO.class);
		AssetDTO stubbedRetypedAsset = Instancio.create(AssetDTO.class);
		when(
			assetMapper.toDtoLight(eq(mockAsset), eq(List.of(mockAssetSnapshot)), eq(mockBook), eq(TODAY))
		).thenReturn(stubbedTopAsset);
		when(
			assetMapper.toDtoLight(
				eq(mockAssetInLiabilityCategory),
				eq(List.of(mockRetypedSnapshot)),
				eq(mockBook),
				eq(TODAY)
			)
		).thenReturn(stubbedRetypedAsset);
		stubAssets(mockUserId, mockAsset, mockDebtInAssetCategory, mockAssetInLiabilityCategory);
		stubSummarySnapshots(
			mockUserId,
			List.of(mockAssetSnapshot, mockDebtSnapshot, mockRetypedSnapshot),
			List.of(),
			LATEST_DATA_DATE
		);

		// WHEN
		AnalyticsSummaryDTO expectedSummary = testSubject.getSummary(mockUserId);

		// THEN
		assertThat(expectedSummary.getTotalGrossAssets()).isEqualByComparingTo(new BigDecimal("1400"));
		assertThat(expectedSummary.getTotalLiabilities()).isEqualByComparingTo(new BigDecimal("700"));
		assertThat(expectedSummary.getTotalNetWorth()).isEqualByComparingTo(new BigDecimal("700"));
		assertThat(expectedSummary.getDebtToAssetRatio()).isEqualByComparingTo(new BigDecimal("50"));
		assertThat(expectedSummary.getAssetAllocation()).containsOnlyKeys("Equities", "Debts");
		assertThat(expectedSummary.getAssetAllocation().get("Equities")).isEqualByComparingTo(
			new BigDecimal("71.43")
		);
		assertThat(expectedSummary.getAssetAllocation().get("Debts")).isEqualByComparingTo(
			new BigDecimal("28.57")
		);
		assertThat(expectedSummary.getTopAssets()).containsExactly(
			stubbedTopAsset,
			stubbedRetypedAsset
		);
	}

	@Test
	void getChartData_followsTheLiabilityType_whenBuildingFavouriteSeries() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, EUR);
		Asset mockDebtInAssetCategory = buildLiability(
			buildCategory(AssetType.ASSET, "Property"),
			EUR,
			"Mortgage",
			true
		);
		Asset mockAssetInLiabilityCategory = buildAsset(
			buildCategory(AssetType.LIABILITY, "Debts"),
			EUR,
			"Savings",
			true
		);
		Snapshot mockDebtSnapshot = buildSnapshot(mockDebtInAssetCategory, "500", CHART_SNAPSHOT_DATE);
		Snapshot mockAssetSnapshot = buildSnapshot(
			mockAssetInLiabilityCategory,
			"300",
			CHART_SNAPSHOT_DATE
		);
		stubBook(EUR, List.of(EUR), ONE_YEAR_CHART_DATES);
		stubAssets(mockUserId, mockDebtInAssetCategory, mockAssetInLiabilityCategory);
		stubSnapshots(mockUserId, mockDebtSnapshot, mockAssetSnapshot);

		// WHEN
		ChartDataDTO expectedChart = testSubject.getChartData(mockUserId);

		// THEN
		int lastPoint = ONE_YEAR_CHART_DATES.size() - 1;
		assertThat(expectedChart.getFavoriteAssetsValues()).containsOnlyKeys("Mortgage", "Savings");
		assertThat(expectedChart.getFavoriteAssetsTypes()).containsEntry("Mortgage", "LIABILITY");
		assertThat(expectedChart.getFavoriteAssetsTypes()).containsEntry("Savings", "ASSET");
		assertThat(
			expectedChart.getFavoriteAssetsValues().get("Mortgage").get(lastPoint)
		).isEqualByComparingTo(new BigDecimal("-500"));
		assertThat(
			expectedChart.getFavoriteAssetsValues().get("Savings").get(lastPoint)
		).isEqualByComparingTo(new BigDecimal("300"));
		assertThat(expectedChart.getTotalNetWorth().get(lastPoint)).isEqualByComparingTo(
			new BigDecimal("-200")
		);
		assertThat(expectedChart.getTotalAssetsOnly().get(lastPoint)).isEqualByComparingTo(
			new BigDecimal("300")
		);
	}

	@Test
	void getSummary_usesTheLastSnapshotAtOrBeforeToday() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, EUR);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Equities"), EUR, "Fund", false);
		Asset mockFutureOnlyAsset = buildAsset(
			buildCategory(AssetType.ASSET, "Cash"),
			EUR,
			"Future only",
			false
		);
		Snapshot mockCurrentSnapshot = buildSnapshot(mockAsset, "1000", LATEST_DATA_DATE);
		Snapshot mockFutureSnapshot = buildSnapshot(mockFutureOnlyAsset, "5000", FUTURE_SNAPSHOT_DATE);
		List<LocalDate> expectedDates = summaryDates(FUTURE_SNAPSHOT_DATE);
		stubBook(EUR, List.of(EUR), expectedDates);
		stubAssets(mockUserId, mockAsset, mockFutureOnlyAsset);
		stubSummarySnapshots(
			mockUserId,
			List.of(mockCurrentSnapshot, mockFutureSnapshot),
			List.of(),
			LATEST_DATA_DATE
		);

		// WHEN
		AnalyticsSummaryDTO expectedSummary = testSubject.getSummary(mockUserId);

		// THEN
		assertThat(expectedSummary.getTotalNetWorth()).isEqualByComparingTo(new BigDecimal("1000"));
		assertThat(expectedSummary.getTotalGrossAssets()).isEqualByComparingTo(new BigDecimal("1000"));
	}

	@Test
	void getSummary_ignoresFutureSnapshots_whenAllocatingAndRankingTopAssets() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, EUR);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Equities"), EUR, "Fund", false);
		Asset mockFutureAsset = buildAsset(
			buildCategory(AssetType.ASSET, "Bonds"),
			EUR,
			"Future",
			false
		);
		Snapshot mockCurrentSnapshot = buildSnapshot(mockAsset, "1000", LATEST_DATA_DATE);
		Snapshot mockFutureSnapshot = buildSnapshot(mockFutureAsset, "5000", FUTURE_SNAPSHOT_DATE);
		List<LocalDate> expectedDates = summaryDates(FUTURE_SNAPSHOT_DATE);
		RateBook mockBook = stubBook(EUR, List.of(EUR), expectedDates);
		AssetDTO stubbedAsset = Instancio.create(AssetDTO.class);
		AssetDTO stubbedFutureAsset = Instancio.create(AssetDTO.class);
		when(
			assetMapper.toDtoLight(
				eq(mockAsset),
				eq(List.of(mockCurrentSnapshot)),
				eq(mockBook),
				eq(TODAY)
			)
		).thenReturn(stubbedAsset);
		when(
			assetMapper.toDtoLight(eq(mockFutureAsset), eq(List.of()), eq(mockBook), eq(TODAY))
		).thenReturn(stubbedFutureAsset);
		stubAssets(mockUserId, mockAsset, mockFutureAsset);
		stubSummarySnapshots(
			mockUserId,
			List.of(mockCurrentSnapshot, mockFutureSnapshot),
			List.of(),
			LATEST_DATA_DATE
		);

		// WHEN
		AnalyticsSummaryDTO expectedSummary = testSubject.getSummary(mockUserId);

		// THEN
		assertThat(expectedSummary.getTotalNetWorth()).isEqualByComparingTo(new BigDecimal("1000"));
		assertThat(expectedSummary.getAssetAllocation()).containsOnlyKeys("Equities");
		assertThat(expectedSummary.getAssetAllocation().get("Equities")).isEqualByComparingTo(
			new BigDecimal("100")
		);
		assertThat(expectedSummary.getTopAssets()).containsExactly(stubbedAsset, stubbedFutureAsset);
	}

	@Test
	void getSummary_breaksSameDateTiesByTheLowestId() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, EUR);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Cash"), EUR, "Ledger", false);
		Snapshot mockHighIdSnapshot = buildSnapshot(mockAsset, "1000", TIE_SNAPSHOT_DATE);
		mockHighIdSnapshot.setId(SNAPSHOT_HIGHEST_ID);
		Snapshot mockLowIdSnapshot = buildSnapshot(mockAsset, "600", TIE_SNAPSHOT_DATE);
		mockLowIdSnapshot.setId(SNAPSHOT_LOWEST_ID);
		List<LocalDate> expectedDates = summaryDates(TIE_SNAPSHOT_DATE);
		stubBook(EUR, List.of(EUR), expectedDates);
		stubAssets(mockUserId, mockAsset);
		stubSummarySnapshots(
			mockUserId,
			List.of(mockHighIdSnapshot, mockLowIdSnapshot),
			List.of(),
			TIE_SNAPSHOT_DATE
		);

		// WHEN
		AnalyticsSummaryDTO expectedSummary = testSubject.getSummary(mockUserId);

		// THEN
		assertThat(expectedSummary.getTotalNetWorth()).isEqualByComparingTo(new BigDecimal("600"));
		assertThat(expectedSummary.getTotalGrossAssets()).isEqualByComparingTo(new BigDecimal("600"));
	}

	@Test
	void getChartData_breaksSameDateTiesByTheLowestId() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, EUR);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Cash"), EUR, "Ledger", false);
		Snapshot mockHighIdSnapshot = buildSnapshot(mockAsset, "1000", CHART_SNAPSHOT_DATE);
		mockHighIdSnapshot.setId(SNAPSHOT_HIGHEST_ID);
		Snapshot mockLowIdSnapshot = buildSnapshot(mockAsset, "600", CHART_SNAPSHOT_DATE);
		mockLowIdSnapshot.setId(SNAPSHOT_LOWEST_ID);
		stubBook(EUR, List.of(EUR), ONE_YEAR_CHART_DATES);
		stubAssets(mockUserId, mockAsset);
		stubSnapshots(mockUserId, mockHighIdSnapshot, mockLowIdSnapshot);

		// WHEN
		ChartDataDTO expectedChart = testSubject.getChartData(mockUserId);

		// THEN
		int tiePoint = ONE_YEAR_CHART_DATES.indexOf(CHART_SNAPSHOT_DATE.withDayOfMonth(31));
		assertThat(expectedChart.getTotalAssetsOnly().get(0)).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(expectedChart.getTotalAssetsOnly().get(tiePoint)).isEqualByComparingTo(
			new BigDecimal("600")
		);
		assertThat(expectedChart.getTotalNetWorth().get(tiePoint)).isEqualByComparingTo(
			new BigDecimal("600")
		);
		assertThat(
			expectedChart.getTotalAssetsOnly().get(ONE_YEAR_CHART_DATES.size() - 1)
		).isEqualByComparingTo(new BigDecimal("600"));
	}

	@Test
	void getSummary_allocatesByTheLowestIdOnASameDateTie() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, EUR);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Equities"), EUR, "Fund", false);
		Asset mockOtherAsset = buildAsset(
			buildCategory(AssetType.ASSET, "Bonds"),
			EUR,
			"Bonds fund",
			false
		);
		Snapshot mockHighIdSnapshot = buildSnapshot(mockAsset, "1000", TIE_SNAPSHOT_DATE);
		mockHighIdSnapshot.setId(SNAPSHOT_HIGHEST_ID);
		Snapshot mockLowIdSnapshot = buildSnapshot(mockAsset, "600", TIE_SNAPSHOT_DATE);
		mockLowIdSnapshot.setId(SNAPSHOT_LOWEST_ID);
		Snapshot mockOtherSnapshot = buildSnapshot(mockOtherAsset, "400", TIE_SNAPSHOT_DATE);
		List<LocalDate> expectedDates = summaryDates(TIE_SNAPSHOT_DATE);
		stubBook(EUR, List.of(EUR), expectedDates);
		stubAssets(mockUserId, mockAsset, mockOtherAsset);
		stubSummarySnapshots(
			mockUserId,
			List.of(mockHighIdSnapshot, mockLowIdSnapshot, mockOtherSnapshot),
			List.of(),
			TIE_SNAPSHOT_DATE
		);

		// WHEN
		AnalyticsSummaryDTO expectedSummary = testSubject.getSummary(mockUserId);

		// THEN
		assertThat(expectedSummary.getAssetAllocation().get("Equities")).isEqualByComparingTo(
			new BigDecimal("60")
		);
		assertThat(expectedSummary.getAssetAllocation().get("Bonds")).isEqualByComparingTo(
			new BigDecimal("40")
		);
	}

	@Test
	void getSummary_ranksTopAssetsByTheLowestIdOnASameDateTie() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, EUR);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Equities"), EUR, "Fund", false);
		Asset mockBiggerAsset = buildAsset(
			buildCategory(AssetType.ASSET, "Bonds"),
			EUR,
			"Bigger fund",
			false
		);
		Snapshot mockHighIdSnapshot = buildSnapshot(mockAsset, "1000", TIE_SNAPSHOT_DATE);
		mockHighIdSnapshot.setId(SNAPSHOT_HIGHEST_ID);
		Snapshot mockLowIdSnapshot = buildSnapshot(mockAsset, "600", TIE_SNAPSHOT_DATE);
		mockLowIdSnapshot.setId(SNAPSHOT_LOWEST_ID);
		Snapshot mockBiggerSnapshot = buildSnapshot(mockBiggerAsset, "800", TIE_SNAPSHOT_DATE);
		List<LocalDate> expectedDates = summaryDates(TIE_SNAPSHOT_DATE);
		RateBook mockBook = stubBook(EUR, List.of(EUR), expectedDates);
		AssetDTO stubbedBiggerAsset = Instancio.create(AssetDTO.class);
		AssetDTO stubbedAsset = Instancio.create(AssetDTO.class);
		when(
			assetMapper.toDtoLight(
				eq(mockBiggerAsset),
				eq(List.of(mockBiggerSnapshot)),
				eq(mockBook),
				eq(TODAY)
			)
		).thenReturn(stubbedBiggerAsset);
		when(
			assetMapper.toDtoLight(
				eq(mockAsset),
				eq(List.of(mockLowIdSnapshot, mockHighIdSnapshot)),
				eq(mockBook),
				eq(TODAY)
			)
		).thenReturn(stubbedAsset);
		stubAssets(mockUserId, mockAsset, mockBiggerAsset);
		stubSummarySnapshots(
			mockUserId,
			List.of(mockHighIdSnapshot, mockLowIdSnapshot, mockBiggerSnapshot),
			List.of(),
			TIE_SNAPSHOT_DATE
		);

		// WHEN
		AnalyticsSummaryDTO expectedSummary = testSubject.getSummary(mockUserId);

		// THEN
		assertThat(expectedSummary.getTopAssets()).containsExactly(stubbedBiggerAsset, stubbedAsset);
	}

	@Test
	void getSummary_computesDebtToAssetRatioOverGrossAssets() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, EUR);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Equities"), EUR, "Fund", false);
		Asset mockLiability = buildLiability(
			buildCategory(AssetType.LIABILITY, "Debts"),
			EUR,
			"Mortgage",
			false
		);
		Snapshot mockAssetSnapshot = buildSnapshot(mockAsset, "3000", LATEST_DATA_DATE);
		Snapshot mockLiabilitySnapshot = buildSnapshot(mockLiability, "1000", LATEST_DATA_DATE);
		List<LocalDate> expectedDates = summaryDates(LATEST_DATA_DATE);
		stubBook(EUR, List.of(EUR), expectedDates);
		stubAssets(mockUserId, mockAsset, mockLiability);
		stubSummarySnapshots(
			mockUserId,
			List.of(mockAssetSnapshot, mockLiabilitySnapshot),
			List.of(),
			LATEST_DATA_DATE
		);

		// WHEN
		AnalyticsSummaryDTO expectedSummary = testSubject.getSummary(mockUserId);

		// THEN
		assertThat(expectedSummary.getTotalGrossAssets()).isEqualByComparingTo(new BigDecimal("3000"));
		assertThat(expectedSummary.getTotalLiabilities()).isEqualByComparingTo(new BigDecimal("1000"));
		assertThat(expectedSummary.getDebtToAssetRatio()).isEqualTo(new BigDecimal("33.3300"));
	}

	@Test
	void getSummary_returnsZeroDebtToAssetRatio_whenThereAreNoGrossAssets() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, EUR);
		Asset mockLiability = buildLiability(
			buildCategory(AssetType.LIABILITY, "Debts"),
			EUR,
			"Mortgage",
			false
		);
		Snapshot mockLiabilitySnapshot = buildSnapshot(mockLiability, "500", LATEST_DATA_DATE);
		List<LocalDate> expectedDates = summaryDates(LATEST_DATA_DATE);
		stubBook(EUR, List.of(EUR), expectedDates);
		stubAssets(mockUserId, mockLiability);
		stubSummarySnapshots(mockUserId, List.of(mockLiabilitySnapshot), List.of(), LATEST_DATA_DATE);

		// WHEN
		AnalyticsSummaryDTO expectedSummary = testSubject.getSummary(mockUserId);

		// THEN
		assertThat(expectedSummary.getTotalGrossAssets()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(expectedSummary.getTotalLiabilities()).isEqualByComparingTo(new BigDecimal("500"));
		assertThat(expectedSummary.getDebtToAssetRatio()).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@Test
	void getSummary_excludesLiabilitiesFromAssetAllocation() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, EUR);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Equities"), EUR, "Fund", false);
		Asset mockLiability = buildLiability(
			buildCategory(AssetType.LIABILITY, "Debts"),
			EUR,
			"Mortgage",
			false
		);
		Snapshot mockAssetSnapshot = buildSnapshot(mockAsset, "1000", LATEST_DATA_DATE);
		Snapshot mockLiabilitySnapshot = buildSnapshot(mockLiability, "500", LATEST_DATA_DATE);
		List<LocalDate> expectedDates = summaryDates(LATEST_DATA_DATE);
		stubBook(EUR, List.of(EUR), expectedDates);
		stubAssets(mockUserId, mockAsset, mockLiability);
		stubSummarySnapshots(
			mockUserId,
			List.of(mockAssetSnapshot, mockLiabilitySnapshot),
			List.of(),
			LATEST_DATA_DATE
		);

		// WHEN
		AnalyticsSummaryDTO expectedSummary = testSubject.getSummary(mockUserId);

		// THEN
		assertThat(expectedSummary.getAssetAllocation()).containsOnlyKeys("Equities");
		assertThat(expectedSummary.getAssetAllocation().get("Equities")).isEqualByComparingTo(
			new BigDecimal("100")
		);
	}

	@Test
	void getSummary_leavesAssetsWithoutSnapshotsOutOfTheAllocationDenominator() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, EUR);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Equities"), EUR, "Fund", false);
		Asset mockUnvaluedAsset = buildAsset(
			buildCategory(AssetType.ASSET, "Bonds"),
			EUR,
			"Unvalued",
			false
		);
		Snapshot mockSnapshot = buildSnapshot(mockAsset, "1000", LATEST_DATA_DATE);
		List<LocalDate> expectedDates = summaryDates(LATEST_DATA_DATE);
		stubBook(EUR, List.of(EUR), expectedDates);
		stubAssets(mockUserId, mockAsset, mockUnvaluedAsset);
		stubSummarySnapshots(mockUserId, List.of(mockSnapshot), List.of(), LATEST_DATA_DATE);

		// WHEN
		AnalyticsSummaryDTO expectedSummary = testSubject.getSummary(mockUserId);

		// THEN
		assertThat(expectedSummary.getAssetAllocation()).containsOnlyKeys("Equities");
		assertThat(expectedSummary.getAssetAllocation().get("Equities")).isEqualByComparingTo(
			new BigDecimal("100")
		);
	}

	@Test
	void getSummary_chargesACurrencyGainOnADebtAsALoss() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, USD);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Equities"), EUR, "Fund", false);
		Asset mockLiability = buildLiability(
			buildCategory(AssetType.LIABILITY, "Debts"),
			EUR,
			"Mortgage",
			false
		);
		Snapshot mockAssetSnapshot = buildSnapshot(mockAsset, "1000", CURRENCY_IMPACT_SNAPSHOT_DATE);
		Snapshot mockLiabilitySnapshot = buildSnapshot(
			mockLiability,
			"400",
			CURRENCY_IMPACT_SNAPSHOT_DATE
		);
		List<LocalDate> expectedDates = summaryDates(CURRENCY_IMPACT_SNAPSHOT_DATE);
		Map<LocalDate, BigDecimal> expectedRates = new HashMap<>();
		for (LocalDate date : expectedDates) {
			expectedRates.put(date, new BigDecimal("1.10"));
		}
		expectedRates.put(TODAY, new BigDecimal("1.40"));
		stubSourceRates(EUR, USD, expectedRates, expectedDates);
		stubBook(USD, List.of(EUR), expectedDates);
		stubAssets(mockUserId, mockAsset, mockLiability);
		stubSummarySnapshots(
			mockUserId,
			List.of(mockAssetSnapshot, mockLiabilitySnapshot),
			List.of(),
			CURRENCY_IMPACT_SNAPSHOT_DATE
		);

		// WHEN
		AnalyticsSummaryDTO expectedSummary = testSubject.getSummary(mockUserId);

		// THEN
		assertThat(expectedSummary.getCurrencyImpact()).isEqualByComparingTo(new BigDecimal("180"));
	}

	@Test
	void getSummary_resolvesSameDateTiesByTheLowestId_whenComputingCurrencyImpact() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, USD);
		Asset mockLiability = buildLiability(
			buildCategory(AssetType.LIABILITY, "Debts"),
			EUR,
			"Mortgage",
			false
		);
		Snapshot mockHighIdSnapshot = buildSnapshot(
			mockLiability,
			"900",
			CURRENCY_IMPACT_SNAPSHOT_DATE
		);
		mockHighIdSnapshot.setId(SNAPSHOT_HIGHEST_ID);
		Snapshot mockLowIdSnapshot = buildSnapshot(mockLiability, "400", CURRENCY_IMPACT_SNAPSHOT_DATE);
		mockLowIdSnapshot.setId(SNAPSHOT_LOWEST_ID);
		List<LocalDate> expectedDates = summaryDates(CURRENCY_IMPACT_SNAPSHOT_DATE);
		Map<LocalDate, BigDecimal> expectedRates = new HashMap<>();
		for (LocalDate date : expectedDates) {
			expectedRates.put(date, new BigDecimal("1.20"));
		}
		expectedRates.put(TODAY, new BigDecimal("1.50"));
		stubSourceRates(EUR, USD, expectedRates, expectedDates);
		stubBook(USD, List.of(EUR), expectedDates);
		stubAssets(mockUserId, mockLiability);
		stubSummarySnapshots(
			mockUserId,
			List.of(mockHighIdSnapshot, mockLowIdSnapshot),
			List.of(),
			CURRENCY_IMPACT_SNAPSHOT_DATE
		);

		// WHEN
		AnalyticsSummaryDTO expectedSummary = testSubject.getSummary(mockUserId);

		// THEN
		assertThat(expectedSummary.getCurrencyImpact()).isEqualByComparingTo(new BigDecimal("-120"));
	}

	@Test
	void getSummary_convertsSnapshotsInTheCurrencyTheyWereWrittenIn() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, USD);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Equities"), EUR, "Fund", false);
		Snapshot mockSnapshot = buildSnapshot(mockAsset, "100", LATEST_DATA_DATE, GBP);
		List<LocalDate> expectedDates = summaryDates(LATEST_DATA_DATE);
		Map<LocalDate, BigDecimal> expectedRates = new HashMap<>();
		for (LocalDate date : expectedDates) {
			expectedRates.put(date, SNAPSHOT_CURRENCY_RATE);
		}
		stubSourceRates(GBP, USD, expectedRates, expectedDates);
		stubBook(USD, List.of(GBP), expectedDates);
		stubAssets(mockUserId, mockAsset);
		stubSummarySnapshots(mockUserId, List.of(mockSnapshot), List.of(), LATEST_DATA_DATE);

		// WHEN
		AnalyticsSummaryDTO expectedSummary = testSubject.getSummary(mockUserId);

		// THEN
		assertThat(expectedSummary.getTotalNetWorth()).isEqualByComparingTo(new BigDecimal("150"));
		assertThat(expectedSummary.getTotalGrossAssets()).isEqualByComparingTo(new BigDecimal("150"));
	}

	@Test
	void getSummary_measuresCurrencyImpact_inTheSnapshotCurrency() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, EUR);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Equities"), USD, "Fund", false);
		Snapshot mockSnapshot = buildSnapshot(mockAsset, "1000", CURRENCY_IMPACT_SNAPSHOT_DATE, EUR);
		List<LocalDate> expectedDates = summaryDates(CURRENCY_IMPACT_SNAPSHOT_DATE);
		stubBook(EUR, List.of(EUR), expectedDates);
		stubAssets(mockUserId, mockAsset);
		stubSummarySnapshots(
			mockUserId,
			List.of(mockSnapshot),
			List.of(),
			CURRENCY_IMPACT_SNAPSHOT_DATE
		);

		// WHEN
		AnalyticsSummaryDTO expectedSummary = testSubject.getSummary(mockUserId);

		// THEN
		assertThat(expectedSummary.getTotalNetWorth()).isEqualByComparingTo(new BigDecimal("1000"));
		assertThat(expectedSummary.getCurrencyImpact()).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@Test
	void getSummary_fallsBackToTheBookTarget_whenNeitherRowNorAssetDeclareACurrency() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, USD);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Equities"), EUR, "Legacy", false);
		mockAsset.setOriginalCurrency(null);
		Snapshot mockSnapshot = buildSnapshot(mockAsset, "4000", LATEST_DATA_DATE);
		List<LocalDate> expectedDates = summaryDates(LATEST_DATA_DATE);
		RateBook mockBook = stubBook(USD, List.of(), expectedDates);
		AssetDTO stubbedTopAsset = Instancio.create(AssetDTO.class);
		when(
			assetMapper.toDtoLight(eq(mockAsset), eq(List.of(mockSnapshot)), eq(mockBook), eq(TODAY))
		).thenReturn(stubbedTopAsset);
		stubAssets(mockUserId, mockAsset);
		stubSummarySnapshots(mockUserId, List.of(mockSnapshot), List.of(), LATEST_DATA_DATE);

		// WHEN
		AnalyticsSummaryDTO expectedSummary = testSubject.getSummary(mockUserId);

		// THEN
		assertThat(expectedSummary.getTotalNetWorth()).isEqualByComparingTo(new BigDecimal("4000"));
		assertThat(expectedSummary.getTotalGrossAssets()).isEqualByComparingTo(new BigDecimal("4000"));
	}

	@Test
	void getSummary_labelsAssetsWithoutCategoryAsOther() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, EUR);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Equities"), EUR, "Fund", false);
		mockAsset.setCategory(null);
		Snapshot mockSnapshot = buildSnapshot(mockAsset, "1000", LATEST_DATA_DATE);
		List<LocalDate> expectedDates = summaryDates(LATEST_DATA_DATE);
		stubBook(EUR, List.of(EUR), expectedDates);
		stubAssets(mockUserId, mockAsset);
		stubSummarySnapshots(mockUserId, List.of(mockSnapshot), List.of(), LATEST_DATA_DATE);

		// WHEN
		AnalyticsSummaryDTO expectedSummary = testSubject.getSummary(mockUserId);

		// THEN
		assertThat(expectedSummary.getAssetAllocation()).containsOnlyKeys(OTHER_CATEGORY_LABEL);
		assertThat(expectedSummary.getAssetAllocation().get(OTHER_CATEGORY_LABEL)).isEqualByComparingTo(
			new BigDecimal("100")
		);
	}

	@Test
	void getSummary_readsOldestSnapshotYearFromTheRepository() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, EUR);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Cash"), EUR, "Fund", false);
		Snapshot mockSnapshot = buildSnapshot(mockAsset, "1000", LATEST_DATA_DATE);
		List<LocalDate> expectedDates = summaryDates(LATEST_DATA_DATE);
		stubBook(EUR, List.of(EUR), expectedDates);
		stubAssets(mockUserId, mockAsset);
		stubSummarySnapshots(mockUserId, List.of(mockSnapshot), List.of(), OLDEST_SNAPSHOT_DATE);

		// WHEN
		AnalyticsSummaryDTO expectedSummary = testSubject.getSummary(mockUserId);

		// THEN
		assertThat(expectedSummary.getOldestSnapshotYear()).isEqualTo(OLDEST_SNAPSHOT_DATE.getYear());
	}

	@Test
	void getSummary_returnsNullOldestSnapshotYear_whenTheQueryIsEmpty() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, EUR);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Cash"), EUR, "Fund", false);
		Snapshot mockSnapshot = buildSnapshot(mockAsset, "1000", LATEST_DATA_DATE);
		List<LocalDate> expectedDates = summaryDates(LATEST_DATA_DATE);
		stubBook(EUR, List.of(EUR), expectedDates);
		stubAssets(mockUserId, mockAsset);
		stubSummarySnapshots(mockUserId, List.of(mockSnapshot), List.of(), null);

		// WHEN
		AnalyticsSummaryDTO expectedSummary = testSubject.getSummary(mockUserId);

		// THEN
		assertThat(expectedSummary.getOldestSnapshotYear()).isNull();
	}

	@Test
	void getSummary_reportsEachVariationAgainstItsOwnAnchorDate() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, EUR);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Cash"), EUR, "Fund", false);
		Snapshot mockLatestSnapshot = buildSnapshot(mockAsset, "1000", LATEST_DATA_DATE);
		Snapshot mockPreviousSnapshot = buildSnapshot(
			mockAsset,
			"500",
			LATEST_DATA_DATE.minusMonths(1)
		);
		List<LocalDate> expectedDates = summaryDates(LATEST_DATA_DATE);
		stubBook(EUR, List.of(EUR), expectedDates);
		stubAssets(mockUserId, mockAsset);
		stubSummarySnapshots(
			mockUserId,
			List.of(mockLatestSnapshot, mockPreviousSnapshot),
			List.of(),
			LATEST_DATA_DATE
		);

		// WHEN
		AnalyticsSummaryDTO expectedSummary = testSubject.getSummary(mockUserId);

		// THEN
		DeltaDTO expectedOneMonth = expectedSummary.getVariations().getOneMonth();
		assertThat(expectedOneMonth.getAbsolute()).isEqualByComparingTo(new BigDecimal("500"));
		assertThat(expectedOneMonth.getPercentage()).isEqualByComparingTo(new BigDecimal("100"));
		assertThat(expectedSummary.getVariations().getSixMonths().getAbsolute()).isEqualByComparingTo(
			new BigDecimal("1000")
		);
		assertThat(expectedSummary.getVariations().getSixMonths().getPercentage()).isEqualByComparingTo(
			BigDecimal.ZERO
		);
	}

	@Test
	void getSummary_convertsTheOneMonthVariationAtItsOwnAnchorRate() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, USD);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Equities"), EUR, "Fund", false);
		Snapshot mockCurrentSnapshot = buildSnapshot(mockAsset, "1000", LATEST_DATA_DATE);
		Snapshot mockAnchorSnapshot = buildSnapshot(mockAsset, "1000", LATEST_DATA_DATE.minusMonths(1));
		List<LocalDate> expectedDates = summaryDates(LATEST_DATA_DATE);
		Map<LocalDate, BigDecimal> expectedRates = new HashMap<>();
		for (LocalDate date : expectedDates) {
			expectedRates.put(date, new BigDecimal("1.20"));
		}
		expectedRates.put(TODAY, new BigDecimal("1.50"));
		stubSourceRates(EUR, USD, expectedRates, expectedDates);
		stubBook(USD, List.of(EUR), expectedDates);
		stubAssets(mockUserId, mockAsset);
		stubSummarySnapshots(
			mockUserId,
			List.of(mockCurrentSnapshot, mockAnchorSnapshot),
			List.of(),
			LATEST_DATA_DATE
		);

		// WHEN
		AnalyticsSummaryDTO expectedSummary = testSubject.getSummary(mockUserId);

		// THEN
		DeltaDTO expectedOneMonth = expectedSummary.getVariations().getOneMonth();
		assertThat(expectedSummary.getTotalNetWorth()).isEqualByComparingTo(new BigDecimal("1500"));
		assertThat(expectedOneMonth.getAbsolute()).isEqualByComparingTo(new BigDecimal("300"));
		assertThat(expectedOneMonth.getPercentage()).isEqualByComparingTo(new BigDecimal("25"));
	}

	@Test
	void getSummary_returnsZeroes_whenThereIsNoData() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, EUR);
		stubBook(EUR, List.of(), summaryDates(TODAY));
		stubAssets(mockUserId);
		stubSummarySnapshots(mockUserId, List.of(), List.of(), null);

		// WHEN
		AnalyticsSummaryDTO expectedSummary = testSubject.getSummary(mockUserId);

		// THEN
		assertThat(expectedSummary.getTotalNetWorth()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(expectedSummary.getTotalGrossAssets()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(expectedSummary.getTotalLiabilities()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(expectedSummary.getDebtToAssetRatio()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(expectedSummary.getCurrencyImpact()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(expectedSummary.getAssetAllocation()).isEmpty();
		assertThat(expectedSummary.getTopAssets()).isEmpty();
		assertThat(expectedSummary.getOldestSnapshotYear()).isNull();
	}

	@Test
	void getSummary_propagatesExchangeRateOutage_whenRatesAreUnavailable() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, USD);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Equities"), EUR, "Fund", false);
		ExchangeRateUnavailableException stubbedOutage = new ExchangeRateUnavailableException(
			EXCHANGE_RATE_OUTAGE_MESSAGE
		);
		when(conversionService.book(eq(USD), eq(List.of()), eq(summaryDates(TODAY)))).thenThrow(
			stubbedOutage
		);
		stubAssets(mockUserId, mockAsset);

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.getSummary(mockUserId)).isSameAs(stubbedOutage);
	}

	@Test
	void getChartData_suffixesASharedFavouriteNameWithTheAssetShortId() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, EUR);
		Asset mockTwinAsset = buildAsset(
			buildCategory(AssetType.ASSET, "Equities"),
			EUR,
			TWIN_NAME,
			true
		);
		mockTwinAsset.setId(TWIN_ASSET_ID);
		Asset mockTwinLiability = buildLiability(
			buildCategory(AssetType.LIABILITY, "Debts"),
			EUR,
			TWIN_NAME,
			true
		);
		mockTwinLiability.setId(TWIN_LIABILITY_ID);
		Asset mockCashAsset = buildAsset(buildCategory(AssetType.ASSET, "Cash"), EUR, CASH_NAME, true);
		Snapshot mockTwinSnapshot = buildSnapshot(mockTwinAsset, "300", CHART_SNAPSHOT_DATE);
		Snapshot mockTwinLiabilitySnapshot = buildSnapshot(
			mockTwinLiability,
			"120",
			CHART_SNAPSHOT_DATE
		);
		Snapshot mockCashSnapshot = buildSnapshot(mockCashAsset, "100", CHART_SNAPSHOT_DATE);
		stubBook(EUR, List.of(EUR), ONE_YEAR_CHART_DATES);
		stubAssets(mockUserId, mockTwinAsset, mockTwinLiability, mockCashAsset);
		stubSnapshots(mockUserId, mockTwinSnapshot, mockTwinLiabilitySnapshot, mockCashSnapshot);

		// WHEN
		ChartDataDTO expectedChart = testSubject.getChartData(mockUserId);

		// THEN
		int lastPoint = ONE_YEAR_CHART_DATES.size() - 1;
		assertThat(expectedChart.getFavoriteAssetsValues()).containsOnlyKeys(
			TWIN_ASSET_SERIES,
			TWIN_LIABILITY_SERIES,
			CASH_NAME
		);
		assertThat(expectedChart.getFavoriteAssetsValues().get(TWIN_ASSET_SERIES)).hasSize(
			expectedChart.getLabels().size()
		);
		assertThat(expectedChart.getFavoriteAssetsValues().get(TWIN_LIABILITY_SERIES)).hasSize(
			expectedChart.getLabels().size()
		);
		assertThat(expectedChart.getFavoriteAssetsValues().get(CASH_NAME)).hasSize(
			expectedChart.getLabels().size()
		);
		assertThat(
			expectedChart.getFavoriteAssetsValues().get(TWIN_ASSET_SERIES).get(lastPoint)
		).isEqualByComparingTo(new BigDecimal("300"));
		assertThat(
			expectedChart.getFavoriteAssetsValues().get(TWIN_LIABILITY_SERIES).get(lastPoint)
		).isEqualByComparingTo(new BigDecimal("-120"));
		assertThat(
			expectedChart.getFavoriteAssetsValues().get(CASH_NAME).get(lastPoint)
		).isEqualByComparingTo(new BigDecimal("100"));
	}

	@Test
	void getChartData_buildsMonthlyLabelsForAllHistory() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, EUR);
		stubBookForAnyDates(EUR, List.of());
		stubAssets(mockUserId);
		stubSnapshots(mockUserId);

		// WHEN
		ChartDataDTO expectedChart = testSubject.getChartData(mockUserId, true);

		// THEN
		assertThat(expectedChart.getLabels()).hasSize(121);
	}

	@Test
	void getChartDataForYear_buildsOnePointPerMonthOfTheYear() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubUserCurrency(mockUserId, EUR);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Equities"), EUR, "Fund", false);
		Snapshot mockSnapshot = buildSnapshot(mockAsset, "1000", HISTORY_SNAPSHOT_DATE);
		stubBook(EUR, List.of(EUR), CHART_2025_DATES);
		stubAssets(mockUserId, mockAsset);
		stubSnapshots(mockUserId, mockSnapshot);

		// WHEN
		ChartDataDTO expectedChart = testSubject.getChartDataForYear(mockUserId, 2025);

		// THEN
		assertThat(expectedChart.getLabels()).hasSize(12);
		assertThat(expectedChart.getLabels().get(0)).isEqualTo("JAN 25");
		assertThat(expectedChart.getLabels().get(11)).isEqualTo("DEC 25");
		assertThat(expectedChart.getTotalAssetsOnly().get(0)).isEqualByComparingTo(
			new BigDecimal("1000")
		);
		assertThat(expectedChart.getTotalNetWorth().get(11)).isEqualByComparingTo(
			new BigDecimal("1000")
		);
	}

	@Test
	void getProjections_returnsOneEntryPerYearUpToTheRequestedCount() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, EUR);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Equities"), EUR, "Fund", false);
		Snapshot mockCurrentSnapshot = buildSnapshot(mockAsset, "2000", LATEST_DATA_DATE);
		Snapshot mockBaselineSnapshot = buildSnapshot(mockAsset, "1000", ONE_YEAR_AGO.minusDays(1));
		stubBook(EUR, List.of(EUR), List.of(TODAY, ONE_YEAR_AGO));
		stubAssets(mockUserId, mockAsset);
		stubSnapshots(mockUserId, mockCurrentSnapshot, mockBaselineSnapshot);

		// WHEN
		Map<Integer, BigDecimal> expectedProjections = testSubject.getProjections(mockUserId, 3, false);

		// THEN
		assertThat(expectedProjections).containsOnlyKeys(1, 2, 3);
		assertThat(expectedProjections.get(1)).isEqualTo(new BigDecimal("4000.00"));
		assertThat(expectedProjections.get(2)).isEqualTo(new BigDecimal("8000.00"));
		assertThat(expectedProjections.get(3)).isEqualTo(new BigDecimal("16000.00"));
	}

	@Test
	void getProjections_projectsGrossAssets_whenOnlyAssetsAreAsked() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, EUR);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Equities"), EUR, "Fund", false);
		Asset mockLiability = buildLiability(
			buildCategory(AssetType.LIABILITY, "Debts"),
			EUR,
			"Mortgage",
			false
		);
		Snapshot mockCurrentSnapshot = buildSnapshot(mockAsset, "1000", LATEST_DATA_DATE);
		Snapshot mockBaselineSnapshot = buildSnapshot(mockAsset, "500", ONE_YEAR_AGO.minusDays(1));
		Snapshot mockCurrentLiabilitySnapshot = buildSnapshot(mockLiability, "200", LATEST_DATA_DATE);
		Snapshot mockBaselineLiabilitySnapshot = buildSnapshot(
			mockLiability,
			"200",
			ONE_YEAR_AGO.minusDays(1)
		);
		stubBook(EUR, List.of(EUR), List.of(TODAY, ONE_YEAR_AGO));
		stubAssets(mockUserId, mockAsset, mockLiability);
		stubSnapshots(
			mockUserId,
			mockCurrentSnapshot,
			mockBaselineSnapshot,
			mockCurrentLiabilitySnapshot,
			mockBaselineLiabilitySnapshot
		);

		// WHEN
		Map<Integer, BigDecimal> expectedProjections = testSubject.getProjections(mockUserId, 2, true);

		// THEN
		assertThat(expectedProjections).containsOnlyKeys(1, 2);
		assertThat(expectedProjections.get(1)).isEqualTo(new BigDecimal("2000.00"));
		assertThat(expectedProjections.get(2)).isEqualTo(new BigDecimal("4000.00"));
	}

	@Test
	void getProjections_returnsEmptyMap_whenYearsIsBelowOne() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();

		// WHEN
		Map<Integer, BigDecimal> expectedProjections = testSubject.getProjections(mockUserId, 0, false);

		// THEN
		assertThat(expectedProjections).isEmpty();
	}

	@Test
	void getProjections_holdsTheCurrentValue_whenTheBaselineIsNotPositive() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, EUR);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Equities"), EUR, "Fund", false);
		Snapshot mockCurrentSnapshot = buildSnapshot(mockAsset, "1000", LATEST_DATA_DATE);
		Snapshot mockBaselineSnapshot = buildSnapshot(mockAsset, "0", ONE_YEAR_AGO.minusDays(1));
		stubBook(EUR, List.of(EUR), List.of(TODAY, ONE_YEAR_AGO));
		stubAssets(mockUserId, mockAsset);
		stubSnapshots(mockUserId, mockCurrentSnapshot, mockBaselineSnapshot);

		// WHEN
		Map<Integer, BigDecimal> expectedProjections = testSubject.getProjections(mockUserId, 2, false);

		// THEN
		assertThat(expectedProjections).containsOnlyKeys(1, 2);
		assertThat(expectedProjections.get(1)).isEqualTo(new BigDecimal("1000.00"));
		assertThat(expectedProjections.get(2)).isEqualTo(new BigDecimal("1000.00"));
	}

	@Test
	void getProjections_holdsTheCurrentValue_whenTheUserIsShrinking() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, EUR);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Equities"), EUR, "Fund", false);
		Snapshot mockCurrentSnapshot = buildSnapshot(mockAsset, "1000", LATEST_DATA_DATE);
		Snapshot mockBaselineSnapshot = buildSnapshot(mockAsset, "1500", ONE_YEAR_AGO.minusDays(1));
		stubBook(EUR, List.of(EUR), List.of(TODAY, ONE_YEAR_AGO));
		stubAssets(mockUserId, mockAsset);
		stubSnapshots(mockUserId, mockCurrentSnapshot, mockBaselineSnapshot);

		// WHEN
		Map<Integer, BigDecimal> expectedProjections = testSubject.getProjections(mockUserId, 2, false);

		// THEN
		assertThat(expectedProjections).containsOnlyKeys(1, 2);
		assertThat(expectedProjections.get(1)).isEqualTo(new BigDecimal("1000.00"));
		assertThat(expectedProjections.get(2)).isEqualTo(new BigDecimal("1000.00"));
	}

	@Test
	void getProjections_returnsZeroes_whenTheCurrentValueIsNotPositive() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, EUR);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Equities"), EUR, "Fund", false);
		Snapshot mockCurrentSnapshot = buildSnapshot(mockAsset, "0", LATEST_DATA_DATE);
		Snapshot mockBaselineSnapshot = buildSnapshot(mockAsset, "1000", ONE_YEAR_AGO.minusDays(1));
		stubBook(EUR, List.of(EUR), List.of(TODAY, ONE_YEAR_AGO));
		stubAssets(mockUserId, mockAsset);
		stubSnapshots(mockUserId, mockCurrentSnapshot, mockBaselineSnapshot);

		// WHEN
		Map<Integer, BigDecimal> expectedProjections = testSubject.getProjections(mockUserId, 2, false);

		// THEN
		assertThat(expectedProjections).containsOnlyKeys(1, 2);
		assertThat(expectedProjections.get(1)).isEqualTo(new BigDecimal("0.00"));
		assertThat(expectedProjections.get(2)).isEqualTo(new BigDecimal("0.00"));
	}

	@Test
	void getNetWorth_isIdentity_whenAssetUsesUserCurrency() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, EUR);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Equities"), EUR, "Fund", false);
		Snapshot mockSnapshot = buildSnapshot(mockAsset, "1000", LATEST_DATA_DATE);
		stubBook(EUR, List.of(EUR), List.of(TODAY));
		stubAssets(mockUserId, mockAsset);
		stubSnapshots(mockUserId, mockSnapshot);

		// WHEN
		BigDecimal expectedNetWorth = testSubject.getNetWorth(mockUserId);

		// THEN
		assertThat(expectedNetWorth).isEqualByComparingTo(new BigDecimal("1000"));
	}

	@Test
	void getNetWorth_fallsBackToUserCurrency_whenAssetHasNoCurrency() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, EUR);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Equities"), null, "Fund", false);
		Snapshot mockSnapshot = buildSnapshot(mockAsset, "1000", LATEST_DATA_DATE);
		stubBook(EUR, List.of(), List.of(TODAY));
		stubAssets(mockUserId, mockAsset);
		stubSnapshots(mockUserId, mockSnapshot);

		// WHEN
		BigDecimal expectedNetWorth = testSubject.getNetWorth(mockUserId);

		// THEN
		assertThat(expectedNetWorth).isEqualByComparingTo(new BigDecimal("1000"));
	}

	@Test
	void getNetWorth_throwsNotFound_whenUserIsMissing() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.getNetWorth(mockUserId)).isInstanceOf(
			NotFoundException.class
		);
	}

	private void stubClockAt(LocalDate date) {
		when(clock.getZone()).thenReturn(ZoneOffset.UTC);
		when(clock.instant()).thenReturn(date.atStartOfDay(ZoneOffset.UTC).toInstant());
	}

	private void stubUserCurrency(UUID userId, Currency currency) {
		User mockUser = Instancio.of(User.class)
			.set(Select.field(User::getId), userId)
			.set(Select.field(User::getCurrency), currency)
			.create();
		when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
	}

	private void stubAssets(UUID userId, Asset... assets) {
		when(assetRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(assets));
	}

	private void stubSnapshots(UUID userId, Snapshot... snapshots) {
		when(snapshotRepository.findByAssetUserId(userId)).thenReturn(List.of(snapshots));
	}

	private void stubSummarySnapshots(
		UUID userId,
		List<Snapshot> windowSnapshots,
		List<Snapshot> carryInSnapshots,
		LocalDate oldestReferenceDate
	) {
		when(
			snapshotRepository.findByAssetUserIdAndReferenceDateGreaterThanEqual(
				eq(userId),
				eq(SUMMARY_WINDOW_START)
			)
		).thenReturn(windowSnapshots);
		when(snapshotRepository.findLatestPerAssetByUserId(userId)).thenReturn(carryInSnapshots);
		when(snapshotRepository.findOldestReferenceDateByAssetUserId(userId)).thenReturn(
			Optional.ofNullable(oldestReferenceDate)
		);
	}

	private void stubSourceRates(
		Currency source,
		Currency target,
		Map<LocalDate, BigDecimal> ratesByDate,
		Collection<LocalDate> requestedDates
	) {
		when(
			exchangeRateProvider.getRates(source.getValue(), target.getValue(), requestedDates)
		).thenReturn(ratesByDate);
	}

	private RateBook stubBook(Currency target, List<Currency> sources, List<LocalDate> dates) {
		RateBook mockBook = buildBook(target, sources, dates);
		when(conversionService.book(eq(target), eq(sources), eq(dates))).thenReturn(mockBook);
		return mockBook;
	}

	private void stubBookForAnyDates(Currency target, List<Currency> sources) {
		when(conversionService.book(eq(target), eq(sources), anyCollection())).thenReturn(
			buildBook(target, sources, List.of())
		);
	}

	private RateBook buildBook(Currency target, List<Currency> sources, Collection<LocalDate> dates) {
		return new CurrencyConversionService(exchangeRateProvider).book(target, sources, dates);
	}

	private List<LocalDate> summaryDates(LocalDate latestDataDate) {
		return List.of(
			TODAY,
			latestDataDate.minusMonths(1),
			latestDataDate.minusMonths(6),
			latestDataDate.minusYears(1),
			CURRENCY_IMPACT_START
		);
	}

	private AssetCategory buildCategory(AssetType type, String name) {
		return Instancio.of(AssetCategory.class)
			.set(Select.field(AssetCategory::getUser), null)
			.set(Select.field(AssetCategory::getType), type)
			.set(Select.field(AssetCategory::getName), name)
			.create();
	}

	private Asset buildAsset(
		AssetCategory category,
		Currency currency,
		String name,
		boolean favorite
	) {
		return buildAsset(category, currency, name, favorite, null);
	}

	private Asset buildLiability(
		AssetCategory category,
		Currency currency,
		String name,
		boolean favorite
	) {
		return buildAsset(category, currency, name, favorite, LiabilityType.MANUAL);
	}

	private Asset buildAsset(
		AssetCategory category,
		Currency currency,
		String name,
		boolean favorite,
		LiabilityType liabilityType
	) {
		return Instancio.of(Asset.class)
			.set(Select.field(Asset::getUser), null)
			.set(Select.field(Asset::getCategory), category)
			.set(Select.field(Asset::getOriginalCurrency), currency)
			.set(Select.field(Asset::getName), name)
			.set(Select.field(Asset::getIsFavorite), favorite)
			.set(Select.field(Asset::getLiabilityType), liabilityType)
			.set(Select.field(Asset::getSnapshots), new ArrayList<>())
			.create();
	}

	private Snapshot buildSnapshot(Asset asset, String amount, LocalDate referenceDate) {
		return buildSnapshot(asset, amount, referenceDate, null);
	}

	private Snapshot buildSnapshot(
		Asset asset,
		String amount,
		LocalDate referenceDate,
		Currency snapshotCurrency
	) {
		return Instancio.of(Snapshot.class)
			.set(Select.field(Snapshot::getAsset), asset)
			.set(Select.field(Snapshot::getAmountOriginalCurrency), new BigDecimal(amount))
			.set(Select.field(Snapshot::getReferenceDate), referenceDate)
			.set(Select.field(Snapshot::getCurrency), snapshotCurrency)
			.set(Select.field(Snapshot::getExchangeRateToBase), STORED_RATE)
			.create();
	}
}
