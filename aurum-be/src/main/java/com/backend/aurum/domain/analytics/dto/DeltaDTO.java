package com.backend.aurum.domain.analytics.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeltaDTO {

	private BigDecimal absolute;
	private BigDecimal percentage;
}
