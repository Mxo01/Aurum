package com.backend.aurum.domain.analytics.service;

import com.backend.aurum.domain.analytics.dto.AnalyticsSummaryDTO;
import com.backend.aurum.domain.analytics.dto.ChartDataDTO;
import com.backend.aurum.domain.analytics.dto.DeltaDTO;
import com.backend.aurum.domain.analytics.dto.VariationDTO;
import com.backend.aurum.domain.asset.dto.AssetDTO;
import com.backend.aurum.domain.asset.mapper.AssetMapper;
import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.asset.repository.SnapshotRepository;
import com.backend.aurum.domain.shared.currency.CurrencyConversionService;
import com.backend.aurum.domain.shared.currency.RateBook;
import com.backend.aurum.domain.shared.exception.NotFoundException;
import com.backend.aurum.domain.user.enums.Currency;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
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

	private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
	private static final int RATIO_SCALE = 4;
	private static final int SHORT_ID_LENGTH = 8;

	// Value at a date: greatest referenceDate at or before it, same-date ties broken by the lowest
	// id — the rule SnapshotRepository#findFirstByAssetIdOrderByReferenceDateDescIdAsc already uses.
	private static final Comparator<Snapshot> LATEST_AT_DATE = Comparator.comparing(
		Snapshot::getReferenceDate
	).thenComparing(Snapshot::getId, Comparator.nullsFirst(Comparator.reverseOrder()));

	private final SnapshotRepository snapshotRepository;
	private final AssetRepository assetRepository;
	private final AssetMapper assetMapper;
	private final UserRepository userRepository;
	private final CurrencyConversionService conversionService;
	private final Clock clock;

	public AnalyticsSummaryDTO getSummary(UUID userId) {
		log.debug("AnalyticsService#getSummary - Computing analytics summary for userId={}", userId);
		LocalDate now = LocalDate.now(clock);

		List<Asset> assets = assetRepository.findByUserIdOrderByCreatedAtDesc(userId);

		// Only load snapshots from the oldest needed date (1 year back) instead of all history.
		// Add a 1-month buffer so carry-forward logic works for the earliest variation date.
		// An asset whose last valuation predates that window is carried in with no lower bound, so a
		// stale debt still counts instead of disappearing from the totals.
		LocalDate earliestNeeded = now.minusYears(1).minusMonths(2);
		Map<UUID, List<Snapshot>> snapshotsByAsset = loadSnapshotsByAssetWithCarryIn(
			userId,
			earliestNeeded
		);

		// Anchor variations at the latest snapshot date so that months without data
		// don't collapse the delta to zero via carry-forward.
		LocalDate latestDataDate = snapshotsByAsset
			.values()
			.stream()
			.flatMap(List::stream)
			.map(Snapshot::getReferenceDate)
			.max(LocalDate::compareTo)
			.orElse(now);

		LocalDate oneMonthAgoDate = latestDataDate.minusMonths(1);
		LocalDate sixMonthsAgoDate = latestDataDate.minusMonths(6);
		LocalDate oneYearAgoDate = latestDataDate.minusYears(1);
		LocalDate currencyImpactStart = now.minusMonths(1);
		List<LocalDate> dates = List.of(
			now,
			oneMonthAgoDate,
			sixMonthsAgoDate,
			oneYearAgoDate,
			currencyImpactStart
		);
		RateBook book = rateBook(userId, assets, dates);

		Map<LocalDate, BigDecimal> netWorthByDate = calculateNetWorthAtDates(
			assets,
			snapshotsByAsset,
			book,
			dates
		);
		Map<LocalDate, BigDecimal> grossAssetsByDate = calculateGrossAssetsAtDates(
			assets,
			snapshotsByAsset,
			book,
			dates
		);

		BigDecimal currentNetWorth = netWorthByDate.get(now);
		BigDecimal totalGrossAssets = grossAssetsByDate.get(now);
		// Liabilities are summed on their own pass: derived as gross − net they could never be
		// checked, and they inherited every asymmetry of the two passes above.
		BigDecimal totalLiabilities = calculateLiabilitiesAt(assets, snapshotsByAsset, book, now);

		log.debug(
			"AnalyticsService#getSummary - Summary computed for userId={}: netWorth={}, grossAssets={}, liabilities={}",
			userId,
			currentNetWorth,
			totalGrossAssets,
			totalLiabilities
		);
		Integer oldestSnapshotYear = snapshotRepository
			.findOldestReferenceDateByAssetUserId(userId)
			.map(LocalDate::getYear)
			.orElse(null);

		return AnalyticsSummaryDTO.builder()
			.totalNetWorth(currentNetWorth)
			.totalGrossAssets(totalGrossAssets)
			.variations(
				VariationDTO.builder()
					.oneMonth(calculateDelta(currentNetWorth, netWorthByDate.get(oneMonthAgoDate)))
					.sixMonths(calculateDelta(currentNetWorth, netWorthByDate.get(sixMonthsAgoDate)))
					.oneYear(calculateDelta(currentNetWorth, netWorthByDate.get(oneYearAgoDate)))
					.build()
			)
			.assetVariations(
				VariationDTO.builder()
					.oneMonth(calculateDelta(totalGrossAssets, grossAssetsByDate.get(oneMonthAgoDate)))
					.sixMonths(calculateDelta(totalGrossAssets, grossAssetsByDate.get(sixMonthsAgoDate)))
					.oneYear(calculateDelta(totalGrossAssets, grossAssetsByDate.get(oneYearAgoDate)))
					.build()
			)
			.assetAllocation(calculateAssetAllocation(assets, snapshotsByAsset, book, now))
			.currencyImpact(
				calculateCurrencyImpact(assets, snapshotsByAsset, currencyImpactStart, now, book)
			)
			.totalLiabilities(totalLiabilities)
			.debtToAssetRatio(calculateDebtToAssetRatio(totalLiabilities, totalGrossAssets))
			.topAssets(calculateTopAssets(assets, snapshotsByAsset, 5, book, now))
			.oldestSnapshotYear(oldestSnapshotYear)
			.build();
	}

	public BigDecimal getNetWorth(UUID userId) {
		log.debug("AnalyticsService#getNetWorth - Computing net worth for userId={}", userId);
		List<Asset> assets = assetRepository.findByUserIdOrderByCreatedAtDesc(userId);
		Map<UUID, List<Snapshot>> snapshotsByAsset = loadSnapshotsByAsset(userId);
		LocalDate today = LocalDate.now(clock);
		RateBook book = rateBook(userId, assets, List.of(today));
		return calculateNetWorthAt(assets, snapshotsByAsset, book, today);
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
		LocalDate end = LocalDate.now(clock);
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

		List<Asset> favoriteAssets = allAssets
			.stream()
			.filter(a -> Boolean.TRUE.equals(a.getIsFavorite()))
			.toList();

		List<LocalDate> datePoints = monthEndDates(start, end);
		RateBook book = rateBook(userId, allAssets, datePoints);

		Map<UUID, String> seriesKeys = favoriteSeriesKeys(favoriteAssets);
		List<String> labels = new ArrayList<>();
		List<BigDecimal> totalSeries = new ArrayList<>();
		List<BigDecimal> assetsOnlySeries = new ArrayList<>();
		Map<String, List<BigDecimal>> favoriteSeriesValues = new LinkedHashMap<>();
		Map<String, String> favoriteAssetsTypes = new LinkedHashMap<>();
		for (Asset asset : favoriteAssets) {
			String key = seriesKeys.get(asset.getId());
			favoriteSeriesValues.put(key, new ArrayList<>());
			favoriteAssetsTypes.put(key, asset.isLiability() ? "LIABILITY" : "ASSET");
		}

		for (LocalDate datePoint : datePoints) {
			labels.add(formatDateLabel(datePoint));
			totalSeries.add(calculateNetWorthAt(allAssets, snapshotsByAsset, book, datePoint));
			assetsOnlySeries.add(calculateGrossAssetsAt(allAssets, snapshotsByAsset, book, datePoint));
			for (Asset asset : favoriteAssets) {
				List<Snapshot> snapshots = snapshotsByAsset.getOrDefault(asset.getId(), List.of());
				BigDecimal value = calculateAssetValueAt(asset, snapshots, book, datePoint);
				if (asset.isLiability()) {
					value = value.negate();
				}
				favoriteSeriesValues.get(seriesKeys.get(asset.getId())).add(value);
			}
		}

		return ChartDataDTO.builder()
			.labels(labels)
			.totalNetWorth(totalSeries)
			.totalAssetsOnly(assetsOnlySeries)
			.favoriteAssetsValues(favoriteSeriesValues)
			.favoriteAssetsTypes(favoriteAssetsTypes)
			.build();
	}

	private List<LocalDate> monthEndDates(LocalDate start, LocalDate end) {
		List<LocalDate> datePoints = new ArrayList<>();
		LocalDate current = start.withDayOfMonth(1);
		LocalDate endFirstOfMonth = end.withDayOfMonth(1);
		while (!current.isAfter(endFirstOfMonth)) {
			LocalDate monthLastDay = current.withDayOfMonth(current.lengthOfMonth());
			datePoints.add(monthLastDay.isAfter(end) ? end : monthLastDay);
			current = current.plusMonths(1);
		}
		return datePoints;
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
		if (years < 1) return Map.of();

		List<Asset> assets = assetRepository.findByUserIdOrderByCreatedAtDesc(userId);
		Map<UUID, List<Snapshot>> snapshotsByAsset = loadSnapshotsByAsset(userId);

		LocalDate today = LocalDate.now(clock);
		LocalDate oneYearAgoDate = today.minusYears(1);
		RateBook book = rateBook(userId, assets, List.of(today, oneYearAgoDate));

		BigDecimal current = assetsOnly
			? calculateGrossAssetsAt(assets, snapshotsByAsset, book, today)
			: calculateNetWorthAt(assets, snapshotsByAsset, book, today);
		BigDecimal oneYearAgo = assetsOnly
			? calculateGrossAssetsAt(assets, snapshotsByAsset, book, oneYearAgoDate)
			: calculateNetWorthAt(assets, snapshotsByAsset, book, oneYearAgoDate);

		BigDecimal growthRate = BigDecimal.ZERO;
		if (current.compareTo(BigDecimal.ZERO) > 0 && oneYearAgo.compareTo(BigDecimal.ZERO) > 0) {
			growthRate = current.divide(oneYearAgo, 10, RoundingMode.HALF_UP).subtract(BigDecimal.ONE);
			if (growthRate.compareTo(BigDecimal.ZERO) < 0) growthRate = BigDecimal.ZERO;
		}
		BigDecimal multiplier = BigDecimal.ONE.add(growthRate);

		Map<Integer, BigDecimal> projections = new LinkedHashMap<>();
		for (int year = 1; year <= years; year++) {
			projections.put(
				year,
				current.multiply(multiplier.pow(year)).setScale(2, RoundingMode.HALF_UP)
			);
		}

		return projections;
	}

	// --- Private helpers ---

	private RateBook rateBook(UUID userId, List<Asset> assets, Collection<LocalDate> dates) {
		return conversionService.book(
			currencyOf(userId),
			assets.stream().map(Asset::getOriginalCurrency).distinct().toList(),
			dates
		);
	}

	private Currency currencyOf(UUID userId) {
		User user = userRepository
			.findById(Objects.requireNonNull(userId))
			.orElseThrow(() -> new NotFoundException("User not found"));
		return user.getCurrency();
	}

	/**
	 * Legacy rows can carry no currency: they were written in the user's currency,
	 * which is the book's target.
	 */
	private Currency currencyOf(Asset asset, RateBook book) {
		return asset.getOriginalCurrency() != null ? asset.getOriginalCurrency() : book.target();
	}

	private BigDecimal convert(Snapshot snapshot, Asset asset, RateBook book, LocalDate date) {
		return book.convert(snapshot.getAmountOriginalCurrency(), currencyOf(asset, book), date);
	}

	private Map<UUID, List<Snapshot>> loadSnapshotsByAsset(UUID userId) {
		return snapshotRepository
			.findByAssetUserId(userId)
			.stream()
			.collect(Collectors.groupingBy(s -> s.getAsset().getId()));
	}

	private Map<UUID, List<Snapshot>> loadSnapshotsByAssetWithCarryIn(UUID userId, LocalDate since) {
		Map<UUID, Snapshot> merged = new LinkedHashMap<>();
		for (Snapshot snapshot : snapshotRepository.findByAssetUserIdAndReferenceDateGreaterThanEqual(
			userId,
			since
		)) {
			merged.put(snapshot.getId(), snapshot);
		}
		for (Snapshot snapshot : snapshotRepository.findLatestPerAssetByUserId(userId)) {
			merged.putIfAbsent(snapshot.getId(), snapshot);
		}
		return merged.values().stream().collect(Collectors.groupingBy(s -> s.getAsset().getId()));
	}

	private Map<LocalDate, BigDecimal> calculateNetWorthAtDates(
		List<Asset> assets,
		Map<UUID, List<Snapshot>> snapshotsByAsset,
		RateBook book,
		List<LocalDate> dates
	) {
		Map<LocalDate, BigDecimal> result = new HashMap<>();
		for (LocalDate date : dates) {
			result.put(date, calculateNetWorthAt(assets, snapshotsByAsset, book, date));
		}
		return result;
	}

	private Map<LocalDate, BigDecimal> calculateGrossAssetsAtDates(
		List<Asset> assets,
		Map<UUID, List<Snapshot>> snapshotsByAsset,
		RateBook book,
		List<LocalDate> dates
	) {
		Map<LocalDate, BigDecimal> result = new HashMap<>();
		for (LocalDate date : dates) {
			result.put(date, calculateGrossAssetsAt(assets, snapshotsByAsset, book, date));
		}
		return result;
	}

	private BigDecimal calculateNetWorthAt(
		List<Asset> assets,
		Map<UUID, List<Snapshot>> snapshotsByAsset,
		RateBook book,
		LocalDate date
	) {
		BigDecimal total = BigDecimal.ZERO;
		for (Asset asset : assets) {
			BigDecimal value = calculateAssetValueAt(
				asset,
				snapshotsByAsset.getOrDefault(asset.getId(), List.of()),
				book,
				date
			);
			total = asset.isLiability() ? total.subtract(value) : total.add(value);
		}
		return total;
	}

	private BigDecimal calculateGrossAssetsAt(
		List<Asset> assets,
		Map<UUID, List<Snapshot>> snapshotsByAsset,
		RateBook book,
		LocalDate date
	) {
		BigDecimal total = BigDecimal.ZERO;
		for (Asset asset : assets) {
			if (asset.isLiability()) continue;
			total = total.add(
				calculateAssetValueAt(
					asset,
					snapshotsByAsset.getOrDefault(asset.getId(), List.of()),
					book,
					date
				)
			);
		}
		return total;
	}

	private BigDecimal calculateLiabilitiesAt(
		List<Asset> assets,
		Map<UUID, List<Snapshot>> snapshotsByAsset,
		RateBook book,
		LocalDate date
	) {
		BigDecimal total = BigDecimal.ZERO;
		for (Asset asset : assets) {
			if (!asset.isLiability()) continue;
			total = total.add(
				calculateAssetValueAt(
					asset,
					snapshotsByAsset.getOrDefault(asset.getId(), List.of()),
					book,
					date
				)
			);
		}
		return total;
	}

	private BigDecimal calculateAssetValueAt(
		Asset asset,
		List<Snapshot> snapshots,
		RateBook book,
		LocalDate date
	) {
		return snapshots
			.stream()
			.filter(s -> !s.getReferenceDate().isAfter(date))
			.max(LATEST_AT_DATE)
			.map(snapshot -> convert(snapshot, asset, book, date))
			.orElse(BigDecimal.ZERO);
	}

	private BigDecimal calculateDebtToAssetRatio(BigDecimal liabilities, BigDecimal grossAssets) {
		if (grossAssets.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
		return liabilities.divide(grossAssets, RATIO_SCALE, RoundingMode.HALF_UP).multiply(HUNDRED);
	}

	private DeltaDTO calculateDelta(BigDecimal current, BigDecimal previous) {
		if (previous.compareTo(BigDecimal.ZERO) == 0) {
			return DeltaDTO.builder().absolute(current).percentage(BigDecimal.ZERO).build();
		}
		BigDecimal absolute = current.subtract(previous);
		BigDecimal percentage = absolute
			.divide(previous.abs(), RATIO_SCALE, RoundingMode.HALF_UP)
			.multiply(HUNDRED);
		return DeltaDTO.builder().absolute(absolute).percentage(percentage).build();
	}

	private Map<String, BigDecimal> calculateAssetAllocation(
		List<Asset> assets,
		Map<UUID, List<Snapshot>> snapshotsByAsset,
		RateBook book,
		LocalDate asOf
	) {
		Map<String, BigDecimal> categoryValues = new HashMap<>();
		BigDecimal totalAbsoluteValue = BigDecimal.ZERO;

		for (Asset asset : assets) {
			if (asset.isLiability()) continue;
			List<Snapshot> snapshots = snapshotsByAsset.getOrDefault(asset.getId(), List.of());
			Optional<Snapshot> latest = snapshots.stream().max(LATEST_AT_DATE);
			if (latest.isPresent()) {
				String category = asset.getCategory() != null ? asset.getCategory().getName() : "Other";
				BigDecimal value = convert(latest.get(), asset, book, asOf).abs();
				categoryValues.merge(category, value, BigDecimal::add);
				totalAbsoluteValue = totalAbsoluteValue.add(value);
			}
		}

		if (totalAbsoluteValue.compareTo(BigDecimal.ZERO) == 0) return Map.of();

		Map<String, BigDecimal> allocation = new HashMap<>();
		for (Map.Entry<String, BigDecimal> entry : categoryValues.entrySet()) {
			BigDecimal percentage = entry
				.getValue()
				.divide(totalAbsoluteValue, RATIO_SCALE, RoundingMode.HALF_UP)
				.multiply(HUNDRED);
			allocation.put(entry.getKey(), percentage);
		}
		return allocation;
	}

	private BigDecimal calculateCurrencyImpact(
		List<Asset> assets,
		Map<UUID, List<Snapshot>> snapshotsByAsset,
		LocalDate start,
		LocalDate end,
		RateBook book
	) {
		BigDecimal impact = BigDecimal.ZERO;

		for (Asset asset : assets) {
			List<Snapshot> snapshots = snapshotsByAsset.getOrDefault(asset.getId(), List.of());

			Optional<Snapshot> snapshotEnd = snapshots
				.stream()
				.filter(s -> !s.getReferenceDate().isAfter(end))
				.max(LATEST_AT_DATE);

			Optional<Snapshot> snapshotStart = snapshots
				.stream()
				.filter(s -> !s.getReferenceDate().isAfter(start))
				.max(LATEST_AT_DATE);

			if (snapshotEnd.isPresent() && snapshotStart.isPresent()) {
				Currency currency = currencyOf(asset, book);
				BigDecimal startAmount = snapshotStart.get().getAmountOriginalCurrency();
				BigDecimal rateDelta = book.rate(currency, end).subtract(book.rate(currency, start));
				// A debt whose currency appreciates is a loss, so its term carries the opposite sign.
				BigDecimal signedAmount = asset.isLiability() ? startAmount.negate() : startAmount;
				impact = impact.add(signedAmount.multiply(rateDelta));
			}
		}
		return impact;
	}

	private List<AssetDTO> calculateTopAssets(
		List<Asset> assets,
		Map<UUID, List<Snapshot>> snapshotsByAsset,
		int limit,
		RateBook book,
		LocalDate asOf
	) {
		Map<UUID, List<Snapshot>> latestTwoByAsset = new HashMap<>();
		for (Map.Entry<UUID, List<Snapshot>> entry : snapshotsByAsset.entrySet()) {
			List<Snapshot> sorted = entry
				.getValue()
				.stream()
				.sorted(LATEST_AT_DATE.reversed())
				.limit(2)
				.toList();
			latestTwoByAsset.put(entry.getKey(), sorted);
		}

		return assets
			.stream()
			.filter(a -> !a.isLiability())
			.sorted((a, b) -> {
				BigDecimal valA = latestValue(
					a,
					snapshotsByAsset.getOrDefault(a.getId(), List.of()),
					book,
					asOf
				).abs();
				BigDecimal valB = latestValue(
					b,
					snapshotsByAsset.getOrDefault(b.getId(), List.of()),
					book,
					asOf
				).abs();
				return valB.compareTo(valA);
			})
			.limit(limit)
			.map(a ->
				assetMapper.toDtoLight(a, latestTwoByAsset.getOrDefault(a.getId(), List.of()), book, asOf)
			)
			.toList();
	}

	private BigDecimal latestValue(
		Asset asset,
		List<Snapshot> snapshots,
		RateBook book,
		LocalDate date
	) {
		return snapshots
			.stream()
			.max(LATEST_AT_DATE)
			.map(snapshot -> convert(snapshot, asset, book, date))
			.orElse(BigDecimal.ZERO);
	}

	/**
	 * Series keys are labels the dashboard renders, but they must still identify one asset: a name
	 * shared by two favourites gets the short id appended.
	 */
	private Map<UUID, String> favoriteSeriesKeys(List<Asset> favoriteAssets) {
		Map<String, Long> occurrences = favoriteAssets
			.stream()
			.collect(Collectors.groupingBy(Asset::getName, Collectors.counting()));
		Map<UUID, String> keys = new LinkedHashMap<>();
		for (Asset asset : favoriteAssets) {
			String name = asset.getName();
			String key =
				occurrences.get(name) > 1
					? name + " (" + asset.getId().toString().substring(0, SHORT_ID_LENGTH) + ")"
					: name;
			keys.put(asset.getId(), key);
		}
		return keys;
	}
}
