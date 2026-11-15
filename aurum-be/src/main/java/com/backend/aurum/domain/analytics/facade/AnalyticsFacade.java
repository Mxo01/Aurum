package com.backend.aurum.domain.analytics.facade;

import com.backend.aurum.domain.analytics.dto.AnalyticsSummaryDTO;
import com.backend.aurum.domain.analytics.dto.ChartDataDTO;
import com.backend.aurum.domain.analytics.service.AnalyticsService;
import com.backend.aurum.domain.shared.exception.ValidationException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalyticsFacade {

	private static final int MIN_PROJECTION_YEARS = 1;
	private static final int MAX_PROJECTION_YEARS = 50;

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
		requireProjectionRange(years);
		return analyticsService.getProjections(userId, years, assetsOnly);
	}

	/**
	 * Each projected year is a BigDecimal raised to a power, so the count decides the allocation and
	 * the work: past a human lifetime the request stops being a projection and becomes a way to
	 * exhaust the heap. Checked here, above the service, so the MCP surface inherits the bound.
	 */
	private void requireProjectionRange(int years) {
		if (years < MIN_PROJECTION_YEARS || years > MAX_PROJECTION_YEARS) {
			throw new ValidationException(
				"years",
				"Years must be between " + MIN_PROJECTION_YEARS + " and " + MAX_PROJECTION_YEARS
			);
		}
	}
}
