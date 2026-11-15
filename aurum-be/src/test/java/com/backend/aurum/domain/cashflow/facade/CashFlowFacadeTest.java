package com.backend.aurum.domain.cashflow.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.cashflow.dto.CashFlowEntryDTO;
import com.backend.aurum.domain.cashflow.dto.CashFlowYearDTO;
import com.backend.aurum.domain.cashflow.dto.UpdateCashFlowEntryDTO;
import com.backend.aurum.domain.cashflow.mapper.CashFlowMapper;
import com.backend.aurum.domain.cashflow.model.CashFlow;
import com.backend.aurum.domain.cashflow.service.CashFlowService;
import com.backend.aurum.domain.cashflow.validation.CashFlowValidationService;
import com.backend.aurum.domain.shared.exception.ValidationException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CashFlowFacadeTest {

	private static final Integer VALID_YEAR = 2025;

	private static final Integer VALID_MONTH = 4;

	private static final Integer YEAR_BEFORE_THE_RANGE = 1899;

	private static final Integer MONTH_AFTER_THE_RANGE = 13;

	private static final Integer FIRST_BULK_ROW_MONTH = 1;

	private static final Integer REJECTED_BULK_ROW_MONTH = 3;

	private static final Integer LAST_BULK_ROW_MONTH = 2;

	private static final BigDecimal VALID_EARNED = new BigDecimal("1800.00");

	private static final BigDecimal VALID_SPENT = new BigDecimal("700.00");

	private static final BigDecimal NEGATIVE_AMOUNT = new BigDecimal("-0.01");

	private static final BigDecimal AMOUNT_WITH_FIVE_DECIMALS = new BigDecimal("1800.12345");

	private static final String YEAR_FIELD = "year";

	private static final String MONTH_FIELD = "month";

	private static final String EARNED_FIELD = "earned";

	private static final String SPENT_FIELD = "spent";

	private static final String YEAR_OUT_OF_RANGE_MESSAGE = "Year must be between 1900 and 2100";

	private static final String MONTH_OUT_OF_RANGE_MESSAGE = "Month must be between 1 and 12";

	private static final String NO_DATA_MESSAGE = "Cash flow data is required";

	private static final String EARNED_NEGATIVE_MESSAGE = "Earned cannot be negative";

	private static final String SPENT_TOO_PRECISE_MESSAGE =
		"Spent cannot have more than 4 decimal places";

	@Mock
	private CashFlowService cashFlowService;

	@Mock
	private CashFlowMapper mapper;

	@Spy
	private CashFlowValidationService validationService = new CashFlowValidationService();

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

	@ParameterizedTest(name = "{0}")
	@MethodSource("rejections")
	void updateEntry_rejectsBeforeTheServiceIsTouched(Rejection rejection) {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UpdateCashFlowEntryDTO mockDto = rejection.dto();

		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.updateEntry(mockUserId, rejection.year(), rejection.month(), mockDto)
		).isInstanceOfSatisfying(ValidationException.class, expectedException -> {
			assertThat(expectedException.getField()).isEqualTo(rejection.expectedField());
			assertThat(expectedException.getMessage()).isEqualTo(rejection.expectedMessage());
		});
		verifyNoInteractions(cashFlowService);
		verifyNoInteractions(mapper);
	}

	static Stream<Rejection> rejections() {
		return Stream.of(
			new Rejection(
				"400 on the year when it is missing",
				null,
				VALID_MONTH,
				buildDto(),
				YEAR_FIELD,
				YEAR_OUT_OF_RANGE_MESSAGE
			),
			new Rejection(
				"400 on the year when it is before the range",
				YEAR_BEFORE_THE_RANGE,
				VALID_MONTH,
				buildDto(),
				YEAR_FIELD,
				YEAR_OUT_OF_RANGE_MESSAGE
			),
			new Rejection(
				"400 on the month when it is after the range",
				VALID_YEAR,
				MONTH_AFTER_THE_RANGE,
				buildDto(),
				MONTH_FIELD,
				MONTH_OUT_OF_RANGE_MESSAGE
			),
			new Rejection(
				"400 without a field when the entry is missing",
				VALID_YEAR,
				VALID_MONTH,
				null,
				null,
				NO_DATA_MESSAGE
			),
			new Rejection(
				"400 on the earned amount when it is negative",
				VALID_YEAR,
				VALID_MONTH,
				dtoWith(dto -> dto.setEarned(NEGATIVE_AMOUNT)),
				EARNED_FIELD,
				EARNED_NEGATIVE_MESSAGE
			),
			new Rejection(
				"400 on the spent amount when it has five decimals",
				VALID_YEAR,
				VALID_MONTH,
				dtoWith(dto -> dto.setSpent(AMOUNT_WITH_FIVE_DECIMALS)),
				SPENT_FIELD,
				SPENT_TOO_PRECISE_MESSAGE
			)
		);
	}

	@Test
	void updateEntry_passesAMissingAmountThroughAsNull_forAPartialUpdate() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UpdateCashFlowEntryDTO mockDto = buildDto(VALID_EARNED, null);
		CashFlow mockUpdated = Instancio.create(CashFlow.class);
		CashFlowEntryDTO stubbedDto = Instancio.create(CashFlowEntryDTO.class);
		when(
			cashFlowService.upsertEntry(mockUserId, VALID_YEAR, VALID_MONTH, VALID_EARNED, null)
		).thenReturn(mockUpdated);
		when(mapper.toEntryDTO(mockUpdated)).thenReturn(stubbedDto);

		// WHEN
		CashFlowEntryDTO expectedDto = testSubject.updateEntry(
			mockUserId,
			VALID_YEAR,
			VALID_MONTH,
			mockDto
		);

		// THEN
		assertThat(expectedDto).isEqualTo(stubbedDto);
	}

	@Test
	void updateEntry_checksEveryRowOfABulkSequence_soTheFacadeIsTheOnlyWriteEntry() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UpdateCashFlowEntryDTO mockFirstRow = buildDto(VALID_EARNED, VALID_SPENT);
		UpdateCashFlowEntryDTO mockRejectedRow = buildDto(NEGATIVE_AMOUNT, VALID_SPENT);
		UpdateCashFlowEntryDTO mockThirdRow = buildDto(VALID_EARNED, VALID_SPENT);
		CashFlow mockUpdated = Instancio.create(CashFlow.class);
		CashFlowEntryDTO stubbedDto = Instancio.create(CashFlowEntryDTO.class);
		when(
			cashFlowService.upsertEntry(
				mockUserId,
				VALID_YEAR,
				FIRST_BULK_ROW_MONTH,
				VALID_EARNED,
				VALID_SPENT
			)
		).thenReturn(mockUpdated);
		when(mapper.toEntryDTO(mockUpdated)).thenReturn(stubbedDto);

		// WHEN / THEN
		assertThatThrownBy(() ->
			writeRowsAsTheMcpBulkToolDoes(
				mockUserId,
				List.of(
					new BulkRow(FIRST_BULK_ROW_MONTH, mockFirstRow),
					new BulkRow(REJECTED_BULK_ROW_MONTH, mockRejectedRow),
					new BulkRow(LAST_BULK_ROW_MONTH, mockThirdRow)
				)
			)
		).isInstanceOfSatisfying(ValidationException.class, expectedException -> {
			assertThat(expectedException.getField()).isEqualTo(EARNED_FIELD);
			assertThat(expectedException.getMessage()).isEqualTo(EARNED_NEGATIVE_MESSAGE);
		});
		verify(cashFlowService).upsertEntry(
			mockUserId,
			VALID_YEAR,
			FIRST_BULK_ROW_MONTH,
			VALID_EARNED,
			VALID_SPENT
		);
		verifyNoMoreInteractions(cashFlowService);
		verify(mapper).toEntryDTO(mockUpdated);
		verifyNoMoreInteractions(mapper);
	}

	private List<CashFlowEntryDTO> writeRowsAsTheMcpBulkToolDoes(UUID userId, List<BulkRow> rows) {
		return rows
			.stream()
			.map(row -> testSubject.updateEntry(userId, VALID_YEAR, row.month(), row.dto()))
			.toList();
	}

	private static UpdateCashFlowEntryDTO buildDto() {
		return buildDto(VALID_EARNED, VALID_SPENT);
	}

	private static UpdateCashFlowEntryDTO buildDto(BigDecimal earned, BigDecimal spent) {
		return dtoWith(dto -> {
			dto.setEarned(earned);
			dto.setSpent(spent);
		});
	}

	private static UpdateCashFlowEntryDTO dtoWith(Consumer<UpdateCashFlowEntryDTO> mutation) {
		UpdateCashFlowEntryDTO dto = new UpdateCashFlowEntryDTO();
		dto.setEarned(VALID_EARNED);
		dto.setSpent(VALID_SPENT);
		mutation.accept(dto);
		return dto;
	}

	private record Rejection(
		String scenario,
		Integer year,
		Integer month,
		UpdateCashFlowEntryDTO dto,
		String expectedField,
		String expectedMessage
	) {}

	private record BulkRow(Integer month, UpdateCashFlowEntryDTO dto) {}
}
