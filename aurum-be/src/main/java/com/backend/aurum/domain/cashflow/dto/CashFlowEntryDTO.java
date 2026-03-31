package com.backend.aurum.domain.cashflow.dto;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CashFlowEntryDTO {

	private UUID id;
	private Integer month;
	private BigDecimal earned;
	private BigDecimal spent;
}
