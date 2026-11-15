package com.backend.aurum.domain.cashflow.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.backend.aurum.domain.cashflow.dto.UpdateCashFlowEntryDTO;
import com.backend.aurum.domain.shared.exception.ValidationException;
import java.math.BigDecimal;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CashFlowValidationServiceTest {

	private static final Integer EARLIEST_YEAR = 1900;

	private static final Integer LATEST_YEAR = 2100;

	private static final Integer YEAR_BEFORE_THE_RANGE = 1899;

	private static final Integer YEAR_AFTER_THE_RANGE = 2101;

	private static final Integer FIRST_MONTH = 1;

	private static final Integer LAST_MONTH = 12;

	private static final Integer MONTH_BEFORE_THE_RANGE = 0;

	private static final Integer MONTH_AFTER_THE_RANGE = 13;

	private static final Integer VALID_YEAR = 2025;

	private static final Integer VALID_MONTH = 4;

	private static final BigDecimal VALID_EARNED = new BigDecimal("1800.00");

	private static final BigDecimal VALID_SPENT = new BigDecimal("700.00");

	private static final BigDecimal NEGATIVE_AMOUNT = new BigDecimal("-0.01");

	private static final BigDecimal ZERO_AMOUNT = new BigDecimal("0.00");

	private static final BigDecimal FOUR_DECIMAL_AMOUNT = new BigDecimal("1800.1234");

	private static final BigDecimal TRAILING_ZEROS_PAST_FOUR_DECIMALS = new BigDecimal(
		"1800.1234000"
	);

	private static final BigDecimal AMOUNT_WITH_FIVE_DECIMALS = new BigDecimal("1800.12345");

	private static final BigDecimal LARGEST_AMOUNT_THAT_FITS = new BigDecimal("999999999999999.9999");

	private static final BigDecimal AMOUNT_ONE_DIGIT_TOO_LARGE = new BigDecimal("1000000000000000");

	private static final String YEAR_FIELD = "year";

	private static final String MONTH_FIELD = "month";

	private static final String EARNED_FIELD = "earned";

	private static final String SPENT_FIELD = "spent";

	private static final String YEAR_OUT_OF_RANGE_MESSAGE = "Year must be between 1900 and 2100";

	private static final String MONTH_OUT_OF_RANGE_MESSAGE = "Month must be between 1 and 12";

	private static final String NO_DATA_MESSAGE = "Cash flow data is required";

	private static final String EARNED_NEGATIVE_MESSAGE = "Earned cannot be negative";

	private static final String SPENT_NEGATIVE_MESSAGE = "Spent cannot be negative";

	private static final String EARNED_TOO_PRECISE_MESSAGE =
		"Earned cannot have more than 4 decimal places";

	private static final String SPENT_TOO_PRECISE_MESSAGE =
		"Spent cannot have more than 4 decimal places";

	private static final String EARNED_TOO_LARGE_MESSAGE = "Earned is too large";

	private static final String SPENT_TOO_LARGE_MESSAGE = "Spent is too large";

	@InjectMocks
	private CashFlowValidationService testSubject;

	@ParameterizedTest(name = "{0}")
	@MethodSource("rejections")
	void validate_answersThePinnedFieldAndMessage(Rejection rejection) {
		// GIVEN
		Integer mockYear = rejection.year();
		Integer mockMonth = rejection.month();
		UpdateCashFlowEntryDTO mockDto = rejection.dto();

		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.validate(mockYear, mockMonth, mockDto)
		).isInstanceOfSatisfying(ValidationException.class, expectedException -> {
			assertThat(expectedException.getField()).isEqualTo(rejection.expectedField());
			assertThat(expectedException.getMessage()).isEqualTo(rejection.expectedMessage());
		});
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
				"400 on the year when it is after the range",
				YEAR_AFTER_THE_RANGE,
				VALID_MONTH,
				buildDto(),
				YEAR_FIELD,
				YEAR_OUT_OF_RANGE_MESSAGE
			),
			new Rejection(
				"400 on the month when it is missing",
				VALID_YEAR,
				null,
				buildDto(),
				MONTH_FIELD,
				MONTH_OUT_OF_RANGE_MESSAGE
			),
			new Rejection(
				"400 on the month when it is before the range",
				VALID_YEAR,
				MONTH_BEFORE_THE_RANGE,
				buildDto(),
				MONTH_FIELD,
				MONTH_OUT_OF_RANGE_MESSAGE
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
				"400 on the spent amount when it is negative",
				VALID_YEAR,
				VALID_MONTH,
				dtoWith(dto -> dto.setSpent(NEGATIVE_AMOUNT)),
				SPENT_FIELD,
				SPENT_NEGATIVE_MESSAGE
			),
			new Rejection(
				"400 on the earned amount when it has five decimals",
				VALID_YEAR,
				VALID_MONTH,
				dtoWith(dto -> dto.setEarned(AMOUNT_WITH_FIVE_DECIMALS)),
				EARNED_FIELD,
				EARNED_TOO_PRECISE_MESSAGE
			),
			new Rejection(
				"400 on the spent amount when it has five decimals",
				VALID_YEAR,
				VALID_MONTH,
				dtoWith(dto -> dto.setSpent(AMOUNT_WITH_FIVE_DECIMALS)),
				SPENT_FIELD,
				SPENT_TOO_PRECISE_MESSAGE
			),
			new Rejection(
				"400 on the earned amount when it is too large",
				VALID_YEAR,
				VALID_MONTH,
				dtoWith(dto -> dto.setEarned(AMOUNT_ONE_DIGIT_TOO_LARGE)),
				EARNED_FIELD,
				EARNED_TOO_LARGE_MESSAGE
			),
			new Rejection(
				"400 on the spent amount when it is too large",
				VALID_YEAR,
				VALID_MONTH,
				dtoWith(dto -> dto.setSpent(AMOUNT_ONE_DIGIT_TOO_LARGE)),
				SPENT_FIELD,
				SPENT_TOO_LARGE_MESSAGE
			)
		);
	}

	@ParameterizedTest(name = "the year {0} is accepted")
	@MethodSource("yearsWithinTheRange")
	void validate_acceptsAYearWithinTheRange(Integer year) {
		// GIVEN
		UpdateCashFlowEntryDTO mockDto = buildDto();

		// WHEN / THEN
		assertThatCode(() ->
			testSubject.validate(year, VALID_MONTH, mockDto)
		).doesNotThrowAnyException();
	}

	static Stream<Arguments> yearsWithinTheRange() {
		return Stream.of(Arguments.of(EARLIEST_YEAR), Arguments.of(LATEST_YEAR));
	}

	@ParameterizedTest(name = "the month {0} is accepted")
	@MethodSource("monthsWithinTheRange")
	void validate_acceptsAMonthWithinTheRange(Integer month) {
		// GIVEN
		UpdateCashFlowEntryDTO mockDto = buildDto();

		// WHEN / THEN
		assertThatCode(() ->
			testSubject.validate(VALID_YEAR, month, mockDto)
		).doesNotThrowAnyException();
	}

	static Stream<Arguments> monthsWithinTheRange() {
		return Stream.of(Arguments.of(FIRST_MONTH), Arguments.of(LAST_MONTH));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("acceptedAmountPairings")
	void validate_acceptsTheStoredAmounts(String scenario, BigDecimal earned, BigDecimal spent) {
		// GIVEN
		UpdateCashFlowEntryDTO mockDto = buildDto(earned, spent);

		// WHEN / THEN
		assertThatCode(() ->
			testSubject.validate(VALID_YEAR, VALID_MONTH, mockDto)
		).doesNotThrowAnyException();
	}

	static Stream<Arguments> acceptedAmountPairings() {
		return Stream.of(
			Arguments.of("a partial update that omits the earned amount", null, VALID_SPENT),
			Arguments.of("a partial update that omits the spent amount", VALID_EARNED, null),
			Arguments.of("a partial update that omits both amounts", null, null),
			Arguments.of("a zero amount", ZERO_AMOUNT, ZERO_AMOUNT),
			Arguments.of("the four decimals the column keeps", FOUR_DECIMAL_AMOUNT, FOUR_DECIMAL_AMOUNT),
			Arguments.of(
				"trailing zeros past four decimals",
				TRAILING_ZEROS_PAST_FOUR_DECIMALS,
				TRAILING_ZEROS_PAST_FOUR_DECIMALS
			),
			Arguments.of(
				"the largest amount the column holds",
				LARGEST_AMOUNT_THAT_FITS,
				LARGEST_AMOUNT_THAT_FITS
			)
		);
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
}
