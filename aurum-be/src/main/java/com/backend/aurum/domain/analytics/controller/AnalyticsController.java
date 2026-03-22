package com.backend.aurum.domain.analytics.controller;

import com.backend.aurum.domain.analytics.dto.AnalyticsSummaryDTO;
import com.backend.aurum.domain.analytics.dto.ChartDataDTO;
import com.backend.aurum.domain.analytics.service.AnalyticsService;
import com.backend.aurum.domain.user.model.UserPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Performance metrics, allocation and projections")
public class AnalyticsController {

	private final AnalyticsService analyticsService;

	@GetMapping("/summary")
	public ResponseEntity<AnalyticsSummaryDTO> getSummary(
		@AuthenticationPrincipal UserPrincipal principal
	) {
		UUID userId = principal.user().getId();
		log.debug(
			"AnalyticsController#getSummary - Request for analytics summary for userId={}",
			userId
		);
		return ResponseEntity.ok(analyticsService.getSummary(userId));
	}

	@GetMapping("/chart")
	public ResponseEntity<ChartDataDTO> getChartData(
		@RequestParam(required = false) Integer year,
		@RequestParam(defaultValue = "false") boolean allHistory,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		UUID userId = principal.user().getId();
		log.debug(
			"AnalyticsController#getChartData - Request for chart data: userId={}, year={}, allHistory={}",
			userId,
			year,
			allHistory
		);
		ChartDataDTO chartData;
		if (year != null) {
			chartData = analyticsService.getChartDataForYear(userId, year);
		} else {
			chartData = analyticsService.getChartData(userId, allHistory);
		}
		return ResponseEntity.ok(chartData);
	}

	@GetMapping("/projections")
	public ResponseEntity<Map<Integer, BigDecimal>> getProjections(
		@RequestParam(defaultValue = "10") int years,
		@RequestParam(defaultValue = "false") boolean assetsOnly,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		UUID userId = principal.user().getId();
		log.debug(
			"AnalyticsController#getProjections - Request for projections: userId={}, years={}, assetsOnly={}",
			userId,
			years,
			assetsOnly
		);
		return ResponseEntity.ok(analyticsService.getProjections(userId, years, assetsOnly));
	}
}
