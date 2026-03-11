package com.backend.aurum.domain.analytics.controller;

import com.backend.aurum.domain.analytics.dto.AnalyticsSummaryDTO;
import com.backend.aurum.domain.analytics.dto.ChartDataDTO;
import com.backend.aurum.domain.analytics.service.AnalyticsService;
import com.backend.aurum.infrastructure.security.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    private final SecurityUtils securityUtils;

    @GetMapping("/summary")
    public ResponseEntity<AnalyticsSummaryDTO> getSummary() {
        UUID userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(analyticsService.getSummary(userId));
    }

    @GetMapping("/chart")
    public ResponseEntity<ChartDataDTO> getChartData() {
        UUID userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(analyticsService.getChartData(userId));
    }

    @GetMapping("/projections")
    public ResponseEntity<Map<Integer, BigDecimal>> getProjections(@RequestParam(defaultValue = "10") int years) {
        UUID userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(analyticsService.getProjections(userId, years));
    }
}
