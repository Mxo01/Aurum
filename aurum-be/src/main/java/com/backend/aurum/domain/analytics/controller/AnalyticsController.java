package com.backend.aurum.domain.analytics.controller;

import com.backend.aurum.domain.analytics.dto.AnalyticsSummaryDTO;
import com.backend.aurum.domain.analytics.dto.ChartDataDTO;
import com.backend.aurum.domain.analytics.service.AnalyticsService;
import com.backend.aurum.domain.user.model.UserPrincipal;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Performance metrics, allocation and projections")
public class AnalyticsController {

	private final AnalyticsService analyticsService;

	@GetMapping("/summary")
	public ResponseEntity<AnalyticsSummaryDTO> getSummary(@AuthenticationPrincipal UserPrincipal principal) {
		UUID userId = principal.user().getId();
		return ResponseEntity.ok(analyticsService.getSummary(userId));
	}

	@GetMapping("/chart")
	public ResponseEntity<ChartDataDTO> getChartData(
			@RequestParam(required = false) Integer year,
			@RequestParam(defaultValue = "false") boolean allHistory,
			@AuthenticationPrincipal UserPrincipal principal) {
		UUID userId = principal.user().getId();
		ChartDataDTO chartData;
		if (year != null) {
			chartData = analyticsService.getChartDataForYear(userId, year);
		} else {
			chartData = analyticsService.getChartData(userId, allHistory);
		}
		return ResponseEntity.ok(chartData);
	}

	@GetMapping("/projections")
	public ResponseEntity<Map<Integer, BigDecimal>> getProjections(@RequestParam(defaultValue = "10") int years,
			@AuthenticationPrincipal UserPrincipal principal) {
		UUID userId = principal.user().getId();
		return ResponseEntity.ok(analyticsService.getProjections(userId, years));
	}
}
