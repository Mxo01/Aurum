package com.backend.aurum.domain.asset.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.asset.dto.CreateAssetDTO;
import com.backend.aurum.domain.asset.dto.UpdateAssetDTO;
import com.backend.aurum.domain.asset.model.LiabilityType;
import com.backend.aurum.domain.asset.model.PaymentFrequency;
import com.backend.aurum.domain.shared.exception.ValidationException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssetValidationServiceTest {

	private static final int NAME_MAX_LENGTH = 100;

	private static final String NAME_AT_MAX_LENGTH = "a".repeat(NAME_MAX_LENGTH);

	private static final String NAME_OVER_MAX_LENGTH = "a".repeat(NAME_MAX_LENGTH + 1);

	private static final LocalDate FIXED_TODAY = LocalDate.of(2026, 4, 20);

	private static final Instant FIXED_INSTANT = FIXED_TODAY.atStartOfDay(ZoneOffset.UTC).toInstant();

	private static final ZoneId FIXED_ZONE = ZoneOffset.UTC;

	private static final LocalDate TOMORROW = FIXED_TODAY.plusDays(1);

	private static final LocalDate BEYOND_TIMEZONE_SKEW = FIXED_TODAY.plusDays(2);

	private static final BigDecimal OPENING_VALUE = new BigDecimal("1000");

	private static final BigDecimal OPENING_VALUE_AT_FOUR_DECIMALS = new BigDecimal("1000.0001");

	private static final BigDecimal OPENING_VALUE_OVER_FOUR_DECIMALS = new BigDecimal("1000.00001");

	private static final BigDecimal OPENING_VALUE_OVER_FIFTEEN_INTEGER_DIGITS = new BigDecimal(
		"1234567890123456"
	);

	private static final BigDecimal NEGATIVE_OPENING_VALUE = new BigDecimal("-1000");

	private static final BigDecimal LARGE_OPENING_VALUE = new BigDecimal("5000");

	private static final BigDecimal MONTHLY_PAYMENT = new BigDecimal("200");

	private static final BigDecimal NO_PAYMENT = new BigDecimal("0.0000");

	private static final BigDecimal NEGATIVE_PAYMENT = new BigDecimal("-50");

	private static final String NAME_FIELD = "name";

	private static final String CATEGORY_ID_FIELD = "categoryId";

	private static final String PAYMENT_AMOUNT_FIELD = "paymentAmount";

	private static final String PAYMENT_FREQUENCY_FIELD = "paymentFrequency";

	private static final String LIABILITY_TYPE_FIELD = "liabilityType";

	private static final String INITIAL_VALUE_FIELD = "initialValue";

	private static final String REFERENCE_DATE_FIELD = "referenceDate";

	private static final String ASSET_DATA_REQUIRED_MESSAGE = "Asset data is required";

	private static final String NAME_REQUIRED_MESSAGE = "Asset name is required";

	private static final String NAME_TOO_LONG_MESSAGE = "Asset name cannot exceed 100 characters";

	private static final String CATEGORY_REQUIRED_MESSAGE = "Asset category is required";

	private static final String MISSING_TYPE_MESSAGE =
		"Liability type is required when a payment frequency or amount is set";

	private static final String NEGATIVE_AMOUNT_MESSAGE = "Payment amount cannot be negative";

	private static final String MISSING_FREQUENCY_MESSAGE =
		"Payment frequency is required for an automatic liability";

	private static final String NON_POSITIVE_AUTOMATIC_AMOUNT_MESSAGE =
		"Payment amount must be greater than zero for an automatic liability";

	private static final String POSITIVE_MANUAL_AMOUNT_MESSAGE =
		"A manual liability cannot have a payment amount";

	private static final String MISSING_OPENING_VALUE_MESSAGE =
		"An automatic liability needs an initial value greater than zero";

	private static final String MISSING_REFERENCE_DATE_MESSAGE =
		"An automatic liability needs a value date";

	private static final String FUTURE_REFERENCE_DATE_MESSAGE = "Value date cannot be in the future";

	private static final String NEGATIVE_AMOUNT_MESSAGE_FOR_OPENING_VALUE =
		"Amount cannot be negative";

	private static final String OPENING_VALUE_OVER_SCALE_MESSAGE =
		"Amount cannot have more than 4 decimal places";

	private static final String OPENING_VALUE_TOO_LARGE_MESSAGE = "Amount is too large";

	@Mock
	private Clock clock;

	@InjectMocks
	private AssetValidationService testSubject;

	@Test
	void validate_createDto_throwsWhenDtoIsNull() {
		// GIVEN
		CreateAssetDTO mockDto = null;

		// WHEN
		ValidationException expectedViolation = catchThrowableOfType(
			() -> testSubject.validate(mockDto),
			ValidationException.class
		);

		// THEN
		assertThat(expectedViolation).hasMessage(ASSET_DATA_REQUIRED_MESSAGE);
		assertThat(expectedViolation.getField()).isNull();
	}

	@ParameterizedTest(name = "create: {0}")
	@MethodSource("rejectedCreatePayloads")
	void validate_createDto_rejectsInvalidPayload(
		String scenario,
		Consumer<CreateAssetDTO> mutation,
		String expectedMessage,
		String expectedField
	) {
		// GIVEN
		CreateAssetDTO mockDto = buildCreateDto();
		mutation.accept(mockDto);

		// WHEN / THEN
		assertValidationViolation(() -> testSubject.validate(mockDto), expectedMessage, expectedField);
	}

	static Stream<Arguments> rejectedCreatePayloads() {
		return Stream.of(
			Arguments.of(
				"a blank name",
				(Consumer<CreateAssetDTO>) dto -> dto.setName(""),
				NAME_REQUIRED_MESSAGE,
				NAME_FIELD
			),
			Arguments.of(
				"a null name",
				(Consumer<CreateAssetDTO>) dto -> dto.setName(null),
				NAME_REQUIRED_MESSAGE,
				NAME_FIELD
			),
			Arguments.of(
				"a name over the max length",
				(Consumer<CreateAssetDTO>) dto -> dto.setName(NAME_OVER_MAX_LENGTH),
				NAME_TOO_LONG_MESSAGE,
				NAME_FIELD
			),
			Arguments.of(
				"a null category",
				(Consumer<CreateAssetDTO>) dto -> dto.setCategoryId(null),
				CATEGORY_REQUIRED_MESSAGE,
				CATEGORY_ID_FIELD
			),
			Arguments.of(
				"a payment frequency without a liability type",
				(Consumer<CreateAssetDTO>) dto -> dto.setPaymentFrequency(PaymentFrequency.MONTHLY),
				MISSING_TYPE_MESSAGE,
				LIABILITY_TYPE_FIELD
			),
			Arguments.of(
				"a payment amount without a liability type",
				(Consumer<CreateAssetDTO>) dto -> dto.setPaymentAmount(NO_PAYMENT),
				MISSING_TYPE_MESSAGE,
				LIABILITY_TYPE_FIELD
			),
			Arguments.of(
				"a negative payment amount",
				(Consumer<CreateAssetDTO>) dto -> {
					dto.setLiabilityType(LiabilityType.MANUAL);
					dto.setPaymentAmount(NEGATIVE_PAYMENT);
				},
				NEGATIVE_AMOUNT_MESSAGE,
				PAYMENT_AMOUNT_FIELD
			),
			Arguments.of(
				"an automatic liability without a payment frequency",
				(Consumer<CreateAssetDTO>) dto -> {
					dto.setLiabilityType(LiabilityType.AUTOMATIC);
					dto.setPaymentAmount(MONTHLY_PAYMENT);
				},
				MISSING_FREQUENCY_MESSAGE,
				PAYMENT_FREQUENCY_FIELD
			),
			Arguments.of(
				"an automatic liability with a zero payment amount",
				(Consumer<CreateAssetDTO>) dto -> {
					dto.setLiabilityType(LiabilityType.AUTOMATIC);
					dto.setPaymentFrequency(PaymentFrequency.MONTHLY);
					dto.setPaymentAmount(NO_PAYMENT);
				},
				NON_POSITIVE_AUTOMATIC_AMOUNT_MESSAGE,
				PAYMENT_AMOUNT_FIELD
			),
			Arguments.of(
				"a manual liability with a positive payment amount",
				(Consumer<CreateAssetDTO>) dto -> {
					dto.setLiabilityType(LiabilityType.MANUAL);
					dto.setPaymentFrequency(PaymentFrequency.MONTHLY);
					dto.setPaymentAmount(MONTHLY_PAYMENT);
				},
				POSITIVE_MANUAL_AMOUNT_MESSAGE,
				PAYMENT_AMOUNT_FIELD
			),
			Arguments.of(
				"an automatic liability without an opening value",
				(Consumer<CreateAssetDTO>) dto -> {
					dto.setLiabilityType(LiabilityType.AUTOMATIC);
					dto.setPaymentFrequency(PaymentFrequency.MONTHLY);
					dto.setPaymentAmount(MONTHLY_PAYMENT);
					dto.setReferenceDate(FIXED_TODAY);
				},
				MISSING_OPENING_VALUE_MESSAGE,
				INITIAL_VALUE_FIELD
			),
			Arguments.of(
				"an automatic liability with a zero opening value",
				(Consumer<CreateAssetDTO>) dto -> {
					dto.setLiabilityType(LiabilityType.AUTOMATIC);
					dto.setPaymentFrequency(PaymentFrequency.MONTHLY);
					dto.setPaymentAmount(MONTHLY_PAYMENT);
					dto.setInitialValue(NO_PAYMENT);
					dto.setReferenceDate(FIXED_TODAY);
				},
				MISSING_OPENING_VALUE_MESSAGE,
				INITIAL_VALUE_FIELD
			),
			Arguments.of(
				"an automatic liability without a value date",
				(Consumer<CreateAssetDTO>) dto -> {
					dto.setLiabilityType(LiabilityType.AUTOMATIC);
					dto.setPaymentFrequency(PaymentFrequency.MONTHLY);
					dto.setPaymentAmount(MONTHLY_PAYMENT);
					dto.setInitialValue(OPENING_VALUE);
				},
				MISSING_REFERENCE_DATE_MESSAGE,
				REFERENCE_DATE_FIELD
			),
			Arguments.of(
				"an automatic liability with a negative opening value",
				(Consumer<CreateAssetDTO>) dto -> {
					dto.setLiabilityType(LiabilityType.AUTOMATIC);
					dto.setPaymentFrequency(PaymentFrequency.MONTHLY);
					dto.setPaymentAmount(MONTHLY_PAYMENT);
					dto.setInitialValue(NEGATIVE_OPENING_VALUE);
				},
				MISSING_OPENING_VALUE_MESSAGE,
				INITIAL_VALUE_FIELD
			),
			Arguments.of(
				"a manual liability with a negative opening value",
				(Consumer<CreateAssetDTO>) dto -> {
					dto.setLiabilityType(LiabilityType.MANUAL);
					dto.setInitialValue(NEGATIVE_OPENING_VALUE);
				},
				NEGATIVE_AMOUNT_MESSAGE_FOR_OPENING_VALUE,
				INITIAL_VALUE_FIELD
			),
			Arguments.of(
				"an asset without a liability and a negative opening value",
				(Consumer<CreateAssetDTO>) dto -> dto.setInitialValue(NEGATIVE_OPENING_VALUE),
				NEGATIVE_AMOUNT_MESSAGE_FOR_OPENING_VALUE,
				INITIAL_VALUE_FIELD
			),
			Arguments.of(
				"a manual liability with an opening value over four decimals",
				(Consumer<CreateAssetDTO>) dto -> {
					dto.setLiabilityType(LiabilityType.MANUAL);
					dto.setInitialValue(OPENING_VALUE_OVER_FOUR_DECIMALS);
				},
				OPENING_VALUE_OVER_SCALE_MESSAGE,
				INITIAL_VALUE_FIELD
			),
			Arguments.of(
				"a manual liability with an opening value over fifteen integer digits",
				(Consumer<CreateAssetDTO>) dto -> {
					dto.setLiabilityType(LiabilityType.MANUAL);
					dto.setInitialValue(OPENING_VALUE_OVER_FIFTEEN_INTEGER_DIGITS);
				},
				OPENING_VALUE_TOO_LARGE_MESSAGE,
				INITIAL_VALUE_FIELD
			)
		);
	}

	@ParameterizedTest(name = "create: an automatic liability with {0}")
	@MethodSource("automaticOpeningValuesOutsideTheAmountRules")
	void validate_createDto_rejectsAnAutomaticOpeningValueOutsideTheAmountRules(
		String scenario,
		BigDecimal openingValue,
		String expectedMessage
	) {
		// GIVEN
		stubFixedToday();
		CreateAssetDTO mockDto = buildAutomaticCreateDto();
		mockDto.setInitialValue(openingValue);
		mockDto.setReferenceDate(FIXED_TODAY);

		// WHEN / THEN
		assertValidationViolation(
			() -> testSubject.validate(mockDto),
			expectedMessage,
			INITIAL_VALUE_FIELD
		);
	}

	static Stream<Arguments> automaticOpeningValuesOutsideTheAmountRules() {
		return Stream.of(
			Arguments.of(
				"an opening value over four decimals",
				OPENING_VALUE_OVER_FOUR_DECIMALS,
				OPENING_VALUE_OVER_SCALE_MESSAGE
			),
			Arguments.of(
				"an opening value over fifteen integer digits",
				OPENING_VALUE_OVER_FIFTEEN_INTEGER_DIGITS,
				OPENING_VALUE_TOO_LARGE_MESSAGE
			)
		);
	}

	@Test
	void validate_createDto_attributesANegativeAmountAndAMissingOpeningValueToDifferentFields() {
		// GIVEN
		CreateAssetDTO mockNegativeAmountDto = buildCreateDto();
		mockNegativeAmountDto.setLiabilityType(LiabilityType.MANUAL);
		mockNegativeAmountDto.setPaymentAmount(NEGATIVE_PAYMENT);
		CreateAssetDTO mockMissingOpeningValueDto = buildAutomaticCreateDto();
		mockMissingOpeningValueDto.setReferenceDate(FIXED_TODAY);

		// WHEN
		ValidationException expectedAmountViolation = catchThrowableOfType(
			() -> testSubject.validate(mockNegativeAmountDto),
			ValidationException.class
		);
		ValidationException expectedOpeningValueViolation = catchThrowableOfType(
			() -> testSubject.validate(mockMissingOpeningValueDto),
			ValidationException.class
		);

		// THEN
		assertThat(expectedAmountViolation.getField()).isEqualTo(PAYMENT_AMOUNT_FIELD);
		assertThat(expectedAmountViolation).hasMessage(NEGATIVE_AMOUNT_MESSAGE);
		assertThat(expectedOpeningValueViolation.getField()).isEqualTo(INITIAL_VALUE_FIELD);
		assertThat(expectedOpeningValueViolation).hasMessage(MISSING_OPENING_VALUE_MESSAGE);
		assertThat(expectedAmountViolation.getField()).isNotEqualTo(
			expectedOpeningValueViolation.getField()
		);
	}

	@ParameterizedTest(name = "create: an opening snapshot dated {0} is accepted")
	@MethodSource("toleratedReferenceDates")
	void validate_createDto_acceptsAReferenceDateWithinTheTimezoneSkew(LocalDate referenceDate) {
		// GIVEN
		stubFixedToday();
		CreateAssetDTO mockDto = buildAutomaticCreateDto();
		mockDto.setInitialValue(OPENING_VALUE);
		mockDto.setReferenceDate(referenceDate);

		// WHEN / THEN
		assertThatCode(() -> testSubject.validate(mockDto)).doesNotThrowAnyException();
	}

	@Test
	void validate_createDto_rejectsAReferenceDateBeyondTheTimezoneSkew() {
		// GIVEN
		stubFixedToday();
		CreateAssetDTO mockDto = buildAutomaticCreateDto();
		mockDto.setInitialValue(OPENING_VALUE);
		mockDto.setReferenceDate(BEYOND_TIMEZONE_SKEW);

		// WHEN / THEN
		assertValidationViolation(
			() -> testSubject.validate(mockDto),
			FUTURE_REFERENCE_DATE_MESSAGE,
			REFERENCE_DATE_FIELD
		);
	}

	@Test
	void validate_createDto_acceptsAFutureReferenceDateForAManualLiability() {
		// GIVEN
		CreateAssetDTO mockDto = buildCreateDto();
		mockDto.setLiabilityType(LiabilityType.MANUAL);
		mockDto.setPaymentFrequency(PaymentFrequency.MONTHLY);
		mockDto.setPaymentAmount(NO_PAYMENT);
		mockDto.setInitialValue(LARGE_OPENING_VALUE);
		mockDto.setReferenceDate(BEYOND_TIMEZONE_SKEW);

		// WHEN / THEN
		assertThatCode(() -> testSubject.validate(mockDto)).doesNotThrowAnyException();
	}

	@Test
	void validate_createDto_acceptsAManualOpeningValueAtTheMoneyScaleAndPrecision() {
		// GIVEN
		CreateAssetDTO mockDto = buildCreateDto();
		mockDto.setLiabilityType(LiabilityType.MANUAL);
		mockDto.setInitialValue(OPENING_VALUE_AT_FOUR_DECIMALS);
		mockDto.setReferenceDate(BEYOND_TIMEZONE_SKEW);

		// WHEN / THEN
		assertThatCode(() -> testSubject.validate(mockDto)).doesNotThrowAnyException();
	}

	@Test
	void validate_createDto_doesNotThrowForValidDto() {
		// GIVEN
		CreateAssetDTO mockDto = buildCreateDto();

		// WHEN / THEN
		assertThatCode(() -> testSubject.validate(mockDto)).doesNotThrowAnyException();
	}

	@Test
	void validate_updateDto_throwsWhenDtoIsNull() {
		// GIVEN
		UpdateAssetDTO mockDto = null;

		// WHEN
		ValidationException expectedViolation = catchThrowableOfType(
			() -> testSubject.validate(mockDto),
			ValidationException.class
		);

		// THEN
		assertThat(expectedViolation).hasMessage(ASSET_DATA_REQUIRED_MESSAGE);
		assertThat(expectedViolation.getField()).isNull();
	}

	@ParameterizedTest(name = "update: {0}")
	@MethodSource("rejectedUpdatePayloads")
	void validate_updateDto_rejectsInvalidPayload(
		String scenario,
		Consumer<UpdateAssetDTO> mutation,
		String expectedMessage,
		String expectedField
	) {
		// GIVEN
		UpdateAssetDTO mockDto = buildUpdateDto();
		mutation.accept(mockDto);

		// WHEN / THEN
		assertValidationViolation(() -> testSubject.validate(mockDto), expectedMessage, expectedField);
	}

	static Stream<Arguments> rejectedUpdatePayloads() {
		return Stream.of(
			Arguments.of(
				"a blank name",
				(Consumer<UpdateAssetDTO>) dto -> dto.setName(""),
				NAME_REQUIRED_MESSAGE,
				NAME_FIELD
			),
			Arguments.of(
				"a null name",
				(Consumer<UpdateAssetDTO>) dto -> dto.setName(null),
				NAME_REQUIRED_MESSAGE,
				NAME_FIELD
			),
			Arguments.of(
				"a name over the max length",
				(Consumer<UpdateAssetDTO>) dto -> dto.setName(NAME_OVER_MAX_LENGTH),
				NAME_TOO_LONG_MESSAGE,
				NAME_FIELD
			),
			Arguments.of(
				"a payment frequency without a liability type",
				(Consumer<UpdateAssetDTO>) dto -> dto.setPaymentFrequency(PaymentFrequency.MONTHLY),
				MISSING_TYPE_MESSAGE,
				LIABILITY_TYPE_FIELD
			),
			Arguments.of(
				"an automatic liability without a payment amount",
				(Consumer<UpdateAssetDTO>) dto -> {
					dto.setLiabilityType(LiabilityType.AUTOMATIC);
					dto.setPaymentFrequency(PaymentFrequency.MONTHLY);
				},
				NON_POSITIVE_AUTOMATIC_AMOUNT_MESSAGE,
				PAYMENT_AMOUNT_FIELD
			)
		);
	}

	@Test
	void validate_updateDto_doesNotThrowForValidDto() {
		// GIVEN
		UpdateAssetDTO mockDto = buildUpdateDto();

		// WHEN / THEN
		assertThatCode(() -> testSubject.validate(mockDto)).doesNotThrowAnyException();
	}

	static Stream<LocalDate> toleratedReferenceDates() {
		return Stream.of(FIXED_TODAY, TOMORROW);
	}

	private static void assertValidationViolation(
		ThrowingCallable callable,
		String expectedMessage,
		String expectedField
	) {
		ValidationException expectedViolation = catchThrowableOfType(
			callable,
			ValidationException.class
		);
		assertThat(expectedViolation).hasMessage(expectedMessage);
		assertThat(expectedViolation.getField()).isEqualTo(expectedField);
	}

	private void stubFixedToday() {
		when(clock.instant()).thenReturn(FIXED_INSTANT);
		when(clock.getZone()).thenReturn(FIXED_ZONE);
	}

	private static CreateAssetDTO buildCreateDto() {
		CreateAssetDTO dto = new CreateAssetDTO();
		dto.setName(NAME_AT_MAX_LENGTH);
		dto.setCategoryId(UUID.randomUUID());
		return dto;
	}

	private static CreateAssetDTO buildAutomaticCreateDto() {
		CreateAssetDTO dto = buildCreateDto();
		dto.setLiabilityType(LiabilityType.AUTOMATIC);
		dto.setPaymentFrequency(PaymentFrequency.MONTHLY);
		dto.setPaymentAmount(MONTHLY_PAYMENT);
		return dto;
	}

	private static UpdateAssetDTO buildUpdateDto() {
		UpdateAssetDTO dto = new UpdateAssetDTO();
		dto.setId(UUID.randomUUID());
		dto.setName(NAME_AT_MAX_LENGTH);
		dto.setCategoryId(UUID.randomUUID());
		return dto;
	}
}
