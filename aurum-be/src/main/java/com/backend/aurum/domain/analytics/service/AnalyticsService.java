package com.backend.aurum.domain.analytics.service;

import com.backend.aurum.domain.analytics.dto.AnalyticsSummaryDTO;
import com.backend.aurum.domain.analytics.dto.ChartDataDTO;
import com.backend.aurum.domain.analytics.dto.DeltaDTO;
import com.backend.aurum.domain.analytics.dto.VariationDTO;
import com.backend.aurum.domain.asset.dto.AssetDTO;
import com.backend.aurum.domain.asset.mapper.AssetMapper;
import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.AssetStatusLog;
import com.backend.aurum.domain.asset.model.AssetType;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.asset.repository.AssetStatusLogRepository;
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
	private final AssetStatusLogRepository statusLogRepository;
	private final AssetMapper assetMapper;

	public AnalyticsSummaryDTO getSummary(UUID userId) {
		log.debug("AnalyticsService#getSummary - Computing analytics summary for userId={}", userId);
		LocalDate now = LocalDate.now();

		List<Asset> assets = assetRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId);

		// Only load snapshots from the oldest needed date (1 year back) instead of all history.
		// Add a 1-month buffer so carry-forward logic works for the earliest variation date.
		LocalDate earliestNeeded = now.minusYears(1).minusMonths(2);
		Map<UUID, List<Snapshot>> snapshotsByAsset = loadSnapshotsByAsset(userId, earliestNeeded);

		// Anchor variations at the latest snapshot date so that months without data
		// don't collapse the delta to zero via carry-forward.
		// e.g. if latest snapshot is Feb 2026 and today is Mar 2026:
		//   1M = Feb vs Jan, not (Mar carry-forward=Feb) vs (Feb carry-forward=Feb)
		LocalDate latestDataDate = snapshotsByAsset
			.values()
			.stream()
			.flatMap(List::stream)
			.map(Snapshot::getReferenceDate)
			.max(LocalDate::compareTo)
			.orElse(now);

		// Compute net worth and gross assets at all 4 dates in a single pass
		// instead of calling calculateNetWorthAt/calculateGrossAssetsAt 8 times.
		LocalDate oneMonthAgoDate = latestDataDate.minusMonths(1);
		LocalDate sixMonthsAgoDate = latestDataDate.minusMonths(6);
		LocalDate oneYearAgoDate = latestDataDate.minusYears(1);
		List<LocalDate> dates = List.of(now, oneMonthAgoDate, sixMonthsAgoDate, oneYearAgoDate);

		Map<LocalDate, BigDecimal> netWorthByDate = calculateNetWorthAtDates(
			assets,
			snapshotsByAsset,
			dates
		);
		Map<LocalDate, BigDecimal> grossAssetsByDate = calculateGrossAssetsAtDates(
			assets,
			snapshotsByAsset,
			dates
		);

		BigDecimal currentNetWorth = netWorthByDate.get(now);
		BigDecimal currentGrossAssets = grossAssetsByDate.get(now);
		BigDecimal totalLiabilities = currentGrossAssets.subtract(currentNetWorth);
		BigDecimal totalGrossAssets = currentGrossAssets;

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
					.oneMonth(calculateDelta(currentNetWorth, netWorthByDate.get(oneMonthAgoDate)))
					.sixMonths(calculateDelta(currentNetWorth, netWorthByDate.get(sixMonthsAgoDate)))
					.oneYear(calculateDelta(currentNetWorth, netWorthByDate.get(oneYearAgoDate)))
					.build()
			)
			.assetVariations(
				VariationDTO.builder()
					.oneMonth(calculateDelta(currentGrossAssets, grossAssetsByDate.get(oneMonthAgoDate)))
					.sixMonths(calculateDelta(currentGrossAssets, grossAssetsByDate.get(sixMonthsAgoDate)))
					.oneYear(calculateDelta(currentGrossAssets, grossAssetsByDate.get(oneYearAgoDate)))
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
		List<Asset> allAssets = assetRepository.findByUserIdOrderByCreatedAtDesc(userId);
		Map<UUID, List<Snapshot>> snapshotsByAsset = loadSnapshotsByAsset(userId);

		List<UUID> assetIds = allAssets.stream().map(Asset::getId).toList();
		Map<UUID, List<AssetStatusLog>> statusLogsByAsset = statusLogRepository
			.findByAssetIdIn(assetIds)
			.stream()
			.collect(Collectors.groupingBy(l -> l.getAsset().getId()));

		// Favorites are a current concept — only active assets shown as individual series
		List<Asset> favoriteAssets = allAssets
			.stream()
			.filter(a -> Boolean.TRUE.equals(a.getIsActive()) && Boolean.TRUE.equals(a.getIsFavorite()))
			.toList();

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

		LocalDate current = start.withDayOfMonth(1);
		LocalDate endFirstOfMonth = end.withDayOfMonth(1);
		while (!current.isAfter(endFirstOfMonth)) {
			LocalDate monthLastDay = current.withDayOfMonth(current.lengthOfMonth());
			final LocalDate datePoint = monthLastDay.isAfter(end) ? end : monthLastDay;
			List<Asset> activeAtDate = allAssets
				.stream()
				.filter(a -> wasActiveAt(a, statusLogsByAsset, datePoint))
				.toList();

			labels.add(formatDateLabel(current));
			totalSeries.add(calculateNetWorthAt(activeAtDate, snapshotsByAsset, datePoint));
			assetsOnlySeries.add(calculateGrossAssetsAt(activeAtDate, snapshotsByAsset, datePoint));
			for (Asset asset : favoriteAssets) {
				List<Snapshot> snapshots = snapshotsByAsset.getOrDefault(asset.getId(), List.of());
				BigDecimal value = calculateAssetValueAt(snapshots, datePoint);
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

		Map<Integer, BigDecimal> projections = new LinkedHashMap<>();
		if (current.compareTo(BigDecimal.ZERO) > 0 && oneYearAgo.compareTo(BigDecimal.ZERO) > 0) {
			BigDecimal growthRate = current
				.divide(oneYearAgo, 10, RoundingMode.HALF_UP)
				.subtract(BigDecimal.ONE);
			if (growthRate.compareTo(BigDecimal.ZERO) < 0) growthRate = BigDecimal.ZERO;
			BigDecimal multiplier = BigDecimal.ONE.add(growthRate);
			projections.put(1, current.multiply(multiplier).setScale(2, RoundingMode.HALF_UP));
			projections.put(5, current.multiply(multiplier.pow(5)).setScale(2, RoundingMode.HALF_UP));
			projections.put(10, current.multiply(multiplier.pow(10)).setScale(2, RoundingMode.HALF_UP));
		} else {
			projections.put(1, current.setScale(2, RoundingMode.HALF_UP));
			projections.put(5, current.setScale(2, RoundingMode.HALF_UP));
			projections.put(10, current.setScale(2, RoundingMode.HALF_UP));
		}

		return projections;
	}

	// --- Private helpers ---

	private Map<UUID, List<Snapshot>> loadSnapshotsByAsset(UUID userId) {
		return snapshotRepository
			.findByAssetUserId(userId)
			.stream()
			.collect(Collectors.groupingBy(s -> s.getAsset().getId()));
	}

	private Map<UUID, List<Snapshot>> loadSnapshotsByAsset(UUID userId, LocalDate since) {
		return snapshotRepository
			.findByAssetUserIdAndReferenceDateGreaterThanEqual(userId, since)
			.stream()
			.collect(Collectors.groupingBy(s -> s.getAsset().getId()));
	}

	private Map<LocalDate, BigDecimal> calculateNetWorthAtDates(
		List<Asset> assets,
		Map<UUID, List<Snapshot>> snapshotsByAsset,
		List<LocalDate> dates
	) {
		Map<LocalDate, BigDecimal> result = new HashMap<>();
		for (LocalDate date : dates) {
			result.put(date, calculateNetWorthAt(assets, snapshotsByAsset, date));
		}
		return result;
	}

	private Map<LocalDate, BigDecimal> calculateGrossAssetsAtDates(
		List<Asset> assets,
		Map<UUID, List<Snapshot>> snapshotsByAsset,
		List<LocalDate> dates
	) {
		Map<LocalDate, BigDecimal> result = new HashMap<>();
		for (LocalDate date : dates) {
			result.put(date, calculateGrossAssetsAt(assets, snapshotsByAsset, date));
		}
		return result;
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
				BigDecimal startAmount = snapshotStart.get().getAmountOriginalCurrency();
				BigDecimal endRate = snapshotEnd.get().getExchangeRateToBase();
				BigDecimal startRate = snapshotStart.get().getExchangeRateToBase();

				impact = impact.add(
					startAmount.multiply(endRate).subtract(startAmount.multiply(startRate))
				);
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

	/**
	 * Returns true if the asset was active on the given date, based on the status log.
	 * For today or later: the isActive flag is authoritative (matches the summary card).
	 * For historical dates: use the latest log entry ≤ date, or assume active if no log exists
	 * so that pre-log history is preserved (supports backdated initial snapshots).
	 * Exception: a currently-inactive asset created after the target date could not have been
	 * active at that date and is excluded, preventing archived assets with backdated snapshots
	 * from inflating historical bars.
	 */
	private boolean wasActiveAt(
		Asset asset,
		Map<UUID, List<AssetStatusLog>> statusLogsByAsset,
		LocalDate date
	) {
		if (!date.isBefore(LocalDate.now())) {
			return Boolean.TRUE.equals(asset.getIsActive());
		}
		if (
			!Boolean.TRUE.equals(asset.getIsActive()) &&
			asset.getCreatedAt() != null &&
			asset.getCreatedAt().toLocalDate().isAfter(date)
		) {
			return false;
		}
		List<AssetStatusLog> logs = statusLogsByAsset.getOrDefault(asset.getId(), List.of());
		return logs
			.stream()
			.filter(l -> !l.getChangedAt().isAfter(date))
			.max(Comparator.comparing(AssetStatusLog::getChangedAt))
			.map(AssetStatusLog::getIsActive)
			.orElse(true);
	}

	private BigDecimal getLatestSnapshotValue(List<Snapshot> snapshots) {
		return snapshots
			.stream()
			.max(Comparator.comparing(Snapshot::getReferenceDate))
			.map(Snapshot::getAmountInBaseCurrency)
			.orElse(BigDecimal.ZERO);
	}
}
