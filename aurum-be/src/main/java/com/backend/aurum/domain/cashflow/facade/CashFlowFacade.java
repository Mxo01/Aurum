package com.backend.aurum.domain.cashflow.facade;

import com.backend.aurum.domain.cashflow.dto.CashFlowEntryDTO;
import com.backend.aurum.domain.cashflow.dto.CashFlowYearDTO;
import com.backend.aurum.domain.cashflow.dto.UpdateCashFlowEntryDTO;
import com.backend.aurum.domain.cashflow.mapper.CashFlowMapper;
import com.backend.aurum.domain.cashflow.model.CashFlow;
import com.backend.aurum.domain.cashflow.service.CashFlowService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CashFlowFacade {

	private final CashFlowService cashFlowService;
	private final CashFlowMapper mapper;

	public CashFlowYearDTO getYear(UUID userId, Integer year) {
		List<CashFlow> entries = cashFlowService.getEntriesForYear(userId, year);
		List<CashFlow> prevEntries = cashFlowService.getEntriesForYear(userId, year - 1);
		List<Integer> availableYears = cashFlowService.getAvailableYears(userId);

		List<CashFlowEntryDTO> previousYearEntries = prevEntries.isEmpty()
			? null
			: mapper.toFullYearGrid(prevEntries);

		return CashFlowYearDTO.builder()
			.year(year)
			.entries(mapper.toFullYearGrid(entries))
			.previousYearEntries(previousYearEntries)
			.availableYears(availableYears)
			.build();
	}

	public CashFlowEntryDTO updateEntry(
		UUID userId,
		Integer year,
		Integer month,
		UpdateCashFlowEntryDTO dto
	) {
		CashFlow updated = cashFlowService.upsertEntry(
			userId,
			year,
			month,
			dto.getEarned(),
			dto.getSpent()
		);
		return mapper.toEntryDTO(updated);
	}
}
