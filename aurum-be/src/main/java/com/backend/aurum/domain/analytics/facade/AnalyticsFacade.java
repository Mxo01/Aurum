package com.backend.aurum.domain.analytics.facade;

import com.backend.aurum.domain.analytics.dto.AnalyticsSummaryDTO;
import com.backend.aurum.domain.analytics.dto.ChartDataDTO;
import com.backend.aurum.domain.analytics.service.AnalyticsService;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalyticsFacade {

	private final AnalyticsService analyticsService;

	public AnalyticsSummaryDTO getSummary(UUID userId) {
		return analyticsService.getSummary(userId);
	}

	public ChartDataDTO getChartData(UUID userId, boolean allHistory) {
		return analyticsService.getChartData(userId, allHistory);
	}

	public ChartDataDTO getChartDataForYear(UUID userId, int year) {
		return analyticsService.getChartDataForYear(userId, year);
	}

	public Map<Integer, BigDecimal> getProjections(UUID userId, int years, boolean assetsOnly) {
		return analyticsService.getProjections(userId, years, assetsOnly);
	}
}
