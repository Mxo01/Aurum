package com.backend.aurum.domain.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(requiredProperties = { "absolute", "percentage" })
public class DeltaDTO {

	private BigDecimal absolute;
	private BigDecimal percentage;
}
