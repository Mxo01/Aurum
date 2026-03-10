package com.backend.aurum.domain.analytics.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class DeltaDTO {
    private BigDecimal absolute;
    private BigDecimal percentage;
}
