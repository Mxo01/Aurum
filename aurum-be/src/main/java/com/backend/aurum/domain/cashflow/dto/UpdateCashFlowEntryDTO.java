package com.backend.aurum.domain.cashflow.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class UpdateCashFlowEntryDTO {

	private BigDecimal earned;
	private BigDecimal spent;
}
