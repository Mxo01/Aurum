package com.backend.aurum.domain.cashflow.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CashFlowYearDTO {

	private Integer year;
	private List<CashFlowEntryDTO> entries;
	private List<CashFlowEntryDTO> previousYearEntries;
	private List<Integer> availableYears;
}
