package com.backend.aurum.domain.analytics.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ChartDataDTO {
    private List<LocalDate> labels;
    private List<BigDecimal> totalNetWorth;
    private Map<String, List<BigDecimal>> favoriteAssetsValues;
}
