package com.backend.aurum.domain.cashflow.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.cashflow.dto.CashFlowEntryDTO;
import com.backend.aurum.domain.cashflow.dto.CashFlowYearDTO;
import com.backend.aurum.domain.cashflow.dto.UpdateCashFlowEntryDTO;
import com.backend.aurum.domain.cashflow.mapper.CashFlowMapper;
import com.backend.aurum.domain.cashflow.model.CashFlow;
import com.backend.aurum.domain.cashflow.service.CashFlowService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CashFlowFacadeTest {

	@Mock
	private CashFlowService cashFlowService;

	@Mock
	private CashFlowMapper mapper;

	@InjectMocks
	private CashFlowFacade testSubject;

	@Test
	void getYear_returnsMappedYearDto_withPreviousYearEntries() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		Integer mockYear = 2025;
		List<CashFlow> stubbedEntries = Instancio.ofList(CashFlow.class).size(3).create();
		List<CashFlow> stubbedPrevEntries = Instancio.ofList(CashFlow.class).size(2).create();
		List<Integer> stubbedAvailableYears = List.of(2024, 2025);
		List<CashFlowEntryDTO> stubbedGrid = Instancio.ofList(CashFlowEntryDTO.class).size(12).create();
		List<CashFlowEntryDTO> stubbedPrevGrid = Instancio.ofList(CashFlowEntryDTO.class)
			.size(12)
			.create();
		when(cashFlowService.getEntriesForYear(mockUserId, mockYear)).thenReturn(stubbedEntries);
		when(cashFlowService.getEntriesForYear(mockUserId, mockYear - 1)).thenReturn(
			stubbedPrevEntries
		);
		when(cashFlowService.getAvailableYears(mockUserId)).thenReturn(stubbedAvailableYears);
		when(mapper.toFullYearGrid(stubbedEntries)).thenReturn(stubbedGrid);
		when(mapper.toFullYearGrid(stubbedPrevEntries)).thenReturn(stubbedPrevGrid);

		// WHEN
		CashFlowYearDTO expectedDto = testSubject.getYear(mockUserId, mockYear);

		// THEN
		assertThat(expectedDto.getYear()).isEqualTo(mockYear);
		assertThat(expectedDto.getEntries()).isEqualTo(stubbedGrid);
		assertThat(expectedDto.getPreviousYearEntries()).isEqualTo(stubbedPrevGrid);
		assertThat(expectedDto.getAvailableYears()).isEqualTo(stubbedAvailableYears);
	}

	@Test
	void getYear_setsNullPreviousYearEntries_whenPreviousYearIsEmpty() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		Integer mockYear = 2025;
		List<CashFlow> stubbedEntries = Instancio.ofList(CashFlow.class).size(2).create();
		List<CashFlowEntryDTO> stubbedGrid = Instancio.ofList(CashFlowEntryDTO.class).size(12).create();
		when(cashFlowService.getEntriesForYear(mockUserId, mockYear)).thenReturn(stubbedEntries);
		when(cashFlowService.getEntriesForYear(mockUserId, mockYear - 1)).thenReturn(List.of());
		when(cashFlowService.getAvailableYears(mockUserId)).thenReturn(List.of(mockYear));
		when(mapper.toFullYearGrid(stubbedEntries)).thenReturn(stubbedGrid);

		// WHEN
		CashFlowYearDTO expectedDto = testSubject.getYear(mockUserId, mockYear);

		// THEN
		assertThat(expectedDto.getPreviousYearEntries()).isNull();
	}

	@Test
	void updateEntry_returnsMappedEntryDto() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		Integer mockYear = 2025;
		Integer mockMonth = 4;
		UpdateCashFlowEntryDTO mockDto = Instancio.of(UpdateCashFlowEntryDTO.class)
			.set(Select.field(UpdateCashFlowEntryDTO::getEarned), new BigDecimal("1800.00"))
			.set(Select.field(UpdateCashFlowEntryDTO::getSpent), new BigDecimal("700.00"))
			.create();
		CashFlow mockUpdated = Instancio.create(CashFlow.class);
		CashFlowEntryDTO stubbedDto = Instancio.create(CashFlowEntryDTO.class);
		when(
			cashFlowService.upsertEntry(
				mockUserId,
				mockYear,
				mockMonth,
				mockDto.getEarned(),
				mockDto.getSpent()
			)
		).thenReturn(mockUpdated);
		when(mapper.toEntryDTO(mockUpdated)).thenReturn(stubbedDto);

		// WHEN
		CashFlowEntryDTO expectedDto = testSubject.updateEntry(
			mockUserId,
			mockYear,
			mockMonth,
			mockDto
		);

		// THEN
		assertThat(expectedDto).isEqualTo(stubbedDto);
	}
}
