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
import java.util.Collections;
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
	private static final LocalDate LATEST_SNAPSHOT_DATE = LocalDate.of(2026, 6, 14);
	private static final LocalDate OLDEST_SNAPSHOT_DATE = LocalDate.of(2025, 6, 14);
	private static final LocalDate ONE_MONTH_ANCHOR = LocalDate.of(2026, 5, 14);
	private static final LocalDate SIX_MONTHS_ANCHOR = LocalDate.of(2025, 12, 14);
	private static final LocalDate CURRENCY_IMPACT_START = LocalDate.of(2026, 5, 15);
	private static final LocalDate ONE_YEAR_AGO = LocalDate.of(2025, 6, 15);
	private static final LocalDate JANUARY_SNAPSHOT_DATE = LocalDate.of(2025, 1, 10);
	private static final LocalDate PROJECTION_BASELINE_DATE = LocalDate.of(2024, 12, 31);
	private static final LocalDate FUTURE_SNAPSHOT_DATE = LocalDate.of(2026, 6, 20);
	private static final LocalDate MID_SERIES_SNAPSHOT_DATE = LocalDate.of(2026, 1, 1);

	private static final Currency EUR = Currency.EUR;
	private static final Currency GBP = Currency.GBP;
	private static final Currency USD = Currency.USD;

	private static final BigDecimal STORED_RATE = new BigDecimal("2.0000");

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
	void getNetWorth_convertsIntoUserCurrencyAtToday() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, USD);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Equities"), EUR, "World", false);
		Snapshot mockSnapshot = buildSnapshot(mockAsset, "1000", TODAY.minusDays(10));
		List<LocalDate> expectedDates = List.of(TODAY);
		stubSourceRates(EUR, USD, Map.of(TODAY, new BigDecimal("1.5")), expectedDates);
		stubBook(USD, List.of(EUR), expectedDates);
		when(assetRepository.findByUserIdOrderByCreatedAtDesc(mockUserId)).thenReturn(
			List.of(mockAsset)
		);
		when(snapshotRepository.findByAssetUserId(mockUserId)).thenReturn(List.of(mockSnapshot));

		// WHEN
		BigDecimal expectedNetWorth = testSubject.getNetWorth(mockUserId);

		// THEN
		assertThat(expectedNetWorth).isEqualByComparingTo(new BigDecimal("1500"));
	}

	@Test
	void getNetWorth_isIdentity_whenAssetUsesUserCurrency() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, EUR);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Equities"), EUR, "World", false);
		Snapshot mockSnapshot = buildSnapshot(mockAsset, "1000", TODAY.minusDays(10));
		List<LocalDate> expectedDates = List.of(TODAY);
		stubBook(EUR, List.of(EUR), expectedDates);
		when(assetRepository.findByUserIdOrderByCreatedAtDesc(mockUserId)).thenReturn(
			List.of(mockAsset)
		);
		when(snapshotRepository.findByAssetUserId(mockUserId)).thenReturn(List.of(mockSnapshot));

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
		stubUserCurrency(mockUserId, USD);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Equities"), null, "World", false);
		Snapshot mockSnapshot = buildSnapshot(mockAsset, "1000", TODAY.minusDays(10));
		List<LocalDate> expectedDates = List.of(TODAY);
		stubBook(USD, Collections.singletonList(null), expectedDates);
		when(assetRepository.findByUserIdOrderByCreatedAtDesc(mockUserId)).thenReturn(
			List.of(mockAsset)
		);
		when(snapshotRepository.findByAssetUserId(mockUserId)).thenReturn(List.of(mockSnapshot));

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
		when(assetRepository.findByUserIdOrderByCreatedAtDesc(mockUserId)).thenReturn(List.of());
		when(snapshotRepository.findByAssetUserId(mockUserId)).thenReturn(List.of());
		when(userRepository.findById(mockUserId)).thenReturn(Optional.empty());

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.getNetWorth(mockUserId)).isInstanceOf(
			NotFoundException.class
		);
	}

	@Test
	void getNetWorth_propagatesExchangeRateOutage_whenRatesAreUnavailable() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, USD);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Equities"), EUR, "World", false);
		ExchangeRateUnavailableException stubbedOutage = stubExchangeRateOutage(
			USD,
			EUR,
			List.of(TODAY)
		);
		when(assetRepository.findByUserIdOrderByCreatedAtDesc(mockUserId)).thenReturn(
			List.of(mockAsset)
		);
		when(snapshotRepository.findByAssetUserId(mockUserId)).thenReturn(List.of());

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.getNetWorth(mockUserId)).isSameAs(stubbedOutage);
	}

	@Test
	void getSummary_convertsCurrentTotalsAndEachVariationAtItsOwnDate() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, USD);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Equities"), EUR, "World", false);
		Snapshot mockLatestSnapshot = buildSnapshot(mockAsset, "2000", LATEST_SNAPSHOT_DATE);
		Snapshot mockOldestSnapshot = buildSnapshot(mockAsset, "1000", OLDEST_SNAPSHOT_DATE);
		List<LocalDate> expectedDates = summaryDates(LATEST_SNAPSHOT_DATE);
		stubSourceRates(
			EUR,
			USD,
			Map.of(
				OLDEST_SNAPSHOT_DATE,
				new BigDecimal("1.15"),
				SIX_MONTHS_ANCHOR,
				new BigDecimal("1.25"),
				ONE_MONTH_ANCHOR,
				new BigDecimal("1.2"),
				CURRENCY_IMPACT_START,
				new BigDecimal("1.1"),
				TODAY,
				new BigDecimal("1.5")
			),
			expectedDates
		);
		RateBook mockBook = stubBook(USD, List.of(EUR), expectedDates);
		AssetDTO stubbedTopAsset = Instancio.create(AssetDTO.class);
		when(
			assetMapper.toDtoLight(
				eq(mockAsset),
				eq(List.of(mockLatestSnapshot, mockOldestSnapshot)),
				eq(mockBook),
				eq(TODAY)
			)
		).thenReturn(stubbedTopAsset);
		when(assetRepository.findByUserIdOrderByCreatedAtDesc(mockUserId)).thenReturn(
			List.of(mockAsset)
		);
		when(
			snapshotRepository.findByAssetUserIdAndReferenceDateGreaterThanEqual(
				eq(mockUserId),
				eq(TODAY.minusYears(1).minusMonths(2))
			)
		).thenReturn(List.of(mockLatestSnapshot, mockOldestSnapshot));

		// WHEN
		AnalyticsSummaryDTO expectedSummary = testSubject.getSummary(mockUserId);

		// THEN
		assertThat(expectedSummary.getTotalNetWorth()).isEqualByComparingTo(new BigDecimal("3000"));
		assertThat(expectedSummary.getTotalGrossAssets()).isEqualByComparingTo(new BigDecimal("3000"));
		assertThat(expectedSummary.getVariations().getOneMonth().getAbsolute()).isEqualByComparingTo(
			new BigDecimal("1800")
		);
		assertThat(expectedSummary.getVariations().getOneMonth().getPercentage()).isEqualByComparingTo(
			new BigDecimal("150")
		);
		assertThat(expectedSummary.getVariations().getSixMonths().getAbsolute()).isEqualByComparingTo(
			new BigDecimal("1750")
		);
		assertThat(expectedSummary.getVariations().getOneYear().getAbsolute()).isEqualByComparingTo(
			new BigDecimal("1850")
		);
		assertThat(
			expectedSummary.getAssetVariations().getOneMonth().getAbsolute()
		).isEqualByComparingTo(new BigDecimal("1800"));
		assertThat(expectedSummary.getCurrencyImpact()).isEqualByComparingTo(new BigDecimal("400"));
		assertThat(expectedSummary.getAssetAllocation().get("Equities")).isEqualByComparingTo(
			new BigDecimal("100")
		);
		assertThat(expectedSummary.getTopAssets()).containsExactly(stubbedTopAsset);
		assertThat(expectedSummary.getOldestSnapshotYear()).isEqualTo(OLDEST_SNAPSHOT_DATE.getYear());
	}

	@Test
	void getSummary_subtractsConvertedLiabilitiesFromConvertedAssets() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, USD);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Equities"), EUR, "World", false);
		Asset mockLiability = buildAsset(
			buildCategory(AssetType.LIABILITY, "Debts"),
			EUR,
			"Mortgage",
			false
		);
		Snapshot mockAssetSnapshot = buildSnapshot(mockAsset, "1000", TODAY);
		Snapshot mockLiabilitySnapshot = buildSnapshot(mockLiability, "400", TODAY);
		List<LocalDate> expectedDates = summaryDates(TODAY);
		stubSourceRates(EUR, USD, Map.of(TODAY, new BigDecimal("1.5")), expectedDates);
		stubBook(USD, List.of(EUR), expectedDates);
		when(assetRepository.findByUserIdOrderByCreatedAtDesc(mockUserId)).thenReturn(
			List.of(mockAsset, mockLiability)
		);
		when(
			snapshotRepository.findByAssetUserIdAndReferenceDateGreaterThanEqual(
				eq(mockUserId),
				eq(TODAY.minusYears(1).minusMonths(2))
			)
		).thenReturn(List.of(mockAssetSnapshot, mockLiabilitySnapshot));

		// WHEN
		AnalyticsSummaryDTO expectedSummary = testSubject.getSummary(mockUserId);

		// THEN
		assertThat(expectedSummary.getTotalGrossAssets()).isEqualByComparingTo(new BigDecimal("1500"));
		assertThat(expectedSummary.getTotalLiabilities()).isEqualByComparingTo(new BigDecimal("600"));
		assertThat(expectedSummary.getTotalNetWorth()).isEqualByComparingTo(new BigDecimal("900"));
		assertThat(expectedSummary.getDebtToAssetRatio()).isEqualByComparingTo(new BigDecimal("28.57"));
		assertThat(expectedSummary.getAssetAllocation().get("Equities")).isEqualByComparingTo(
			new BigDecimal("71.43")
		);
		assertThat(expectedSummary.getAssetAllocation().get("Debts")).isEqualByComparingTo(
			new BigDecimal("28.57")
		);
		assertThat(expectedSummary.getVariations().getOneMonth().getPercentage()).isEqualByComparingTo(
			BigDecimal.ZERO
		);
		assertThat(
			expectedSummary.getAssetVariations().getOneMonth().getAbsolute()
		).isEqualByComparingTo(new BigDecimal("1500"));
	}

	@Test
	void getSummary_returnsZeroes_whenThereIsNoData() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, USD);
		List<LocalDate> expectedDates = summaryDates(TODAY);
		stubBook(USD, List.of(), expectedDates);
		when(assetRepository.findByUserIdOrderByCreatedAtDesc(mockUserId)).thenReturn(List.of());
		when(
			snapshotRepository.findByAssetUserIdAndReferenceDateGreaterThanEqual(
				eq(mockUserId),
				eq(TODAY.minusYears(1).minusMonths(2))
			)
		).thenReturn(List.of());

		// WHEN
		AnalyticsSummaryDTO expectedSummary = testSubject.getSummary(mockUserId);

		// THEN
		assertThat(expectedSummary.getTotalNetWorth()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(expectedSummary.getTotalLiabilities()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(expectedSummary.getDebtToAssetRatio()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(expectedSummary.getCurrencyImpact()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(expectedSummary.getAssetAllocation()).isEmpty();
		assertThat(expectedSummary.getTopAssets()).isEmpty();
		assertThat(expectedSummary.getOldestSnapshotYear()).isNull();
		DeltaDTO expectedOneMonth = expectedSummary.getVariations().getOneMonth();
		assertThat(expectedOneMonth.getAbsolute()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(expectedOneMonth.getPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@Test
	void getSummary_propagatesExchangeRateOutage_whenRatesAreUnavailable() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, USD);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Equities"), EUR, "World", false);
		ExchangeRateUnavailableException stubbedOutage = stubExchangeRateOutage(
			USD,
			EUR,
			summaryDates(TODAY)
		);
		when(assetRepository.findByUserIdOrderByCreatedAtDesc(mockUserId)).thenReturn(
			List.of(mockAsset)
		);
		when(
			snapshotRepository.findByAssetUserIdAndReferenceDateGreaterThanEqual(
				eq(mockUserId),
				eq(TODAY.minusYears(1).minusMonths(2))
			)
		).thenReturn(List.of());

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.getSummary(mockUserId)).isSameAs(stubbedOutage);
	}

	@Test
	void getSummary_ordersTopAssetsByConvertedValue() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, USD);
		Asset mockEuroAsset = buildAsset(
			buildCategory(AssetType.ASSET, "Equities"),
			EUR,
			"Euro fund",
			false
		);
		Asset mockPoundAsset = buildAsset(
			buildCategory(AssetType.ASSET, "Bonds"),
			GBP,
			"UK fund",
			false
		);
		Asset mockLiability = buildAsset(
			buildCategory(AssetType.LIABILITY, "Debts"),
			EUR,
			"Mortgage",
			false
		);
		Asset mockUncategorized = buildAsset(null, EUR, "Loose cash", false);
		Asset mockUntracked = buildAsset(
			buildCategory(AssetType.ASSET, "Equities"),
			EUR,
			"Untracked",
			false
		);
		Snapshot mockEuroSnapshot = buildSnapshot(mockEuroAsset, "100", TODAY);
		Snapshot mockPoundSnapshot = buildSnapshot(mockPoundAsset, "200", TODAY);
		Snapshot mockLiabilitySnapshot = buildSnapshot(mockLiability, "500", TODAY);
		Snapshot mockUncategorizedSnapshot = buildSnapshot(mockUncategorized, "50", TODAY);
		List<LocalDate> expectedDates = summaryDates(TODAY);
		stubSourceRates(EUR, USD, Map.of(TODAY, new BigDecimal("3.0")), expectedDates);
		stubSourceRates(GBP, USD, Map.of(TODAY, BigDecimal.ONE), expectedDates);
		RateBook mockBook = stubBook(USD, List.of(EUR, GBP), expectedDates);
		AssetDTO stubbedEuroAsset = Instancio.create(AssetDTO.class);
		AssetDTO stubbedPoundAsset = Instancio.create(AssetDTO.class);
		AssetDTO stubbedUntrackedAsset = Instancio.create(AssetDTO.class);
		when(
			assetMapper.toDtoLight(
				eq(mockEuroAsset),
				eq(List.of(mockEuroSnapshot)),
				eq(mockBook),
				eq(TODAY)
			)
		).thenReturn(stubbedEuroAsset);
		when(
			assetMapper.toDtoLight(
				eq(mockPoundAsset),
				eq(List.of(mockPoundSnapshot)),
				eq(mockBook),
				eq(TODAY)
			)
		).thenReturn(stubbedPoundAsset);
		when(
			assetMapper.toDtoLight(eq(mockUntracked), eq(List.of()), eq(mockBook), eq(TODAY))
		).thenReturn(stubbedUntrackedAsset);
		when(assetRepository.findByUserIdOrderByCreatedAtDesc(mockUserId)).thenReturn(
			List.of(mockEuroAsset, mockPoundAsset, mockLiability, mockUncategorized, mockUntracked)
		);
		when(
			snapshotRepository.findByAssetUserIdAndReferenceDateGreaterThanEqual(
				eq(mockUserId),
				eq(TODAY.minusYears(1).minusMonths(2))
			)
		).thenReturn(
			List.of(mockEuroSnapshot, mockPoundSnapshot, mockLiabilitySnapshot, mockUncategorizedSnapshot)
		);

		// WHEN
		AnalyticsSummaryDTO expectedSummary = testSubject.getSummary(mockUserId);

		// THEN
		assertThat(expectedSummary.getTopAssets()).containsExactly(
			stubbedEuroAsset,
			stubbedPoundAsset,
			stubbedUntrackedAsset
		);
		assertThat(expectedSummary.getAssetAllocation().get("Other")).isEqualByComparingTo(
			new BigDecimal("6.98")
		);
	}

	@Test
	void getChartData_convertsEveryMonthEndAtItsOwnDate() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, USD);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Equities"), EUR, "World", false);
		Snapshot mockSnapshot = buildSnapshot(mockAsset, "1000", JANUARY_SNAPSHOT_DATE);
		stubSourceRates(
			EUR,
			USD,
			Map.of(ONE_YEAR_CHART_DATES.get(0), new BigDecimal("1.1"), TODAY, new BigDecimal("1.5")),
			ONE_YEAR_CHART_DATES
		);
		stubBook(USD, List.of(EUR), ONE_YEAR_CHART_DATES);
		when(assetRepository.findByUserIdOrderByCreatedAtDesc(mockUserId)).thenReturn(
			List.of(mockAsset)
		);
		when(snapshotRepository.findByAssetUserId(mockUserId)).thenReturn(List.of(mockSnapshot));

		// WHEN
		ChartDataDTO expectedChart = testSubject.getChartData(mockUserId, false);

		// THEN
		assertThat(expectedChart.getLabels()).hasSize(ONE_YEAR_CHART_DATES.size());
		assertThat(expectedChart.getLabels().get(0)).isEqualTo("JUN 25");
		assertThat(expectedChart.getLabels().get(ONE_YEAR_CHART_DATES.size() - 1)).isEqualTo("JUN 26");
		assertThat(expectedChart.getTotalAssetsOnly().get(0)).isEqualByComparingTo(
			new BigDecimal("1100")
		);
		assertThat(
			expectedChart.getTotalAssetsOnly().get(ONE_YEAR_CHART_DATES.size() - 1)
		).isEqualByComparingTo(new BigDecimal("1500"));
		assertThat(expectedChart.getTotalNetWorth().get(0)).isEqualByComparingTo(
			new BigDecimal("1100")
		);
	}

	@Test
	void getChartData_buildsMonthlyLabelsForAllHistory() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, USD);
		when(conversionService.book(eq(USD), eq(List.of()), anyCollection())).thenReturn(
			buildBook(USD, List.of(), List.of())
		);
		when(assetRepository.findByUserIdOrderByCreatedAtDesc(mockUserId)).thenReturn(List.of());
		when(snapshotRepository.findByAssetUserId(mockUserId)).thenReturn(List.of());

		// WHEN
		ChartDataDTO expectedChart = testSubject.getChartData(mockUserId, true);

		// THEN
		assertThat(expectedChart.getLabels()).hasSize(121);
	}

	@Test
	void getChartData_propagatesExchangeRateOutage_whenRatesAreUnavailable() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, USD);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Equities"), EUR, "World", false);
		ExchangeRateUnavailableException stubbedOutage = stubExchangeRateOutage(
			USD,
			EUR,
			ONE_YEAR_CHART_DATES
		);
		when(assetRepository.findByUserIdOrderByCreatedAtDesc(mockUserId)).thenReturn(
			List.of(mockAsset)
		);
		when(snapshotRepository.findByAssetUserId(mockUserId)).thenReturn(List.of());

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.getChartData(mockUserId, false)).isSameAs(stubbedOutage);
	}

	@Test
	void getChartDataForYear_buildsOnePointPerMonthOfTheYear() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubUserCurrency(mockUserId, EUR);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Equities"), EUR, "World", false);
		Snapshot mockSnapshot = buildSnapshot(mockAsset, "1000", PROJECTION_BASELINE_DATE);
		stubBook(EUR, List.of(EUR), CHART_2025_DATES);
		when(assetRepository.findByUserIdOrderByCreatedAtDesc(mockUserId)).thenReturn(
			List.of(mockAsset)
		);
		when(snapshotRepository.findByAssetUserId(mockUserId)).thenReturn(List.of(mockSnapshot));

		// WHEN
		ChartDataDTO expectedChart = testSubject.getChartDataForYear(mockUserId, 2025);

		// THEN
		assertThat(expectedChart.getLabels()).hasSize(12);
		assertThat(expectedChart.getLabels().get(0)).isEqualTo("JAN 25");
		assertThat(expectedChart.getLabels().get(11)).isEqualTo("DEC 25");
		assertThat(expectedChart.getTotalAssetsOnly().get(0)).isEqualByComparingTo(
			new BigDecimal("1000")
		);
		assertThat(expectedChart.getTotalAssetsOnly().get(11)).isEqualByComparingTo(
			new BigDecimal("1000")
		);
	}

	@Test
	void getChartData_convertsFavouriteSeriesAndNegatesLiabilities() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, USD);
		Asset mockFavouriteAsset = buildAsset(
			buildCategory(AssetType.ASSET, "Equities"),
			EUR,
			"Favourite asset",
			true
		);
		Asset mockFavouriteLiability = buildAsset(
			buildCategory(AssetType.LIABILITY, "Debts"),
			EUR,
			"Favourite liability",
			true
		);
		Asset mockFavouriteUncategorized = buildAsset(null, EUR, "Favourite cash", true);
		Asset mockFavouriteWithoutSnapshots = buildAsset(
			buildCategory(AssetType.ASSET, "Equities"),
			EUR,
			"Favourite empty",
			true
		);
		Asset mockFavouriteLate = buildAsset(
			buildCategory(AssetType.ASSET, "Equities"),
			EUR,
			"Favourite late",
			true
		);
		Asset mockPlainAsset = buildAsset(
			buildCategory(AssetType.ASSET, "Equities"),
			EUR,
			"Plain",
			false
		);
		Snapshot mockAssetSnapshot = buildSnapshot(mockFavouriteAsset, "500", JANUARY_SNAPSHOT_DATE);
		Snapshot mockLiabilitySnapshot = buildSnapshot(
			mockFavouriteLiability,
			"300",
			JANUARY_SNAPSHOT_DATE
		);
		Snapshot mockUncategorizedSnapshot = buildSnapshot(
			mockFavouriteUncategorized,
			"100",
			JANUARY_SNAPSHOT_DATE
		);
		Snapshot mockPlainSnapshot = buildSnapshot(mockPlainAsset, "999", JANUARY_SNAPSHOT_DATE);
		Snapshot mockLateSnapshot = buildSnapshot(mockFavouriteLate, "700", MID_SERIES_SNAPSHOT_DATE);
		stubSourceRates(
			EUR,
			USD,
			Map.of(ONE_YEAR_CHART_DATES.get(0), BigDecimal.ONE),
			ONE_YEAR_CHART_DATES
		);
		stubBook(USD, List.of(EUR), ONE_YEAR_CHART_DATES);
		when(assetRepository.findByUserIdOrderByCreatedAtDesc(mockUserId)).thenReturn(
			List.of(
				mockFavouriteAsset,
				mockFavouriteLiability,
				mockFavouriteUncategorized,
				mockFavouriteWithoutSnapshots,
				mockFavouriteLate,
				mockPlainAsset
			)
		);
		when(snapshotRepository.findByAssetUserId(mockUserId)).thenReturn(
			List.of(
				mockAssetSnapshot,
				mockLiabilitySnapshot,
				mockUncategorizedSnapshot,
				mockPlainSnapshot,
				mockLateSnapshot
			)
		);

		// WHEN
		ChartDataDTO expectedChart = testSubject.getChartData(mockUserId);

		// THEN
		assertThat(
			expectedChart.getFavoriteAssetsValues().get("Favourite asset").get(0)
		).isEqualByComparingTo(new BigDecimal("500"));
		assertThat(
			expectedChart.getFavoriteAssetsValues().get("Favourite liability").get(0)
		).isEqualByComparingTo(new BigDecimal("-300"));
		assertThat(
			expectedChart.getFavoriteAssetsValues().get("Favourite cash").get(0)
		).isEqualByComparingTo(new BigDecimal("100"));
		assertThat(
			expectedChart.getFavoriteAssetsValues().get("Favourite empty").get(0)
		).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(
			expectedChart.getFavoriteAssetsValues().get("Favourite late").get(0)
		).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(
			expectedChart
				.getFavoriteAssetsValues()
				.get("Favourite late")
				.get(ONE_YEAR_CHART_DATES.size() - 1)
		).isEqualByComparingTo(new BigDecimal("700"));
		assertThat(expectedChart.getFavoriteAssetsTypes()).containsEntry(
			"Favourite liability",
			"LIABILITY"
		);
		assertThat(expectedChart.getFavoriteAssetsTypes()).containsEntry("Favourite asset", "ASSET");
		assertThat(expectedChart.getTotalNetWorth().get(0)).isEqualByComparingTo(
			new BigDecimal("1299")
		);
		assertThat(expectedChart.getTotalAssetsOnly().get(0)).isEqualByComparingTo(
			new BigDecimal("1499")
		);
	}

	@Test
	void getProjections_convertsCurrentValueAtTodayAndBaselineOneYearAgo() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, USD);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Equities"), EUR, "World", false);
		Snapshot mockCurrentSnapshot = buildSnapshot(mockAsset, "2000", LATEST_SNAPSHOT_DATE);
		Snapshot mockBaselineSnapshot = buildSnapshot(mockAsset, "1000", OLDEST_SNAPSHOT_DATE);
		List<LocalDate> expectedDates = List.of(TODAY, ONE_YEAR_AGO);
		stubSourceRates(
			EUR,
			USD,
			Map.of(
				OLDEST_SNAPSHOT_DATE,
				new BigDecimal("1.2"),
				LATEST_SNAPSHOT_DATE,
				new BigDecimal("1.5")
			),
			expectedDates
		);
		stubBook(USD, List.of(EUR), expectedDates);
		when(assetRepository.findByUserIdOrderByCreatedAtDesc(mockUserId)).thenReturn(
			List.of(mockAsset)
		);
		when(snapshotRepository.findByAssetUserId(mockUserId)).thenReturn(
			List.of(mockCurrentSnapshot, mockBaselineSnapshot)
		);

		// WHEN
		Map<Integer, BigDecimal> expectedProjections = testSubject.getProjections(
			mockUserId,
			10,
			false
		);

		// THEN
		assertThat(expectedProjections).containsOnlyKeys(1, 5, 10);
		assertThat(expectedProjections.get(1)).isEqualByComparingTo(new BigDecimal("7500"));
		assertThat(expectedProjections.get(5)).isEqualByComparingTo(new BigDecimal("292968.75"));
		assertThat(expectedProjections.get(10)).isEqualByComparingTo(new BigDecimal("28610229.49"));
	}

	@Test
	void getProjections_returnsCurrentValue_whenTheUserIsShrinking() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, EUR);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Equities"), EUR, "World", false);
		Snapshot mockCurrentSnapshot = buildSnapshot(mockAsset, "1000", LATEST_SNAPSHOT_DATE);
		Snapshot mockBaselineSnapshot = buildSnapshot(mockAsset, "1500", OLDEST_SNAPSHOT_DATE);
		List<LocalDate> expectedDates = List.of(TODAY, ONE_YEAR_AGO);
		stubBook(EUR, List.of(EUR), expectedDates);
		when(assetRepository.findByUserIdOrderByCreatedAtDesc(mockUserId)).thenReturn(
			List.of(mockAsset)
		);
		when(snapshotRepository.findByAssetUserId(mockUserId)).thenReturn(
			List.of(mockCurrentSnapshot, mockBaselineSnapshot)
		);

		// WHEN
		Map<Integer, BigDecimal> expectedProjections = testSubject.getProjections(
			mockUserId,
			10,
			false
		);

		// THEN
		assertThat(expectedProjections.get(1)).isEqualByComparingTo(new BigDecimal("1000"));
		assertThat(expectedProjections.get(5)).isEqualByComparingTo(new BigDecimal("1000"));
		assertThat(expectedProjections.get(10)).isEqualByComparingTo(new BigDecimal("1000"));
	}

	@Test
	void getProjections_returnsZeroes_whenThereIsNothingToProject() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, EUR);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Equities"), EUR, "World", false);
		Snapshot mockSnapshot = buildSnapshot(mockAsset, "0", LATEST_SNAPSHOT_DATE);
		List<LocalDate> expectedDates = List.of(TODAY, ONE_YEAR_AGO);
		stubBook(EUR, List.of(EUR), expectedDates);
		when(assetRepository.findByUserIdOrderByCreatedAtDesc(mockUserId)).thenReturn(
			List.of(mockAsset)
		);
		when(snapshotRepository.findByAssetUserId(mockUserId)).thenReturn(List.of(mockSnapshot));

		// WHEN
		Map<Integer, BigDecimal> expectedProjections = testSubject.getProjections(mockUserId, 10, true);

		// THEN
		assertThat(expectedProjections.get(1)).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(expectedProjections.get(5)).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(expectedProjections.get(10)).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@Test
	void getProjections_returnsCurrentValue_whenThereIsNoBaselineToCompare() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, EUR);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Equities"), EUR, "World", false);
		Snapshot mockCurrentSnapshot = buildSnapshot(mockAsset, "1000", LATEST_SNAPSHOT_DATE);
		List<LocalDate> expectedDates = List.of(TODAY, ONE_YEAR_AGO);
		stubBook(EUR, List.of(EUR), expectedDates);
		when(assetRepository.findByUserIdOrderByCreatedAtDesc(mockUserId)).thenReturn(
			List.of(mockAsset)
		);
		when(snapshotRepository.findByAssetUserId(mockUserId)).thenReturn(List.of(mockCurrentSnapshot));

		// WHEN
		Map<Integer, BigDecimal> expectedProjections = testSubject.getProjections(
			mockUserId,
			10,
			false
		);

		// THEN
		assertThat(expectedProjections.get(1)).isEqualByComparingTo(new BigDecimal("1000"));
		assertThat(expectedProjections.get(5)).isEqualByComparingTo(new BigDecimal("1000"));
		assertThat(expectedProjections.get(10)).isEqualByComparingTo(new BigDecimal("1000"));
	}

	@Test
	void getProjections_propagatesExchangeRateOutage_whenRatesAreUnavailable() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, USD);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Equities"), EUR, "World", false);
		ExchangeRateUnavailableException stubbedOutage = stubExchangeRateOutage(
			USD,
			EUR,
			List.of(TODAY, ONE_YEAR_AGO)
		);
		when(assetRepository.findByUserIdOrderByCreatedAtDesc(mockUserId)).thenReturn(
			List.of(mockAsset)
		);
		when(snapshotRepository.findByAssetUserId(mockUserId)).thenReturn(List.of());

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.getProjections(mockUserId, 10, false)).isSameAs(
			stubbedOutage
		);
	}

	@Test
	void getSummary_ignoresSnapshotDatedAfterToday() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		stubClockAt(TODAY);
		stubUserCurrency(mockUserId, EUR);
		Asset mockAsset = buildAsset(buildCategory(AssetType.ASSET, "Equities"), EUR, "World", false);
		Snapshot mockFutureSnapshot = buildSnapshot(mockAsset, "1000", FUTURE_SNAPSHOT_DATE);
		List<LocalDate> expectedDates = summaryDates(FUTURE_SNAPSHOT_DATE);
		RateBook mockBook = stubBook(EUR, List.of(EUR), expectedDates);
		AssetDTO stubbedTopAsset = Instancio.create(AssetDTO.class);
		when(
			assetMapper.toDtoLight(
				eq(mockAsset),
				eq(List.of(mockFutureSnapshot)),
				eq(mockBook),
				eq(TODAY)
			)
		).thenReturn(stubbedTopAsset);
		when(assetRepository.findByUserIdOrderByCreatedAtDesc(mockUserId)).thenReturn(
			List.of(mockAsset)
		);
		when(
			snapshotRepository.findByAssetUserIdAndReferenceDateGreaterThanEqual(
				eq(mockUserId),
				eq(TODAY.minusYears(1).minusMonths(2))
			)
		).thenReturn(List.of(mockFutureSnapshot));

		// WHEN
		AnalyticsSummaryDTO expectedSummary = testSubject.getSummary(mockUserId);

		// THEN
		assertThat(expectedSummary.getTotalNetWorth()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(expectedSummary.getCurrencyImpact()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(expectedSummary.getAssetAllocation().get("Equities")).isEqualByComparingTo(
			new BigDecimal("100")
		);
		assertThat(expectedSummary.getTopAssets()).containsExactly(stubbedTopAsset);
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

	private ExchangeRateUnavailableException stubExchangeRateOutage(
		Currency target,
		Currency source,
		List<LocalDate> dates
	) {
		ExchangeRateUnavailableException stubbedOutage = new ExchangeRateUnavailableException(
			EXCHANGE_RATE_OUTAGE_MESSAGE
		);
		when(exchangeRateProvider.getRates(source.getValue(), target.getValue(), dates)).thenThrow(
			stubbedOutage
		);
		when(conversionService.book(eq(target), eq(List.of(source)), eq(dates))).thenAnswer(
			invocation ->
				buildBook(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2))
		);
		return stubbedOutage;
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
			TODAY.minusMonths(1)
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
		return Instancio.of(Asset.class)
			.set(Select.field(Asset::getUser), null)
			.set(Select.field(Asset::getCategory), category)
			.set(Select.field(Asset::getOriginalCurrency), currency)
			.set(Select.field(Asset::getName), name)
			.set(Select.field(Asset::getIsFavorite), favorite)
			.set(Select.field(Asset::getSnapshots), new ArrayList<>())
			.create();
	}

	private Snapshot buildSnapshot(Asset asset, String amount, LocalDate referenceDate) {
		return Instancio.of(Snapshot.class)
			.set(Select.field(Snapshot::getAsset), asset)
			.set(Select.field(Snapshot::getAmountOriginalCurrency), new BigDecimal(amount))
			.set(Select.field(Snapshot::getReferenceDate), referenceDate)
			.set(Select.field(Snapshot::getExchangeRateToBase), STORED_RATE)
			.create();
	}
}
