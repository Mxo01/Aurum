package com.backend.aurum.domain.analytics.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ChartDataDTO {
    private List<String> labels;
    private List<BigDecimal> totalNetWorth;
    private List<BigDecimal> totalAssetsOnly;
    private Map<String, List<BigDecimal>> favoriteAssetsValues;
    private Map<String, String> favoriteAssetsTypes;
}
