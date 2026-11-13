package com.backend.aurum.domain.cashflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(requiredProperties = { "year", "entries", "previousYearEntries", "availableYears" })
public class CashFlowYearDTO {

	private Integer year;
	private List<CashFlowEntryDTO> entries;

	@Schema(nullable = true)
	private List<CashFlowEntryDTO> previousYearEntries;

	private List<Integer> availableYears;
}
