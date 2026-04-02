package com.backend.aurum.domain.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.analytics.dto.AnalyticsSummaryDTO;
import com.backend.aurum.domain.analytics.dto.ChartDataDTO;
import com.backend.aurum.domain.asset.mapper.AssetMapper;
import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.AssetCategory;
import com.backend.aurum.domain.asset.model.AssetStatusLog;
import com.backend.aurum.domain.asset.model.AssetType;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.asset.repository.AssetStatusLogRepository;
import com.backend.aurum.domain.asset.repository.SnapshotRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

	@Mock
	private SnapshotRepository snapshotRepository;

	@Mock
	private AssetRepository assetRepository;

	@Mock
	private AssetStatusLogRepository statusLogRepository;

	@Mock
	private AssetMapper assetMapper;

	@InjectMocks
	private AnalyticsService testSubject;

	private AssetCategory buildCategory(AssetType type) {
		AssetCategory cat = new AssetCategory();
		cat.setId(UUID.randomUUID());
		cat.setType(type);
		cat.setName(type.name());
		return cat;
	}

	private Asset buildAsset(AssetCategory category) {
		Asset asset = new Asset();
		asset.setId(UUID.randomUUID());
		asset.setCategory(category);
		asset.setIsActive(true);
		asset.setIsFavorite(false);
		asset.setSnapshots(new ArrayList<>());
		return asset;
	}

	private Snapshot buildSnapshot(Asset asset, BigDecimal amount, LocalDate date) {
		Snapshot snapshot = new Snapshot();
		snapshot.setId(UUID.randomUUID());
		snapshot.setAsset(asset);
		snapshot.setAmountOriginalCurrency(amount);
		snapshot.setExchangeRateToBase(BigDecimal.ONE);
		snapshot.setReferenceDate(date);
		return snapshot;
	}

	private AssetStatusLog buildStatusLog(Asset asset, boolean isActive, LocalDate changedAt) {
		AssetStatusLog log = new AssetStatusLog();
		log.setId(UUID.randomUUID());
		log.setAsset(asset);
		log.setIsActive(isActive);
		log.setChangedAt(changedAt);
		return log;
	}

	@Test
	void getNetWorth_returnsZero_whenNoAssets() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		when(assetRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(mockUserId)).thenReturn(
			List.of()
		);
		when(snapshotRepository.findByAssetUserId(mockUserId)).thenReturn(List.of());

		// WHEN
		BigDecimal expectedNetWorth = testSubject.getNetWorth(mockUserId);

		// THEN
		assertThat(expectedNetWorth).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@Test
	void getNetWorth_sumsAssetValues() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		AssetCategory mockCategory = buildCategory(AssetType.ASSET);
		Asset mockAsset = buildAsset(mockCategory);
		Snapshot mockSnapshot = buildSnapshot(
			mockAsset,
			new BigDecimal("1000"),
			LocalDate.now().minusDays(1)
		);
		when(assetRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(mockUserId)).thenReturn(
			List.of(mockAsset)
		);
		when(snapshotRepository.findByAssetUserId(mockUserId)).thenReturn(List.of(mockSnapshot));

		// WHEN
		BigDecimal expectedNetWorth = testSubject.getNetWorth(mockUserId);

		// THEN
		assertThat(expectedNetWorth).isEqualByComparingTo(new BigDecimal("1000"));
	}

	@Test
	void getNetWorth_subtractsLiabilityValues() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		AssetCategory mockAssetCat = buildCategory(AssetType.ASSET);
		AssetCategory mockLiabilityCat = buildCategory(AssetType.LIABILITY);
		Asset mockAsset = buildAsset(mockAssetCat);
		Asset mockLiability = buildAsset(mockLiabilityCat);
		Snapshot mockAssetSnapshot = buildSnapshot(
			mockAsset,
			new BigDecimal("2000"),
			LocalDate.now().minusDays(1)
		);
		Snapshot mockLiabilitySnapshot = buildSnapshot(
			mockLiability,
			new BigDecimal("500"),
			LocalDate.now().minusDays(1)
		);
		when(assetRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(mockUserId)).thenReturn(
			List.of(mockAsset, mockLiability)
		);
		when(snapshotRepository.findByAssetUserId(mockUserId)).thenReturn(
			List.of(mockAssetSnapshot, mockLiabilitySnapshot)
		);

		// WHEN
		BigDecimal expectedNetWorth = testSubject.getNetWorth(mockUserId);

		// THEN
		assertThat(expectedNetWorth).isEqualByComparingTo(new BigDecimal("1500"));
	}

	@Test
	void getNetWorth_ignoresAssetsWithNoSnapshots() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		AssetCategory mockCategory = buildCategory(AssetType.ASSET);
		Asset mockAsset = buildAsset(mockCategory);
		when(assetRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(mockUserId)).thenReturn(
			List.of(mockAsset)
		);
		when(snapshotRepository.findByAssetUserId(mockUserId)).thenReturn(List.of());

		// WHEN
		BigDecimal expectedNetWorth = testSubject.getNetWorth(mockUserId);

		// THEN
		assertThat(expectedNetWorth).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@Test
	void getNetWorth_usesCarryForward_whenSnapshotPredatesQuery() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		AssetCategory mockCategory = buildCategory(AssetType.ASSET);
		Asset mockAsset = buildAsset(mockCategory);
		Snapshot mockOldSnapshot = buildSnapshot(
			mockAsset,
			new BigDecimal("800"),
			LocalDate.now().minusMonths(3)
		);
		when(assetRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(mockUserId)).thenReturn(
			List.of(mockAsset)
		);
		when(snapshotRepository.findByAssetUserId(mockUserId)).thenReturn(List.of(mockOldSnapshot));

		// WHEN
		BigDecimal expectedNetWorth = testSubject.getNetWorth(mockUserId);

		// THEN
		assertThat(expectedNetWorth).isEqualByComparingTo(new BigDecimal("800"));
	}

	@Test
	void getSummary_returnsNonNullSummaryWithVariations() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		AssetCategory mockCategory = buildCategory(AssetType.ASSET);
		Asset mockAsset = buildAsset(mockCategory);
		Snapshot mockSnapshot = buildSnapshot(
			mockAsset,
			new BigDecimal("1000"),
			LocalDate.now().minusDays(5)
		);
		when(assetRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(mockUserId)).thenReturn(
			List.of(mockAsset)
		);
		when(
			snapshotRepository.findByAssetUserIdAndReferenceDateGreaterThanEqual(
				eq(mockUserId),
				any(LocalDate.class)
			)
		).thenReturn(List.of(mockSnapshot));
		when(assetMapper.toDtoLight(eq(mockAsset), anyMap())).thenReturn(
			new com.backend.aurum.domain.asset.dto.AssetDTO()
		);

		// WHEN
		AnalyticsSummaryDTO expectedSummary = testSubject.getSummary(mockUserId);

		// THEN
		assertThat(expectedSummary).isNotNull();
		assertThat(expectedSummary.getTotalNetWorth()).isNotNull();
		assertThat(expectedSummary.getVariations()).isNotNull();
	}

	@Test
	void getSummary_returnsZeroNetWorth_whenNoData() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		when(assetRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(mockUserId)).thenReturn(
			List.of()
		);
		when(
			snapshotRepository.findByAssetUserIdAndReferenceDateGreaterThanEqual(
				eq(mockUserId),
				any(LocalDate.class)
			)
		).thenReturn(List.of());

		// WHEN
		AnalyticsSummaryDTO expectedSummary = testSubject.getSummary(mockUserId);

		// THEN
		assertThat(expectedSummary.getTotalNetWorth()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(expectedSummary.getTotalLiabilities()).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@Test
	void getChartData_buildsMonthlyLabelsForOneYear() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		when(assetRepository.findByUserIdOrderByCreatedAtDesc(mockUserId)).thenReturn(List.of());
		when(snapshotRepository.findByAssetUserId(mockUserId)).thenReturn(List.of());
		when(statusLogRepository.findByAssetIdIn(List.of())).thenReturn(List.of());

		// WHEN
		ChartDataDTO expectedChart = testSubject.getChartData(mockUserId, false);

		// THEN
		assertThat(expectedChart.getLabels()).isNotNull();
		assertThat(expectedChart.getLabels()).hasSize(13);
	}

	@Test
	void getChartData_buildsLabelsForAllHistory_whenFlagIsTrue() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		when(assetRepository.findByUserIdOrderByCreatedAtDesc(mockUserId)).thenReturn(List.of());
		when(snapshotRepository.findByAssetUserId(mockUserId)).thenReturn(List.of());
		when(statusLogRepository.findByAssetIdIn(List.of())).thenReturn(List.of());

		// WHEN
		ChartDataDTO expectedChart = testSubject.getChartData(mockUserId, true);

		// THEN
		assertThat(expectedChart.getLabels()).hasSizeGreaterThan(12);
	}

	@Test
	void getChartDataForYear_builds12Labels() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		int mockYear = 2025;
		when(assetRepository.findByUserIdOrderByCreatedAtDesc(mockUserId)).thenReturn(List.of());
		when(snapshotRepository.findByAssetUserId(mockUserId)).thenReturn(List.of());
		when(statusLogRepository.findByAssetIdIn(List.of())).thenReturn(List.of());

		// WHEN
		ChartDataDTO expectedChart = testSubject.getChartDataForYear(mockUserId, mockYear);

		// THEN
		assertThat(expectedChart.getLabels()).hasSize(12);
	}

	@Test
	void getChartData_excludesArchivedAssetWithNoStatusLog_fromCurrentMonthBar() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		AssetCategory mockCategory = buildCategory(AssetType.ASSET);
		Asset mockArchivedAsset = buildAsset(mockCategory);
		mockArchivedAsset.setIsActive(false);
		Snapshot mockSnapshot = buildSnapshot(
			mockArchivedAsset,
			new BigDecimal("1000"),
			LocalDate.now().minusDays(1)
		);
		when(assetRepository.findByUserIdOrderByCreatedAtDesc(mockUserId)).thenReturn(
			List.of(mockArchivedAsset)
		);
		when(snapshotRepository.findByAssetUserId(mockUserId)).thenReturn(List.of(mockSnapshot));
		when(statusLogRepository.findByAssetIdIn(List.of(mockArchivedAsset.getId()))).thenReturn(
			List.of()
		);

		// WHEN
		ChartDataDTO expectedChart = testSubject.getChartData(mockUserId, false);

		// THEN — current month bar (last entry) must be zero since the asset is archived
		List<BigDecimal> assetsOnly = expectedChart.getTotalAssetsOnly();
		assertThat(assetsOnly.get(assetsOnly.size() - 1)).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@Test
	void getChartData_includesArchivedAssetWithNoStatusLog_inHistoricalBars() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		AssetCategory mockCategory = buildCategory(AssetType.ASSET);
		Asset mockArchivedAsset = buildAsset(mockCategory);
		mockArchivedAsset.setIsActive(false);
		// snapshot from 6 months ago — falls in a historical bar
		Snapshot mockOldSnapshot = buildSnapshot(
			mockArchivedAsset,
			new BigDecimal("1000"),
			LocalDate.now().minusMonths(6)
		);
		when(assetRepository.findByUserIdOrderByCreatedAtDesc(mockUserId)).thenReturn(
			List.of(mockArchivedAsset)
		);
		when(snapshotRepository.findByAssetUserId(mockUserId)).thenReturn(List.of(mockOldSnapshot));
		when(statusLogRepository.findByAssetIdIn(List.of(mockArchivedAsset.getId()))).thenReturn(
			List.of()
		);

		// WHEN
		ChartDataDTO expectedChart = testSubject.getChartData(mockUserId, false);

		// THEN — the bar 7 from the end covers ~6 months ago and must include the archived asset
		List<BigDecimal> assetsOnly = expectedChart.getTotalAssetsOnly();
		assertThat(assetsOnly.get(assetsOnly.size() - 7)).isGreaterThan(BigDecimal.ZERO);
	}

	@Test
	void getChartData_includesActiveAsset_inCurrentMonthBar_evenWhenStatusLogSaysInactive() {
		// GIVEN — asset is currently active (isActive=true) but has a stale log saying inactive
		UUID mockUserId = UUID.randomUUID();
		AssetCategory mockCategory = buildCategory(AssetType.ASSET);
		Asset mockAsset = buildAsset(mockCategory);
		mockAsset.setIsActive(true);
		Snapshot mockSnapshot = buildSnapshot(
			mockAsset,
			new BigDecimal("1000"),
			LocalDate.now().minusDays(1)
		);
		AssetStatusLog staleLog = buildStatusLog(mockAsset, false, LocalDate.now().minusDays(30));
		when(assetRepository.findByUserIdOrderByCreatedAtDesc(mockUserId)).thenReturn(
			List.of(mockAsset)
		);
		when(snapshotRepository.findByAssetUserId(mockUserId)).thenReturn(List.of(mockSnapshot));
		when(statusLogRepository.findByAssetIdIn(List.of(mockAsset.getId()))).thenReturn(
			List.of(staleLog)
		);

		// WHEN
		ChartDataDTO expectedChart = testSubject.getChartData(mockUserId, false);

		// THEN — current month bar must include the active asset despite the stale log
		List<BigDecimal> assetsOnly = expectedChart.getTotalAssetsOnly();
		assertThat(assetsOnly.get(assetsOnly.size() - 1)).isGreaterThan(BigDecimal.ZERO);
	}

	@Test
	void getProjections_returnsPositiveValues_whenNetWorthIsGrowing() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		AssetCategory mockCategory = buildCategory(AssetType.ASSET);
		Asset mockAsset = buildAsset(mockCategory);
		Snapshot mockCurrentSnapshot = buildSnapshot(
			mockAsset,
			new BigDecimal("2000"),
			LocalDate.now().minusDays(1)
		);
		Snapshot mockOldSnapshot = buildSnapshot(
			mockAsset,
			new BigDecimal("1000"),
			LocalDate.now().minusYears(1).minusDays(1)
		);
		when(assetRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(mockUserId)).thenReturn(
			List.of(mockAsset)
		);
		when(snapshotRepository.findByAssetUserId(mockUserId)).thenReturn(
			List.of(mockCurrentSnapshot, mockOldSnapshot)
		);

		// WHEN
		var expectedProjections = testSubject.getProjections(mockUserId, 10, false);

		// THEN
		assertThat(expectedProjections).containsKeys(1, 5, 10);
		assertThat(expectedProjections.get(1)).isGreaterThan(BigDecimal.ZERO);
	}

	@Test
	void getProjections_returnsCurrentValueForAll_whenNoGrowth() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		AssetCategory mockCategory = buildCategory(AssetType.ASSET);
		Asset mockAsset = buildAsset(mockCategory);
		Snapshot mockCurrentSnapshot = buildSnapshot(
			mockAsset,
			new BigDecimal("1000"),
			LocalDate.now().minusDays(1)
		);
		Snapshot mockOldSnapshot = buildSnapshot(
			mockAsset,
			new BigDecimal("1500"),
			LocalDate.now().minusYears(1).minusDays(1)
		);
		when(assetRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(mockUserId)).thenReturn(
			List.of(mockAsset)
		);
		when(snapshotRepository.findByAssetUserId(mockUserId)).thenReturn(
			List.of(mockCurrentSnapshot, mockOldSnapshot)
		);

		// WHEN
		var expectedProjections = testSubject.getProjections(mockUserId, 10, false);

		// THEN
		assertThat(expectedProjections.get(1)).isEqualByComparingTo(new BigDecimal("1000"));
	}
}
