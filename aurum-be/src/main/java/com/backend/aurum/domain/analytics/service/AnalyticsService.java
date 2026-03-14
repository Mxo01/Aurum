package com.backend.aurum.domain.analytics.service;

import com.backend.aurum.domain.analytics.dto.AnalyticsSummaryDTO;
import com.backend.aurum.domain.analytics.dto.ChartDataDTO;
import com.backend.aurum.domain.analytics.dto.DeltaDTO;
import com.backend.aurum.domain.analytics.dto.VariationDTO;
import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.asset.repository.SnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final SnapshotRepository snapshotRepository;
    private final AssetRepository assetRepository;

    public AnalyticsSummaryDTO getSummary(UUID userId) {
        LocalDate now = LocalDate.now();
        
        BigDecimal currentNetWorth = calculateNetWorthAt(userId, now);
        BigDecimal oneMonthAgoNetWorth = calculateNetWorthAt(userId, now.minusMonths(1));
        BigDecimal sixMonthsAgoNetWorth = calculateNetWorthAt(userId, now.minusMonths(6));
        BigDecimal oneYearAgoNetWorth = calculateNetWorthAt(userId, now.minusYears(1));

        return AnalyticsSummaryDTO.builder()
                .totalNetWorth(currentNetWorth)
                .variations(VariationDTO.builder()
                        .oneMonth(calculateDelta(currentNetWorth, oneMonthAgoNetWorth))
                        .sixMonths(calculateDelta(currentNetWorth, sixMonthsAgoNetWorth))
                        .oneYear(calculateDelta(currentNetWorth, oneYearAgoNetWorth))
                        .build())
                .assetAllocation(calculateAssetAllocation(userId))
                .currencyImpact(calculateCurrencyImpact(userId, now.minusMonths(1), now))
                .savingsRate(BigDecimal.ZERO) // Simplified placeholder for now
                .build();
    }

    public ChartDataDTO getChartData(UUID userId) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusYears(1);
        
        List<LocalDate> labels = new ArrayList<>();
        List<BigDecimal> totalSeries = new ArrayList<>();
        List<BigDecimal> favoriteSeries = new ArrayList<>();
        
        LocalDate current = start;
        
				while (!current.isAfter(end)) {
            labels.add(current);
            totalSeries.add(calculateNetWorthAt(userId, current));
            favoriteSeries.add(calculateFavoriteAssetsValueAt(userId, current));
            current = current.plusMonths(1);
        }
        
        return ChartDataDTO.builder()
                .labels(labels)
                .totalNetWorth(totalSeries)
                .favoriteAssetsValue(favoriteSeries)
                .build();
    }

    private BigDecimal calculateFavoriteAssetsValueAt(UUID userId, LocalDate date) {
        List<Asset> favoriteAssets = assetRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId).stream()
                .filter(Asset::getIsFavorite)
                .toList();
        
        BigDecimal total = BigDecimal.ZERO;
        
				for (Asset asset : favoriteAssets) {
            Optional<Snapshot> latestSnapshot = snapshotRepository.findByAssetId(asset.getId()).stream()
                    .filter(s -> !s.getReferenceDate().isAfter(date))
                    .max(Comparator.comparing(Snapshot::getReferenceDate));
            
            if (latestSnapshot.isPresent()) {
                total = total.add(latestSnapshot.get().getAmountInBaseCurrency());
            }
        }
        
				return total;
    }

    public Map<Integer, BigDecimal> getProjections(UUID userId, int years) {
        BigDecimal current = calculateNetWorthAt(userId, LocalDate.now());
        BigDecimal oneYearAgo = calculateNetWorthAt(userId, LocalDate.now().minusYears(1));
        
        // Simple linear growth estimate based on last year
        BigDecimal growth = current.subtract(oneYearAgo);
        if (growth.compareTo(BigDecimal.ZERO) < 0) growth = BigDecimal.ZERO;

        Map<Integer, BigDecimal> projections = new LinkedHashMap<>();
        projections.put(1, current.add(growth));
        projections.put(5, current.add(growth.multiply(BigDecimal.valueOf(5))));
        projections.put(10, current.add(growth.multiply(BigDecimal.valueOf(10))));
        
        return projections;
    }

    private BigDecimal calculateNetWorthAt(UUID userId, LocalDate date) {
        List<Asset> assets = assetRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId);
        BigDecimal total = BigDecimal.ZERO;
        
        for (Asset asset : assets) {
            Optional<Snapshot> latestSnapshot = snapshotRepository.findByAssetId(asset.getId()).stream()
                    .filter(s -> !s.getReferenceDate().isAfter(date))
                    .max(Comparator.comparing(Snapshot::getReferenceDate));
            
            if (latestSnapshot.isPresent()) {
                total = total.add(latestSnapshot.get().getAmountInBaseCurrency());
            }
        }

        return total;
    }

    private DeltaDTO calculateDelta(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return DeltaDTO.builder()
                    .absolute(current)
                    .percentage(BigDecimal.ZERO)
                    .build();
        }
        BigDecimal absolute = current.subtract(previous);
        BigDecimal percentage = absolute.divide(previous, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

        return DeltaDTO.builder()
                .absolute(absolute)
                .percentage(percentage)
                .build();
    }

    private Map<String, BigDecimal> calculateAssetAllocation(UUID userId) {
        List<Asset> assets = assetRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId);
        Map<String, BigDecimal> categoryValues = new HashMap<>();
        BigDecimal totalValue = BigDecimal.ZERO;

        for (Asset asset : assets) {
            Optional<Snapshot> latestSnapshot = snapshotRepository.findByAssetId(asset.getId()).stream()
                    .max(Comparator.comparing(Snapshot::getReferenceDate));

            if (latestSnapshot.isPresent()) {
                String category = asset.getCategory() != null ? asset.getCategory().getName() : "Other";
                BigDecimal value = latestSnapshot.get().getAmountInBaseCurrency();
                categoryValues.put(category, categoryValues.getOrDefault(category, BigDecimal.ZERO).add(value));
                totalValue = totalValue.add(value);
            }
        }

        Map<String, BigDecimal> allocation = new HashMap<>();
        if (totalValue.compareTo(BigDecimal.ZERO) > 0) {
            for (Map.Entry<String, BigDecimal> entry : categoryValues.entrySet()) {
                BigDecimal percentage = entry.getValue()
                        .divide(totalValue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                allocation.put(entry.getKey(), percentage);
            }
        }
        return allocation;
    }

    private BigDecimal calculateCurrencyImpact(UUID userId, LocalDate start, LocalDate end) {
        List<Asset> assets = assetRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId);
        BigDecimal impact = BigDecimal.ZERO;

        for (Asset asset : assets) {
            Optional<Snapshot> snapshotEnd = snapshotRepository.findByAssetId(asset.getId()).stream()
                    .filter(s -> !s.getReferenceDate().isAfter(end))
                    .max(Comparator.comparing(Snapshot::getReferenceDate));

            Optional<Snapshot> snapshotStart = snapshotRepository.findByAssetId(asset.getId()).stream()
                    .filter(s -> !s.getReferenceDate().isAfter(start))
                    .max(Comparator.comparing(Snapshot::getReferenceDate));

            if (snapshotEnd.isPresent() && snapshotStart.isPresent()) {
                BigDecimal endAmount = snapshotEnd.get().getAmountOriginalCurrency();
                BigDecimal endRate = snapshotEnd.get().getExchangeRateToBase();
                BigDecimal startRate = snapshotStart.get().getExchangeRateToBase();

                BigDecimal valueAtEndRate = endAmount.multiply(endRate);
                BigDecimal valueAtStartRate = endAmount.multiply(startRate);
                
                impact = impact.add(valueAtEndRate.subtract(valueAtStartRate));
            }
        }
		
        return impact;
    }
}
