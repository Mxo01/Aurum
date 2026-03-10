package com.backend.aurum.domain.analytics.controller;

import com.backend.aurum.domain.analytics.dto.AnalyticsSummaryDTO;
import com.backend.aurum.domain.analytics.dto.ChartDataDTO;
import com.backend.aurum.domain.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/summary")
    public ResponseEntity<AnalyticsSummaryDTO> getSummary(@RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(analyticsService.getSummary(userId));
    }

    @GetMapping("/chart")
    public ResponseEntity<ChartDataDTO> getChartData(@RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(analyticsService.getChartData(userId));
    }

    @GetMapping("/projections")
    public ResponseEntity<Map<Integer, BigDecimal>> getProjections(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "10") int years) {
        return ResponseEntity.ok(analyticsService.getProjections(userId, years));
    }
}
