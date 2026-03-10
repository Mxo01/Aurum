package com.backend.aurum.domain.analytics.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class AnalyticsSummaryDTO {
    private BigDecimal totalNetWorth;
    private VariationDTO variations;
    private Map<String, BigDecimal> assetAllocation;
    private BigDecimal currencyImpact;
    private BigDecimal savingsRate;
}
