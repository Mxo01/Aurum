package com.backend.aurum.domain.cashflow.mapper;

import com.backend.aurum.domain.cashflow.dto.CashFlowEntryDTO;
import com.backend.aurum.domain.cashflow.model.CashFlow;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class CashFlowMapper {

	public CashFlowEntryDTO toEntryDTO(CashFlow entity) {
		return CashFlowEntryDTO.builder()
			.id(entity.getId())
			.month(entity.getMonth())
			.earned(entity.getEarned())
			.spent(entity.getSpent())
			.build();
	}

	public List<CashFlowEntryDTO> toFullYearGrid(List<CashFlow> entries) {
		Map<Integer, CashFlow> byMonth = entries
			.stream()
			.collect(Collectors.toMap(CashFlow::getMonth, Function.identity()));

		List<CashFlowEntryDTO> result = new ArrayList<>(12);
		for (int month = 1; month <= 12; month++) {
			CashFlow entry = byMonth.get(month);
			if (entry != null) {
				result.add(toEntryDTO(entry));
			} else {
				result.add(
					CashFlowEntryDTO.builder()
						.month(month)
						.earned(BigDecimal.ZERO)
						.spent(BigDecimal.ZERO)
						.build()
				);
			}
		}
		return result;
	}
}
