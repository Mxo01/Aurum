package com.backend.aurum.domain.asset.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

	private static final BigDecimal LARGE_OPENING_VALUE = new BigDecimal("5000");

	private static final BigDecimal MONTHLY_PAYMENT = new BigDecimal("200");

	private static final BigDecimal NO_PAYMENT = new BigDecimal("0.0000");

	private static final BigDecimal NEGATIVE_PAYMENT = new BigDecimal("-50");

	private static final String MISSING_TYPE_MESSAGE =
		"liabilityType is required when paymentFrequency or paymentAmount is provided";

	private static final String NEGATIVE_AMOUNT_MESSAGE = "paymentAmount cannot be negative";

	private static final String MISSING_FREQUENCY_MESSAGE =
		"paymentFrequency is required for an AUTOMATIC liability";

	private static final String NON_POSITIVE_AUTOMATIC_AMOUNT_MESSAGE =
		"paymentAmount must be greater than 0 for an AUTOMATIC liability";

	private static final String POSITIVE_MANUAL_AMOUNT_MESSAGE =
		"A MANUAL liability cannot carry a positive paymentAmount";

	private static final String MISSING_OPENING_VALUE_MESSAGE =
		"An AUTOMATIC liability requires an initialValue greater than 0";

	private static final String MISSING_REFERENCE_DATE_MESSAGE =
		"An AUTOMATIC liability requires a referenceDate";

	private static final String FUTURE_REFERENCE_DATE_MESSAGE =
		"referenceDate cannot be in the future";

	@Mock
	private Clock clock;

	@InjectMocks
	private AssetValidationService testSubject;

	@Test
	void validate_createDto_throwsWhenDtoIsNull() {
		// GIVEN
		CreateAssetDTO mockDto = null;

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto)).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	@ParameterizedTest(name = "create: {0}")
	@MethodSource("rejectedCreatePayloads")
	void validate_createDto_rejectsInvalidPayload(
		String scenario,
		Consumer<CreateAssetDTO> mutation
	) {
		// GIVEN
		CreateAssetDTO mockDto = buildCreateDto();
		mutation.accept(mockDto);

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto)).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	static Stream<Arguments> rejectedCreatePayloads() {
		return Stream.of(
			Arguments.of("blank name", (Consumer<CreateAssetDTO>) dto -> dto.setName("")),
			Arguments.of("null name", (Consumer<CreateAssetDTO>) dto -> dto.setName(null)),
			Arguments.of(
				"name over max length",
				(Consumer<CreateAssetDTO>) dto -> dto.setName(NAME_OVER_MAX_LENGTH)
			),
			Arguments.of("null category", (Consumer<CreateAssetDTO>) dto -> dto.setCategoryId(null))
		);
	}

	@ParameterizedTest(name = "create: {0}")
	@MethodSource("rejectedCreateLiabilityPayloads")
	void validate_createDto_rejectsInvalidLiabilityConfiguration(
		String scenario,
		Consumer<CreateAssetDTO> mutation,
		String expectedMessage
	) {
		// GIVEN
		CreateAssetDTO mockDto = buildCreateDto();
		mutation.accept(mockDto);

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto))
			.isInstanceOf(ValidationException.class)
			.hasMessage(expectedMessage);
	}

	static Stream<Arguments> rejectedCreateLiabilityPayloads() {
		return Stream.of(
			Arguments.of(
				"a payment frequency without a liability type",
				(Consumer<CreateAssetDTO>) dto -> dto.setPaymentFrequency(PaymentFrequency.MONTHLY),
				MISSING_TYPE_MESSAGE
			),
			Arguments.of(
				"a payment amount without a liability type",
				(Consumer<CreateAssetDTO>) dto -> dto.setPaymentAmount(NO_PAYMENT),
				MISSING_TYPE_MESSAGE
			),
			Arguments.of(
				"a negative payment amount",
				(Consumer<CreateAssetDTO>) dto -> {
					dto.setLiabilityType(LiabilityType.MANUAL);
					dto.setPaymentAmount(NEGATIVE_PAYMENT);
				},
				NEGATIVE_AMOUNT_MESSAGE
			),
			Arguments.of(
				"an automatic liability without a payment frequency",
				(Consumer<CreateAssetDTO>) dto -> {
					dto.setLiabilityType(LiabilityType.AUTOMATIC);
					dto.setPaymentAmount(MONTHLY_PAYMENT);
				},
				MISSING_FREQUENCY_MESSAGE
			),
			Arguments.of(
				"an automatic liability with a zero payment amount",
				(Consumer<CreateAssetDTO>) dto -> {
					dto.setLiabilityType(LiabilityType.AUTOMATIC);
					dto.setPaymentFrequency(PaymentFrequency.MONTHLY);
					dto.setPaymentAmount(NO_PAYMENT);
				},
				NON_POSITIVE_AUTOMATIC_AMOUNT_MESSAGE
			),
			Arguments.of(
				"a manual liability with a positive payment amount",
				(Consumer<CreateAssetDTO>) dto -> {
					dto.setLiabilityType(LiabilityType.MANUAL);
					dto.setPaymentFrequency(PaymentFrequency.MONTHLY);
					dto.setPaymentAmount(MONTHLY_PAYMENT);
				},
				POSITIVE_MANUAL_AMOUNT_MESSAGE
			),
			Arguments.of(
				"an automatic liability without an opening value",
				(Consumer<CreateAssetDTO>) dto -> {
					dto.setLiabilityType(LiabilityType.AUTOMATIC);
					dto.setPaymentFrequency(PaymentFrequency.MONTHLY);
					dto.setPaymentAmount(MONTHLY_PAYMENT);
					dto.setReferenceDate(FIXED_TODAY);
				},
				MISSING_OPENING_VALUE_MESSAGE
			)
		);
	}

	@Test
	void validate_createDto_rejectsAnAutomaticLiabilityWithAZeroOpeningValue() {
		// GIVEN
		CreateAssetDTO mockDto = buildAutomaticCreateDto();
		mockDto.setInitialValue(NO_PAYMENT);

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto))
			.isInstanceOf(ValidationException.class)
			.hasMessage(MISSING_OPENING_VALUE_MESSAGE);
	}

	@Test
	void validate_createDto_rejectsAnAutomaticLiabilityWithoutAReferenceDate() {
		// GIVEN
		CreateAssetDTO mockDto = buildAutomaticCreateDto();
		mockDto.setInitialValue(OPENING_VALUE);

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto))
			.isInstanceOf(ValidationException.class)
			.hasMessage(MISSING_REFERENCE_DATE_MESSAGE);
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
		assertThatThrownBy(() -> testSubject.validate(mockDto))
			.isInstanceOf(ValidationException.class)
			.hasMessage(FUTURE_REFERENCE_DATE_MESSAGE);
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

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto)).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	@ParameterizedTest(name = "update: {0}")
	@MethodSource("rejectedUpdatePayloads")
	void validate_updateDto_rejectsInvalidPayload(
		String scenario,
		Consumer<UpdateAssetDTO> mutation
	) {
		// GIVEN
		UpdateAssetDTO mockDto = buildUpdateDto();
		mutation.accept(mockDto);

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto)).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	static Stream<Arguments> rejectedUpdatePayloads() {
		return Stream.of(
			Arguments.of("blank name", (Consumer<UpdateAssetDTO>) dto -> dto.setName("")),
			Arguments.of("null name", (Consumer<UpdateAssetDTO>) dto -> dto.setName(null)),
			Arguments.of(
				"name over max length",
				(Consumer<UpdateAssetDTO>) dto -> dto.setName(NAME_OVER_MAX_LENGTH)
			)
		);
	}

	@ParameterizedTest(name = "update: {0}")
	@MethodSource("rejectedUpdateLiabilityPayloads")
	void validate_updateDto_rejectsInvalidLiabilityConfiguration(
		String scenario,
		Consumer<UpdateAssetDTO> mutation,
		String expectedMessage
	) {
		// GIVEN
		UpdateAssetDTO mockDto = buildUpdateDto();
		mutation.accept(mockDto);

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto))
			.isInstanceOf(ValidationException.class)
			.hasMessage(expectedMessage);
	}

	static Stream<Arguments> rejectedUpdateLiabilityPayloads() {
		return Stream.of(
			Arguments.of(
				"a payment frequency without a liability type",
				(Consumer<UpdateAssetDTO>) dto -> dto.setPaymentFrequency(PaymentFrequency.MONTHLY),
				MISSING_TYPE_MESSAGE
			),
			Arguments.of(
				"an automatic liability without a payment amount",
				(Consumer<UpdateAssetDTO>) dto -> {
					dto.setLiabilityType(LiabilityType.AUTOMATIC);
					dto.setPaymentFrequency(PaymentFrequency.MONTHLY);
				},
				NON_POSITIVE_AUTOMATIC_AMOUNT_MESSAGE
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
