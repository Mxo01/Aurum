package com.backend.aurum.domain.cashflow.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.backend.aurum.domain.cashflow.dto.CashFlowEntryDTO;
import com.backend.aurum.domain.cashflow.model.CashFlow;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CashFlowMapperTest {

	@InjectMocks
	private CashFlowMapper testSubject;

	@Test
	void toEntryDTO_mapsAllFields() {
		// GIVEN
		CashFlow mockEntry = Instancio.of(CashFlow.class)
			.set(Select.field(CashFlow::getId), UUID.randomUUID())
			.set(Select.field(CashFlow::getMonth), 7)
			.set(Select.field(CashFlow::getEarned), new BigDecimal("2500.00"))
			.set(Select.field(CashFlow::getSpent), new BigDecimal("1100.00"))
			.create();

		// WHEN
		CashFlowEntryDTO expectedDto = testSubject.toEntryDTO(mockEntry);

		// THEN
		assertThat(expectedDto.getId()).isEqualTo(mockEntry.getId());
		assertThat(expectedDto.getMonth()).isEqualTo(7);
		assertThat(expectedDto.getEarned()).isEqualByComparingTo(new BigDecimal("2500.00"));
		assertThat(expectedDto.getSpent()).isEqualByComparingTo(new BigDecimal("1100.00"));
	}

	@Test
	void toFullYearGrid_returnsExactly12Months() {
		// GIVEN
		List<CashFlow> mockEntries = Instancio.ofList(CashFlow.class).size(3).create();

		// WHEN
		List<CashFlowEntryDTO> expectedGrid = testSubject.toFullYearGrid(mockEntries);

		// THEN
		assertThat(expectedGrid).hasSize(12);
	}

	@Test
	void toFullYearGrid_fillsMissingMonthsWithZeroes() {
		// GIVEN
		CashFlow mockEntry = Instancio.of(CashFlow.class)
			.set(Select.field(CashFlow::getMonth), 6)
			.set(Select.field(CashFlow::getEarned), new BigDecimal("1000.00"))
			.set(Select.field(CashFlow::getSpent), new BigDecimal("400.00"))
			.create();

		// WHEN
		List<CashFlowEntryDTO> expectedGrid = testSubject.toFullYearGrid(List.of(mockEntry));

		// THEN
		CashFlowEntryDTO juneEntry = expectedGrid.get(5);
		assertThat(juneEntry.getEarned()).isEqualByComparingTo(new BigDecimal("1000.00"));
		assertThat(juneEntry.getSpent()).isEqualByComparingTo(new BigDecimal("400.00"));

		CashFlowEntryDTO januaryEntry = expectedGrid.get(0);
		assertThat(januaryEntry.getId()).isNull();
		assertThat(januaryEntry.getMonth()).isEqualTo(1);
		assertThat(januaryEntry.getEarned()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(januaryEntry.getSpent()).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@Test
	void toFullYearGrid_mapsMonthsInOrder() {
		// GIVEN
		List<CashFlow> mockEntries = List.of();

		// WHEN
		List<CashFlowEntryDTO> expectedGrid = testSubject.toFullYearGrid(mockEntries);

		// THEN
		for (int i = 0; i < 12; i++) {
			assertThat(expectedGrid.get(i).getMonth()).isEqualTo(i + 1);
		}
	}

	@Test
	void toFullYearGrid_preservesRealEntriesWhenPresent() {
		// GIVEN
		CashFlow mockMarch = Instancio.of(CashFlow.class)
			.set(Select.field(CashFlow::getMonth), 3)
			.set(Select.field(CashFlow::getEarned), new BigDecimal("3000.00"))
			.set(Select.field(CashFlow::getSpent), new BigDecimal("900.00"))
			.create();
		CashFlow mockNovember = Instancio.of(CashFlow.class)
			.set(Select.field(CashFlow::getMonth), 11)
			.set(Select.field(CashFlow::getEarned), new BigDecimal("3500.00"))
			.set(Select.field(CashFlow::getSpent), new BigDecimal("1200.00"))
			.create();

		// WHEN
		List<CashFlowEntryDTO> expectedGrid = testSubject.toFullYearGrid(
			List.of(mockMarch, mockNovember)
		);

		// THEN
		assertThat(expectedGrid.get(2).getEarned()).isEqualByComparingTo(new BigDecimal("3000.00"));
		assertThat(expectedGrid.get(2).getId()).isEqualTo(mockMarch.getId());
		assertThat(expectedGrid.get(10).getEarned()).isEqualByComparingTo(new BigDecimal("3500.00"));
		assertThat(expectedGrid.get(10).getId()).isEqualTo(mockNovember.getId());
	}

	@Test
	void toFullYearGrid_returnsAllZeroGrid_whenEntriesIsEmpty() {
		// WHEN
		List<CashFlowEntryDTO> expectedGrid = testSubject.toFullYearGrid(List.of());

		// THEN
		assertThat(expectedGrid).hasSize(12);
		assertThat(expectedGrid).allSatisfy(dto -> {
			assertThat(dto.getId()).isNull();
			assertThat(dto.getEarned()).isEqualByComparingTo(BigDecimal.ZERO);
			assertThat(dto.getSpent()).isEqualByComparingTo(BigDecimal.ZERO);
		});
	}
}
