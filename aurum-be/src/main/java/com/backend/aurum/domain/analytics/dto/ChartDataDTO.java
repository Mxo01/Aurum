package com.backend.aurum.domain.analytics.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ChartDataDTO {
    private List<LocalDate> labels;
    private List<BigDecimal> totalNetWorth;
    private List<BigDecimal> favoriteAssetsValue;
}
