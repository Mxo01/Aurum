package com.backend.aurum.domain.cashflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(requiredProperties = { "id", "month", "earned", "spent" })
public class CashFlowEntryDTO {

	@Schema(nullable = true)
	private UUID id;

	private Integer month;
	private BigDecimal earned;
	private BigDecimal spent;
}
