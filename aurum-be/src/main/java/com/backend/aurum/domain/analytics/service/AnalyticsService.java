package com.backend.aurum.domain.analytics.service;

import com.backend.aurum.domain.analytics.dto.AnalyticsSummaryDTO;
import com.backend.aurum.domain.analytics.dto.ChartDataDTO;
import com.backend.aurum.domain.analytics.dto.DeltaDTO;
import com.backend.aurum.domain.analytics.dto.VariationDTO;
import com.backend.aurum.domain.asset.dto.AssetDTO;
import com.backend.aurum.domain.asset.mapper.AssetMapper;
import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.AssetType;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.asset.repository.SnapshotRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AnalyticsService {

	private final SnapshotRepository snapshotRepository;
	private final AssetRepository assetRepository;
	private final AssetMapper assetMapper;

	public AnalyticsSummaryDTO getSummary(UUID userId) {
		log.debug("AnalyticsService#getSummary - Computing analytics summary for userId={}", userId);
		LocalDate now = LocalDate.now();

		List<Asset> assets = assetRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId);
		Map<UUID, List<Snapshot>> snapshotsByAsset = loadSnapshotsByAsset(userId);

		BigDecimal currentNetWorth = calculateNetWorthAt(assets, snapshotsByAsset, now);
		BigDecimal oneMonthAgoNetWorth = calculateNetWorthAt(
			assets,
			snapshotsByAsset,
			now.minusMonths(1)
		);
		BigDecimal sixMonthsAgoNetWorth = calculateNetWorthAt(
			assets,
			snapshotsByAsset,
			now.minusMonths(6)
		);
		BigDecimal oneYearAgoNetWorth = calculateNetWorthAt(
			assets,
			snapshotsByAsset,
			now.minusYears(1)
		);

		BigDecimal totalLiabilities = calculateTotalLiabilities(assets, snapshotsByAsset);
		BigDecimal totalGrossAssets = calculateTotalGrossAssets(assets, snapshotsByAsset);

		BigDecimal currentGrossAssets = calculateGrossAssetsAt(assets, snapshotsByAsset, now);
		BigDecimal oneMonthAgoGrossAssets = calculateGrossAssetsAt(
			assets,
			snapshotsByAsset,
			now.minusMonths(1)
		);
		BigDecimal sixMonthsAgoGrossAssets = calculateGrossAssetsAt(
			assets,
			snapshotsByAsset,
			now.minusMonths(6)
		);
		BigDecimal oneYearAgoGrossAssets = calculateGrossAssetsAt(
			assets,
			snapshotsByAsset,
			now.minusYears(1)
		);

		log.debug(
			"AnalyticsService#getSummary - Summary computed for userId={}: netWorth={}, grossAssets={}, liabilities={}",
			userId,
			currentNetWorth,
			currentGrossAssets,
			totalLiabilities
		);
		Integer oldestSnapshotYear = snapshotsByAsset
			.values()
			.stream()
			.flatMap(List::stream)
			.map(s -> s.getReferenceDate().getYear())
			.min(Integer::compareTo)
			.orElse(null);

		return AnalyticsSummaryDTO.builder()
			.totalNetWorth(currentNetWorth)
			.totalGrossAssets(currentGrossAssets)
			.variations(
				VariationDTO.builder()
					.oneMonth(calculateDelta(currentNetWorth, oneMonthAgoNetWorth))
					.sixMonths(calculateDelta(currentNetWorth, sixMonthsAgoNetWorth))
					.oneYear(calculateDelta(currentNetWorth, oneYearAgoNetWorth))
					.build()
			)
			.assetVariations(
				VariationDTO.builder()
					.oneMonth(calculateDelta(currentGrossAssets, oneMonthAgoGrossAssets))
					.sixMonths(calculateDelta(currentGrossAssets, sixMonthsAgoGrossAssets))
					.oneYear(calculateDelta(currentGrossAssets, oneYearAgoGrossAssets))
					.build()
			)
			.assetAllocation(calculateAssetAllocation(assets, snapshotsByAsset))
			.currencyImpact(calculateCurrencyImpact(assets, snapshotsByAsset, now.minusMonths(1), now))
			.totalLiabilities(totalLiabilities)
			.debtToAssetRatio(calculateDebtToAssetRatio(totalLiabilities, totalGrossAssets))
			.topAssets(calculateTopAssets(assets, snapshotsByAsset, 5))
			.oldestSnapshotYear(oldestSnapshotYear)
			.build();
	}

	public BigDecimal getNetWorth(UUID userId) {
		log.debug("AnalyticsService#getNetWorth - Computing net worth for userId={}", userId);
		List<Asset> assets = assetRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId);
		Map<UUID, List<Snapshot>> snapshotsByAsset = loadSnapshotsByAsset(userId);
		return calculateNetWorthAt(assets, snapshotsByAsset, LocalDate.now());
	}

	public ChartDataDTO getChartData(UUID userId) {
		return getChartData(userId, false);
	}

	public ChartDataDTO getChartDataForYear(UUID userId, int year) {
		log.debug(
			"AnalyticsService#getChartDataForYear - Building chart data for userId={}, year={}",
			userId,
			year
		);
		LocalDate start = LocalDate.of(year, 1, 1);
		LocalDate end = LocalDate.of(year, 12, 31);
		return getChartDataForPeriod(userId, start, end);
	}

	public ChartDataDTO getChartData(UUID userId, boolean allHistory) {
		log.debug(
			"AnalyticsService#getChartData - Building chart data for userId={}, allHistory={}",
			userId,
			allHistory
		);
		LocalDate end = LocalDate.now();
		LocalDate start = allHistory ? end.minusYears(10) : end.minusYears(1);
		return getChartDataForPeriod(userId, start, end);
	}

	private ChartDataDTO getChartDataForPeriod(UUID userId, LocalDate start, LocalDate end) {
		log.debug(
			"AnalyticsService#getChartDataForPeriod - Computing chart data for userId={} from {} to {}",
			userId,
			start,
			end
		);
		List<Asset> assets = assetRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId);
		Map<UUID, List<Snapshot>> snapshotsByAsset = loadSnapshotsByAsset(userId);

		List<Asset> favoriteAssets = assets.stream().filter(Asset::getIsFavorite).toList();

		List<String> labels = new ArrayList<>();
		List<BigDecimal> totalSeries = new ArrayList<>();
		List<BigDecimal> assetsOnlySeries = new ArrayList<>();
		Map<String, List<BigDecimal>> favoriteSeriesValues = new HashMap<>();
		Map<String, String> favoriteAssetsTypes = new LinkedHashMap<>();
		for (Asset asset : favoriteAssets) {
			favoriteSeriesValues.put(asset.getName(), new ArrayList<>());
			boolean isLiability =
				asset.getCategory() != null && asset.getCategory().getType() == AssetType.LIABILITY;
			favoriteAssetsTypes.put(asset.getName(), isLiability ? "LIABILITY" : "ASSET");
		}

		LocalDate current = start;
		while (!current.isAfter(end)) {
			labels.add(formatDateLabel(current));
			totalSeries.add(calculateNetWorthAt(assets, snapshotsByAsset, current));
			assetsOnlySeries.add(calculateGrossAssetsAt(assets, snapshotsByAsset, current));
			for (Asset asset : favoriteAssets) {
				List<Snapshot> snapshots = snapshotsByAsset.getOrDefault(asset.getId(), List.of());
				BigDecimal value = calculateAssetValueAt(snapshots, current);
				boolean isLiability =
					asset.getCategory() != null && asset.getCategory().getType() == AssetType.LIABILITY;
				if (isLiability) {
					value = value.negate();
				}
				favoriteSeriesValues.get(asset.getName()).add(value);
			}
			current = current.plusMonths(1);
		}

		return ChartDataDTO.builder()
			.labels(labels)
			.totalNetWorth(totalSeries)
			.totalAssetsOnly(assetsOnlySeries)
			.favoriteAssetsValues(favoriteSeriesValues)
			.favoriteAssetsTypes(favoriteAssetsTypes)
			.build();
	}

	private String formatDateLabel(LocalDate date) {
		String month = date.getMonth().toString().substring(0, 3).toUpperCase();
		String year = String.format("%02d", date.getYear() % 100);
		return month + " " + year;
	}

	public Map<Integer, BigDecimal> getProjections(UUID userId, int years, boolean assetsOnly) {
		log.debug(
			"AnalyticsService#getProjections - Computing projections for userId={}, years={}, assetsOnly={}",
			userId,
			years,
			assetsOnly
		);
		List<Asset> assets = assetRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId);
		Map<UUID, List<Snapshot>> snapshotsByAsset = loadSnapshotsByAsset(userId);

		BigDecimal current = assetsOnly
			? calculateGrossAssetsAt(assets, snapshotsByAsset, LocalDate.now())
			: calculateNetWorthAt(assets, snapshotsByAsset, LocalDate.now());
		BigDecimal oneYearAgo = assetsOnly
			? calculateGrossAssetsAt(assets, snapshotsByAsset, LocalDate.now().minusYears(1))
			: calculateNetWorthAt(assets, snapshotsByAsset, LocalDate.now().minusYears(1));

		BigDecimal growth = current.subtract(oneYearAgo);
		if (growth.compareTo(BigDecimal.ZERO) < 0) growth = BigDecimal.ZERO;

		Map<Integer, BigDecimal> projections = new LinkedHashMap<>();
		projections.put(1, current.add(growth));
		projections.put(5, current.add(growth.multiply(BigDecimal.valueOf(5))));
		projections.put(10, current.add(growth.multiply(BigDecimal.valueOf(10))));

		return projections;
	}

	// --- Private helpers ---

	private Map<UUID, List<Snapshot>> loadSnapshotsByAsset(UUID userId) {
		return snapshotRepository
			.findByAssetUserId(userId)
			.stream()
			.collect(Collectors.groupingBy(s -> s.getAsset().getId()));
	}

	private BigDecimal calculateNetWorthAt(
		List<Asset> assets,
		Map<UUID, List<Snapshot>> snapshotsByAsset,
		LocalDate date
	) {
		BigDecimal total = BigDecimal.ZERO;
		for (Asset asset : assets) {
			List<Snapshot> snapshots = snapshotsByAsset.getOrDefault(asset.getId(), List.of());
			Optional<Snapshot> latest = snapshots
				.stream()
				.filter(s -> !s.getReferenceDate().isAfter(date))
				.max(Comparator.comparing(Snapshot::getReferenceDate));
			if (latest.isPresent()) {
				BigDecimal value = latest.get().getAmountInBaseCurrency();
				if (asset.getCategory() != null && asset.getCategory().getType() == AssetType.LIABILITY) {
					total = total.subtract(value);
				} else {
					total = total.add(value);
				}
			}
		}
		return total;
	}

	private BigDecimal calculateGrossAssetsAt(
		List<Asset> assets,
		Map<UUID, List<Snapshot>> snapshotsByAsset,
		LocalDate date
	) {
		BigDecimal total = BigDecimal.ZERO;
		for (Asset asset : assets) {
			if (asset.getCategory() == null || asset.getCategory().getType() != AssetType.ASSET) continue;
			List<Snapshot> snapshots = snapshotsByAsset.getOrDefault(asset.getId(), List.of());
			Optional<Snapshot> latest = snapshots
				.stream()
				.filter(s -> !s.getReferenceDate().isAfter(date))
				.max(Comparator.comparing(Snapshot::getReferenceDate));
			if (latest.isPresent()) {
				total = total.add(latest.get().getAmountInBaseCurrency());
			}
		}
		return total;
	}

	private BigDecimal calculateAssetValueAt(List<Snapshot> snapshots, LocalDate date) {
		return snapshots
			.stream()
			.filter(s -> !s.getReferenceDate().isAfter(date))
			.max(Comparator.comparing(Snapshot::getReferenceDate))
			.map(Snapshot::getAmountInBaseCurrency)
			.orElse(BigDecimal.ZERO);
	}

	private BigDecimal calculateTotalLiabilities(
		List<Asset> assets,
		Map<UUID, List<Snapshot>> snapshotsByAsset
	) {
		BigDecimal total = BigDecimal.ZERO;
		for (Asset asset : assets) {
			if (
				asset.getCategory() == null || asset.getCategory().getType() != AssetType.LIABILITY
			) continue;
			total = total.add(
				getLatestSnapshotValue(snapshotsByAsset.getOrDefault(asset.getId(), List.of()))
			);
		}
		return total;
	}

	private BigDecimal calculateTotalGrossAssets(
		List<Asset> assets,
		Map<UUID, List<Snapshot>> snapshotsByAsset
	) {
		BigDecimal total = BigDecimal.ZERO;
		for (Asset asset : assets) {
			if (asset.getCategory() == null || asset.getCategory().getType() != AssetType.ASSET) continue;
			total = total.add(
				getLatestSnapshotValue(snapshotsByAsset.getOrDefault(asset.getId(), List.of()))
			);
		}
		return total;
	}

	private BigDecimal calculateDebtToAssetRatio(BigDecimal liabilities, BigDecimal grossAssets) {
		BigDecimal denominator = liabilities.add(grossAssets);
		if (denominator.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
		return liabilities
			.divide(denominator, 4, RoundingMode.HALF_UP)
			.multiply(BigDecimal.valueOf(100));
	}

	private DeltaDTO calculateDelta(BigDecimal current, BigDecimal previous) {
		if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
			return DeltaDTO.builder().absolute(current).percentage(BigDecimal.ZERO).build();
		}
		BigDecimal absolute = current.subtract(previous);
		BigDecimal percentage = absolute
			.divide(previous.abs(), 4, RoundingMode.HALF_UP)
			.multiply(BigDecimal.valueOf(100));
		return DeltaDTO.builder().absolute(absolute).percentage(percentage).build();
	}

	private Map<String, BigDecimal> calculateAssetAllocation(
		List<Asset> assets,
		Map<UUID, List<Snapshot>> snapshotsByAsset
	) {
		Map<String, BigDecimal> categoryValues = new HashMap<>();
		BigDecimal totalAbsoluteValue = BigDecimal.ZERO;

		for (Asset asset : assets) {
			List<Snapshot> snapshots = snapshotsByAsset.getOrDefault(asset.getId(), List.of());
			Optional<Snapshot> latest = snapshots
				.stream()
				.max(Comparator.comparing(Snapshot::getReferenceDate));
			if (latest.isPresent()) {
				String category = asset.getCategory() != null ? asset.getCategory().getName() : "Other";
				BigDecimal value = latest.get().getAmountInBaseCurrency().abs();
				categoryValues.merge(category, value, BigDecimal::add);
				totalAbsoluteValue = totalAbsoluteValue.add(value);
			}
		}

		if (totalAbsoluteValue.compareTo(BigDecimal.ZERO) == 0) return Map.of();

		Map<String, BigDecimal> allocation = new HashMap<>();
		for (Map.Entry<String, BigDecimal> entry : categoryValues.entrySet()) {
			BigDecimal percentage = entry
				.getValue()
				.divide(totalAbsoluteValue, 4, RoundingMode.HALF_UP)
				.multiply(BigDecimal.valueOf(100));
			allocation.put(entry.getKey(), percentage);
		}
		return allocation;
	}

	private BigDecimal calculateCurrencyImpact(
		List<Asset> assets,
		Map<UUID, List<Snapshot>> snapshotsByAsset,
		LocalDate start,
		LocalDate end
	) {
		BigDecimal impact = BigDecimal.ZERO;

		for (Asset asset : assets) {
			List<Snapshot> snapshots = snapshotsByAsset.getOrDefault(asset.getId(), List.of());

			Optional<Snapshot> snapshotEnd = snapshots
				.stream()
				.filter(s -> !s.getReferenceDate().isAfter(end))
				.max(Comparator.comparing(Snapshot::getReferenceDate));

			Optional<Snapshot> snapshotStart = snapshots
				.stream()
				.filter(s -> !s.getReferenceDate().isAfter(start))
				.max(Comparator.comparing(Snapshot::getReferenceDate));

			if (snapshotEnd.isPresent() && snapshotStart.isPresent()) {
				BigDecimal endAmount = snapshotEnd.get().getAmountOriginalCurrency();
				BigDecimal endRate = snapshotEnd.get().getExchangeRateToBase();
				BigDecimal startRate = snapshotStart.get().getExchangeRateToBase();

				impact = impact.add(endAmount.multiply(endRate).subtract(endAmount.multiply(startRate)));
			}
		}
		return impact;
	}

	private List<AssetDTO> calculateTopAssets(
		List<Asset> assets,
		Map<UUID, List<Snapshot>> snapshotsByAsset,
		int limit
	) {
		// Build a map with the latest 2 snapshots per asset (desc by date),
		// matching what AssetController does for the main asset list.
		Map<UUID, List<Snapshot>> latestTwoByAsset = new HashMap<>();
		for (Map.Entry<UUID, List<Snapshot>> entry : snapshotsByAsset.entrySet()) {
			List<Snapshot> sorted = entry
				.getValue()
				.stream()
				.sorted(Comparator.comparing(Snapshot::getReferenceDate).reversed())
				.limit(2)
				.toList();
			latestTwoByAsset.put(entry.getKey(), sorted);
		}

		return assets
			.stream()
			.filter(a -> a.getCategory() != null && a.getCategory().getType() != AssetType.LIABILITY)
			.sorted((a, b) -> {
				BigDecimal valA = getLatestSnapshotValue(
					snapshotsByAsset.getOrDefault(a.getId(), List.of())
				).abs();
				BigDecimal valB = getLatestSnapshotValue(
					snapshotsByAsset.getOrDefault(b.getId(), List.of())
				).abs();
				return valB.compareTo(valA);
			})
			.limit(limit)
			.map(a -> assetMapper.toDtoLight(a, latestTwoByAsset))
			.toList();
	}

	private BigDecimal getLatestSnapshotValue(List<Snapshot> snapshots) {
		return snapshots
			.stream()
			.max(Comparator.comparing(Snapshot::getReferenceDate))
			.map(Snapshot::getAmountInBaseCurrency)
			.orElse(BigDecimal.ZERO);
	}
}
