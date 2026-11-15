package com.backend.aurum.domain.asset.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.backend.aurum.domain.shared.exception.ValidationException;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class LiabilityConfigurationTest {

	private static final LiabilityType MANUAL = LiabilityType.MANUAL;

	private static final LiabilityType AUTOMATIC = LiabilityType.AUTOMATIC;

	private static final PaymentFrequency MONTHLY = PaymentFrequency.MONTHLY;

	private static final PaymentFrequency YEARLY = PaymentFrequency.YEARLY;

	private static final BigDecimal MONTHLY_PAYMENT = new BigDecimal("200");

	private static final BigDecimal SAME_PAYMENT_AT_A_WIDER_SCALE = new BigDecimal("200.00");

	private static final BigDecimal PADDED_PAYMENT = new BigDecimal("200.00000");

	private static final BigDecimal OTHER_PAYMENT = new BigDecimal("250");

	private static final BigDecimal LEGACY_ZERO_PAYMENT = new BigDecimal("0.0000");

	private static final BigDecimal NEGATIVE_PAYMENT = new BigDecimal("-50");

	private static final BigDecimal FOUR_DECIMAL_PAYMENT = new BigDecimal("1.1234");

	private static final BigDecimal FIVE_DECIMAL_PAYMENT = new BigDecimal("1.12345");

	private static final String MISSING_TYPE_WITH_FIELDS_MESSAGE =
		"liabilityType is required when paymentFrequency or paymentAmount is provided";

	private static final String NEGATIVE_AMOUNT_MESSAGE = "paymentAmount cannot be negative";

	private static final String AMOUNT_SCALE_MESSAGE = "paymentAmount supports at most 4 decimals";

	private static final String MISSING_FREQUENCY_MESSAGE =
		"paymentFrequency is required for an AUTOMATIC liability";

	private static final String NON_POSITIVE_AUTOMATIC_AMOUNT_MESSAGE =
		"paymentAmount must be greater than 0 for an AUTOMATIC liability";

	private static final String POSITIVE_MANUAL_AMOUNT_MESSAGE =
		"A MANUAL liability cannot carry a positive paymentAmount";

	@ParameterizedTest(name = "{0}")
	@MethodSource("rejectedConfigurations")
	void validate_rejectsInvalidConfiguration(
		String scenario,
		LiabilityType type,
		PaymentFrequency frequency,
		BigDecimal amount,
		String expectedMessage
	) {
		// GIVEN
		LiabilityConfiguration testSubject = new LiabilityConfiguration(type, frequency, amount);

		// WHEN / THEN
		assertThatThrownBy(testSubject::validate)
			.isInstanceOf(ValidationException.class)
			.hasMessage(expectedMessage);
	}

	static Stream<Arguments> rejectedConfigurations() {
		return Stream.of(
			Arguments.of(
				"no liability type, but a payment frequency",
				null,
				MONTHLY,
				null,
				MISSING_TYPE_WITH_FIELDS_MESSAGE
			),
			Arguments.of(
				"no liability type, but a payment amount",
				null,
				null,
				LEGACY_ZERO_PAYMENT,
				MISSING_TYPE_WITH_FIELDS_MESSAGE
			),
			Arguments.of(
				"no liability type, with a frequency and an amount",
				null,
				MONTHLY,
				MONTHLY_PAYMENT,
				MISSING_TYPE_WITH_FIELDS_MESSAGE
			),
			Arguments.of(
				"a negative payment amount",
				MANUAL,
				null,
				NEGATIVE_PAYMENT,
				NEGATIVE_AMOUNT_MESSAGE
			),
			Arguments.of(
				"a negative payment amount on an automatic liability",
				AUTOMATIC,
				MONTHLY,
				NEGATIVE_PAYMENT,
				NEGATIVE_AMOUNT_MESSAGE
			),
			Arguments.of(
				"a payment amount with five decimals",
				AUTOMATIC,
				MONTHLY,
				FIVE_DECIMAL_PAYMENT,
				AMOUNT_SCALE_MESSAGE
			),
			Arguments.of(
				"an automatic liability without a payment frequency",
				AUTOMATIC,
				null,
				MONTHLY_PAYMENT,
				MISSING_FREQUENCY_MESSAGE
			),
			Arguments.of(
				"an automatic liability without a payment amount",
				AUTOMATIC,
				MONTHLY,
				null,
				NON_POSITIVE_AUTOMATIC_AMOUNT_MESSAGE
			),
			Arguments.of(
				"an automatic liability with a zero payment amount",
				AUTOMATIC,
				MONTHLY,
				LEGACY_ZERO_PAYMENT,
				NON_POSITIVE_AUTOMATIC_AMOUNT_MESSAGE
			),
			Arguments.of(
				"a manual liability with a positive payment amount",
				MANUAL,
				MONTHLY,
				MONTHLY_PAYMENT,
				POSITIVE_MANUAL_AMOUNT_MESSAGE
			),
			Arguments.of(
				"a manual liability without a frequency and a positive payment amount",
				MANUAL,
				null,
				MONTHLY_PAYMENT,
				POSITIVE_MANUAL_AMOUNT_MESSAGE
			)
		);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("toleratedConfigurations")
	void validate_acceptsValidConfiguration(
		String scenario,
		LiabilityType type,
		PaymentFrequency frequency,
		BigDecimal amount
	) {
		// GIVEN
		LiabilityConfiguration testSubject = new LiabilityConfiguration(type, frequency, amount);

		// WHEN / THEN
		assertThatCode(testSubject::validate).doesNotThrowAnyException();
	}

	static Stream<Arguments> toleratedConfigurations() {
		return Stream.of(
			Arguments.of("an asset without a liability block", null, null, null),
			Arguments.of(
				"the legacy manual liability with a zero payment",
				MANUAL,
				MONTHLY,
				LEGACY_ZERO_PAYMENT
			),
			Arguments.of("a manual liability without an amount", MANUAL, MONTHLY, null),
			Arguments.of("a manual liability without a frequency", MANUAL, null, null),
			Arguments.of(
				"an automatic liability with a positive payment",
				AUTOMATIC,
				MONTHLY,
				MONTHLY_PAYMENT
			),
			Arguments.of("a payment amount with four decimals", AUTOMATIC, MONTHLY, FOUR_DECIMAL_PAYMENT),
			Arguments.of(
				"a payment amount padded with trailing zeros",
				AUTOMATIC,
				MONTHLY,
				PADDED_PAYMENT
			)
		);
	}

	@Test
	void isAutomatic_isTrueForAnAutomaticLiability() {
		// GIVEN
		LiabilityConfiguration testSubject = new LiabilityConfiguration(
			AUTOMATIC,
			MONTHLY,
			MONTHLY_PAYMENT
		);

		// WHEN
		boolean expectedAutomatic = testSubject.isAutomatic();

		// THEN
		assertThat(expectedAutomatic).isTrue();
	}

	@Test
	void isAutomatic_isFalseForAManualLiability() {
		// GIVEN
		LiabilityConfiguration testSubject = new LiabilityConfiguration(
			MANUAL,
			MONTHLY,
			LEGACY_ZERO_PAYMENT
		);

		// WHEN
		boolean expectedAutomatic = testSubject.isAutomatic();

		// THEN
		assertThat(expectedAutomatic).isFalse();
	}

	@Test
	void isAutomatic_isFalseWhenTheLiabilityTypeIsAbsent() {
		// GIVEN
		LiabilityConfiguration testSubject = new LiabilityConfiguration(null, null, null);

		// WHEN
		boolean expectedAutomatic = testSubject.isAutomatic();

		// THEN
		assertThat(expectedAutomatic).isFalse();
	}

	@Test
	void of_readsTheLiabilityBlockOfTheAsset() {
		// GIVEN
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getLiabilityType), AUTOMATIC)
			.set(Select.field(Asset::getPaymentFrequency), YEARLY)
			.set(Select.field(Asset::getPaymentAmount), MONTHLY_PAYMENT)
			.create();

		// WHEN
		LiabilityConfiguration expectedConfiguration = LiabilityConfiguration.of(mockAsset);

		// THEN
		assertThat(expectedConfiguration).isEqualTo(
			new LiabilityConfiguration(AUTOMATIC, YEARLY, MONTHLY_PAYMENT)
		);
	}

	@Test
	void equals_isTrueForTheSameInstance() {
		// GIVEN
		LiabilityConfiguration testSubject = new LiabilityConfiguration(
			AUTOMATIC,
			MONTHLY,
			MONTHLY_PAYMENT
		);

		// WHEN
		boolean expectedEqual = testSubject.equals(testSubject);

		// THEN
		assertThat(expectedEqual).isTrue();
	}

	@Test
	void equals_isValueBasedAcrossScales() {
		// GIVEN
		LiabilityConfiguration testSubject = new LiabilityConfiguration(
			AUTOMATIC,
			MONTHLY,
			MONTHLY_PAYMENT
		);
		LiabilityConfiguration mockWiderScale = new LiabilityConfiguration(
			AUTOMATIC,
			MONTHLY,
			SAME_PAYMENT_AT_A_WIDER_SCALE
		);

		// WHEN
		boolean expectedEqual = testSubject.equals(mockWiderScale);

		// THEN
		assertThat(expectedEqual).isTrue();
		assertThat(testSubject).hasSameHashCodeAs(mockWiderScale);
	}

	@Test
	void equals_isFalseForADifferentLiabilityType() {
		// GIVEN
		LiabilityConfiguration testSubject = new LiabilityConfiguration(
			MANUAL,
			MONTHLY,
			MONTHLY_PAYMENT
		);
		LiabilityConfiguration mockAutomatic = new LiabilityConfiguration(
			AUTOMATIC,
			MONTHLY,
			MONTHLY_PAYMENT
		);

		// WHEN
		boolean expectedEqual = testSubject.equals(mockAutomatic);

		// THEN
		assertThat(expectedEqual).isFalse();
	}

	@Test
	void equals_isFalseForADifferentPaymentFrequency() {
		// GIVEN
		LiabilityConfiguration testSubject = new LiabilityConfiguration(
			AUTOMATIC,
			MONTHLY,
			MONTHLY_PAYMENT
		);
		LiabilityConfiguration mockYearly = new LiabilityConfiguration(
			AUTOMATIC,
			YEARLY,
			MONTHLY_PAYMENT
		);

		// WHEN
		boolean expectedEqual = testSubject.equals(mockYearly);

		// THEN
		assertThat(expectedEqual).isFalse();
	}

	@Test
	void equals_isFalseForADifferentPaymentAmount() {
		// GIVEN
		LiabilityConfiguration testSubject = new LiabilityConfiguration(
			AUTOMATIC,
			MONTHLY,
			MONTHLY_PAYMENT
		);
		LiabilityConfiguration mockOtherAmount = new LiabilityConfiguration(
			AUTOMATIC,
			MONTHLY,
			OTHER_PAYMENT
		);

		// WHEN
		boolean expectedEqual = testSubject.equals(mockOtherAmount);

		// THEN
		assertThat(expectedEqual).isFalse();
	}

	@Test
	void equals_isFalseForAnotherTypeOfObject() {
		// GIVEN
		LiabilityConfiguration testSubject = new LiabilityConfiguration(
			AUTOMATIC,
			MONTHLY,
			MONTHLY_PAYMENT
		);

		// WHEN
		boolean expectedEqual = testSubject.equals(MONTHLY_PAYMENT);

		// THEN
		assertThat(expectedEqual).isFalse();
	}

	@Test
	void equals_isTrueWhenBothConfigurationsHaveNoPaymentAmount() {
		// GIVEN
		LiabilityConfiguration testSubject = new LiabilityConfiguration(MANUAL, MONTHLY, null);
		LiabilityConfiguration mockSameShape = new LiabilityConfiguration(MANUAL, MONTHLY, null);

		// WHEN
		boolean expectedEqual = testSubject.equals(mockSameShape);

		// THEN
		assertThat(expectedEqual).isTrue();
		assertThat(testSubject).hasSameHashCodeAs(mockSameShape);
	}

	@Test
	void equals_isFalseWhenOnlyOneConfigurationHasAPaymentAmount() {
		// GIVEN
		LiabilityConfiguration testSubject = new LiabilityConfiguration(MANUAL, MONTHLY, null);
		LiabilityConfiguration mockWithAmount = new LiabilityConfiguration(
			MANUAL,
			MONTHLY,
			LEGACY_ZERO_PAYMENT
		);

		// WHEN
		boolean expectedEqual = testSubject.equals(mockWithAmount);
		boolean expectedReverseEqual = mockWithAmount.equals(testSubject);

		// THEN
		assertThat(expectedEqual).isFalse();
		assertThat(expectedReverseEqual).isFalse();
	}
}
