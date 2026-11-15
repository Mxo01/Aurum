package com.backend.aurum.domain.shared.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.backend.aurum.domain.shared.exception.ValidationException;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class AmountRulesTest {

	private static final String AMOUNT_FIELD = "initialValue";

	private static final String AMOUNT_LABEL = "Amount";

	private static final String RATE_FIELD = "exchangeRateToBase";

	private static final String RATE_LABEL = "Exchange rate";

	private static final BigDecimal ZERO = BigDecimal.ZERO;

	private static final BigDecimal NEGATIVE_AMOUNT = new BigDecimal("-0.01");

	private static final BigDecimal NEGATIVE_RATE = new BigDecimal("-0.5");

	private static final BigDecimal AMOUNT_AT_FOUR_DECIMALS = new BigDecimal("0.0001");

	private static final BigDecimal AMOUNT_AT_FIVE_DECIMALS = new BigDecimal("0.00001");

	private static final BigDecimal AMOUNT_WITH_TRAILING_ZEROS = new BigDecimal("1.50000");

	private static final BigDecimal AMOUNT_AT_NINETEEN_DIGITS = new BigDecimal(
		"123456789012345.9999"
	);

	private static final BigDecimal AMOUNT_OVER_FOUR_DECIMALS = new BigDecimal("1.00001");

	private static final BigDecimal AMOUNT_AT_SIXTEEN_INTEGER_DIGITS = new BigDecimal(
		"1234567890123456"
	);

	private static final BigDecimal AMOUNT_AT_TWENTY_INTEGER_DIGITS = new BigDecimal(
		"12345678901234567890"
	);

	private static final BigDecimal RATE_AT_SIX_DECIMALS = new BigDecimal("1.123456");

	private static final BigDecimal RATE_AT_SEVEN_DECIMALS = new BigDecimal("1.1234567");

	private static final BigDecimal SMALL_RATE_AT_SEVEN_DECIMALS = new BigDecimal("0.0000001");

	private static final BigDecimal RATE_AT_CEILING = new BigDecimal("1000000");

	private static final BigDecimal RATE_AT_CEILING_WITH_TRAILING_ZEROS = new BigDecimal(
		"1000000.000000"
	);

	private static final BigDecimal RATE_JUST_ABOVE_CEILING = new BigDecimal("1000000.000001");

	private static final BigDecimal RATE_ABOVE_CEILING = new BigDecimal("1000001");

	private static final String NEGATIVE_AMOUNT_MESSAGE = "Amount cannot be negative";

	private static final String NON_POSITIVE_RATE_MESSAGE = "Exchange rate must be greater than zero";

	private static final String AMOUNT_SCALE_MESSAGE =
		"Amount cannot have more than 4 decimal places";

	private static final String AMOUNT_TOO_LARGE_MESSAGE = "Amount is too large";

	private static final String RATE_SCALE_MESSAGE =
		"Exchange rate cannot have more than 6 decimal places";

	private static final String RATE_TOO_LARGE_MESSAGE = "Exchange rate is too large";

	@ParameterizedTest(name = "requireNonNegative accepts {0}")
	@MethodSource("nonNegativeAmounts")
	void requireNonNegative_acceptsZeroAndPositiveAmounts(BigDecimal amount) {
		// WHEN / THEN
		assertThatCode(() ->
			AmountRules.requireNonNegative(amount, AMOUNT_FIELD, AMOUNT_LABEL)
		).doesNotThrowAnyException();
	}

	static Stream<BigDecimal> nonNegativeAmounts() {
		return Stream.of(ZERO, AMOUNT_AT_FOUR_DECIMALS, AMOUNT_AT_NINETEEN_DIGITS);
	}

	@ParameterizedTest(name = "requireNonNegative rejects {0}")
	@MethodSource("negativeAmounts")
	void requireNonNegative_rejectsANegativeAmount(BigDecimal amount) {
		// WHEN / THEN
		assertViolation(
			() -> AmountRules.requireNonNegative(amount, AMOUNT_FIELD, AMOUNT_LABEL),
			AMOUNT_FIELD,
			NEGATIVE_AMOUNT_MESSAGE
		);
	}

	static Stream<BigDecimal> negativeAmounts() {
		return Stream.of(NEGATIVE_AMOUNT, AMOUNT_AT_NINETEEN_DIGITS.negate());
	}

	@ParameterizedTest(name = "requirePositive accepts {0}")
	@MethodSource("positiveRates")
	void requirePositive_acceptsAPositiveAmount(BigDecimal rate) {
		// WHEN / THEN
		assertThatCode(() ->
			AmountRules.requirePositive(rate, RATE_FIELD, RATE_LABEL)
		).doesNotThrowAnyException();
	}

	static Stream<BigDecimal> positiveRates() {
		return Stream.of(RATE_AT_SIX_DECIMALS, RATE_AT_CEILING);
	}

	@ParameterizedTest(name = "requirePositive rejects {0}")
	@MethodSource("nonPositiveRates")
	void requirePositive_rejectsZeroAndNegativeAmounts(BigDecimal rate) {
		// WHEN / THEN
		assertViolation(
			() -> AmountRules.requirePositive(rate, RATE_FIELD, RATE_LABEL),
			RATE_FIELD,
			NON_POSITIVE_RATE_MESSAGE
		);
	}

	static Stream<BigDecimal> nonPositiveRates() {
		return Stream.of(ZERO, NEGATIVE_RATE);
	}

	@ParameterizedTest(name = "requireAmount accepts {0}")
	@MethodSource("amountsWithinTheMoneyColumn")
	void requireAmount_acceptsAnAmountWithinTheMoneyColumn(BigDecimal amount) {
		// WHEN / THEN
		assertThatCode(() ->
			AmountRules.requireAmount(amount, AMOUNT_FIELD, AMOUNT_LABEL)
		).doesNotThrowAnyException();
	}

	static Stream<BigDecimal> amountsWithinTheMoneyColumn() {
		return Stream.of(
			ZERO,
			AMOUNT_AT_FOUR_DECIMALS,
			AMOUNT_WITH_TRAILING_ZEROS,
			AMOUNT_AT_NINETEEN_DIGITS
		);
	}

	@ParameterizedTest(name = "requireAmount rejects {0}")
	@MethodSource("amountsOverTheMoneyScale")
	void requireAmount_rejectsMoreThanFourDecimalPlaces(BigDecimal amount) {
		// WHEN / THEN
		assertViolation(
			() -> AmountRules.requireAmount(amount, AMOUNT_FIELD, AMOUNT_LABEL),
			AMOUNT_FIELD,
			AMOUNT_SCALE_MESSAGE
		);
	}

	static Stream<BigDecimal> amountsOverTheMoneyScale() {
		return Stream.of(AMOUNT_AT_FIVE_DECIMALS, AMOUNT_OVER_FOUR_DECIMALS);
	}

	@ParameterizedTest(name = "requireAmount rejects {0}")
	@MethodSource("amountsOverTheMoneyPrecision")
	void requireAmount_rejectsMoreThanFifteenIntegerDigits(BigDecimal amount) {
		// WHEN / THEN
		assertViolation(
			() -> AmountRules.requireAmount(amount, AMOUNT_FIELD, AMOUNT_LABEL),
			AMOUNT_FIELD,
			AMOUNT_TOO_LARGE_MESSAGE
		);
	}

	static Stream<BigDecimal> amountsOverTheMoneyPrecision() {
		return Stream.of(AMOUNT_AT_SIXTEEN_INTEGER_DIGITS, AMOUNT_AT_TWENTY_INTEGER_DIGITS);
	}

	@ParameterizedTest(name = "requireExchangeRate accepts {0}")
	@MethodSource("ratesWithinTheColumnAndCeiling")
	void requireExchangeRate_acceptsARateWithinTheColumnAndTheCeiling(BigDecimal rate) {
		// WHEN / THEN
		assertThatCode(() ->
			AmountRules.requireExchangeRate(rate, RATE_FIELD, RATE_LABEL)
		).doesNotThrowAnyException();
	}

	static Stream<BigDecimal> ratesWithinTheColumnAndCeiling() {
		return Stream.of(RATE_AT_SIX_DECIMALS, RATE_AT_CEILING, RATE_AT_CEILING_WITH_TRAILING_ZEROS);
	}

	@ParameterizedTest(name = "requireExchangeRate rejects {0}")
	@MethodSource("ratesOverTheExchangeScale")
	void requireExchangeRate_rejectsMoreThanSixDecimalPlaces(BigDecimal rate) {
		// WHEN / THEN
		assertViolation(
			() -> AmountRules.requireExchangeRate(rate, RATE_FIELD, RATE_LABEL),
			RATE_FIELD,
			RATE_SCALE_MESSAGE
		);
	}

	static Stream<BigDecimal> ratesOverTheExchangeScale() {
		return Stream.of(RATE_AT_SEVEN_DECIMALS, SMALL_RATE_AT_SEVEN_DECIMALS);
	}

	@ParameterizedTest(name = "requireExchangeRate rejects {0}")
	@MethodSource("ratesOverTheCeiling")
	void requireExchangeRate_rejectsARatePastTheCeiling(BigDecimal rate) {
		// WHEN / THEN
		assertViolation(
			() -> AmountRules.requireExchangeRate(rate, RATE_FIELD, RATE_LABEL),
			RATE_FIELD,
			RATE_TOO_LARGE_MESSAGE
		);
	}

	static Stream<BigDecimal> ratesOverTheCeiling() {
		return Stream.of(RATE_JUST_ABOVE_CEILING, RATE_ABOVE_CEILING);
	}

	private static void assertViolation(
		ThrowingCallable callable,
		String expectedField,
		String expectedMessage
	) {
		ValidationException expectedViolation = catchThrowableOfType(
			callable,
			ValidationException.class
		);
		assertThat(expectedViolation.getField()).isEqualTo(expectedField);
		assertThat(expectedViolation).hasMessage(expectedMessage);
	}
}
