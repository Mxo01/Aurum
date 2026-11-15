package com.backend.aurum.domain.asset.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

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

	private static final AssetType ASSET_CATEGORY = AssetType.ASSET;

	private static final AssetType LIABILITY_CATEGORY = AssetType.LIABILITY;

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

	private static final String LIABILITY_TYPE_FIELD = "liabilityType";

	private static final String PAYMENT_FREQUENCY_FIELD = "paymentFrequency";

	private static final String PAYMENT_AMOUNT_FIELD = "paymentAmount";

	private static final String MISSING_TYPE_WITH_FIELDS_MESSAGE =
		"Liability type is required when a payment frequency or amount is set";

	private static final String NEGATIVE_AMOUNT_MESSAGE = "Payment amount cannot be negative";

	private static final String AMOUNT_SCALE_MESSAGE =
		"Payment amount cannot have more than 4 decimal places";

	private static final String MISSING_FREQUENCY_MESSAGE =
		"Payment frequency is required for an automatic liability";

	private static final String NON_POSITIVE_AUTOMATIC_AMOUNT_MESSAGE =
		"Payment amount must be greater than zero for an automatic liability";

	private static final String POSITIVE_MANUAL_AMOUNT_MESSAGE =
		"A manual liability cannot have a payment amount";

	private static final String LIABILITY_OUTSIDE_LIABILITY_CATEGORY_MESSAGE =
		"Liability type is only allowed on a liability category";

	@ParameterizedTest(name = "{0}")
	@MethodSource("rejectedConfigurations")
	void validate_rejectsInvalidConfiguration(
		String scenario,
		LiabilityType type,
		PaymentFrequency frequency,
		BigDecimal amount,
		String expectedMessage,
		String expectedField
	) {
		// GIVEN
		LiabilityConfiguration testSubject = new LiabilityConfiguration(type, frequency, amount);

		// WHEN
		ValidationException expectedViolation = catchThrowableOfType(
			testSubject::validate,
			ValidationException.class
		);

		// THEN
		assertThat(expectedViolation).hasMessage(expectedMessage);
		assertThat(expectedViolation.getField()).isEqualTo(expectedField);
	}

	static Stream<Arguments> rejectedConfigurations() {
		return Stream.of(
			Arguments.of(
				"no liability type, but a payment frequency",
				null,
				MONTHLY,
				null,
				MISSING_TYPE_WITH_FIELDS_MESSAGE,
				LIABILITY_TYPE_FIELD
			),
			Arguments.of(
				"no liability type, but a payment amount",
				null,
				null,
				LEGACY_ZERO_PAYMENT,
				MISSING_TYPE_WITH_FIELDS_MESSAGE,
				LIABILITY_TYPE_FIELD
			),
			Arguments.of(
				"no liability type, with a frequency and an amount",
				null,
				MONTHLY,
				MONTHLY_PAYMENT,
				MISSING_TYPE_WITH_FIELDS_MESSAGE,
				LIABILITY_TYPE_FIELD
			),
			Arguments.of(
				"a negative payment amount",
				MANUAL,
				null,
				NEGATIVE_PAYMENT,
				NEGATIVE_AMOUNT_MESSAGE,
				PAYMENT_AMOUNT_FIELD
			),
			Arguments.of(
				"a negative payment amount on an automatic liability",
				AUTOMATIC,
				MONTHLY,
				NEGATIVE_PAYMENT,
				NEGATIVE_AMOUNT_MESSAGE,
				PAYMENT_AMOUNT_FIELD
			),
			Arguments.of(
				"a payment amount with five decimals",
				AUTOMATIC,
				MONTHLY,
				FIVE_DECIMAL_PAYMENT,
				AMOUNT_SCALE_MESSAGE,
				PAYMENT_AMOUNT_FIELD
			),
			Arguments.of(
				"an automatic liability without a payment frequency",
				AUTOMATIC,
				null,
				MONTHLY_PAYMENT,
				MISSING_FREQUENCY_MESSAGE,
				PAYMENT_FREQUENCY_FIELD
			),
			Arguments.of(
				"an automatic liability without a payment amount",
				AUTOMATIC,
				MONTHLY,
				null,
				NON_POSITIVE_AUTOMATIC_AMOUNT_MESSAGE,
				PAYMENT_AMOUNT_FIELD
			),
			Arguments.of(
				"an automatic liability with a zero payment amount",
				AUTOMATIC,
				MONTHLY,
				LEGACY_ZERO_PAYMENT,
				NON_POSITIVE_AUTOMATIC_AMOUNT_MESSAGE,
				PAYMENT_AMOUNT_FIELD
			),
			Arguments.of(
				"a manual liability with a positive payment amount",
				MANUAL,
				MONTHLY,
				MONTHLY_PAYMENT,
				POSITIVE_MANUAL_AMOUNT_MESSAGE,
				PAYMENT_AMOUNT_FIELD
			),
			Arguments.of(
				"a manual liability without a frequency and a positive payment amount",
				MANUAL,
				null,
				MONTHLY_PAYMENT,
				POSITIVE_MANUAL_AMOUNT_MESSAGE,
				PAYMENT_AMOUNT_FIELD
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

	@ParameterizedTest(name = "{0}")
	@MethodSource("rejectedCategoryMismatches")
	void validateForCategory_rejectsALiabilityTypeOutsideALiabilityCategory(
		String scenario,
		LiabilityType type,
		PaymentFrequency frequency,
		BigDecimal amount
	) {
		// GIVEN
		LiabilityConfiguration testSubject = new LiabilityConfiguration(type, frequency, amount);

		// WHEN
		ValidationException expectedViolation = catchThrowableOfType(
			() -> testSubject.validateForCategory(ASSET_CATEGORY),
			ValidationException.class
		);

		// THEN
		assertThat(expectedViolation).hasMessage(LIABILITY_OUTSIDE_LIABILITY_CATEGORY_MESSAGE);
		assertThat(expectedViolation.getField()).isEqualTo(LIABILITY_TYPE_FIELD);
	}

	static Stream<Arguments> rejectedCategoryMismatches() {
		return Stream.of(
			Arguments.of(
				"the legacy manual liability filed under an asset category",
				MANUAL,
				MONTHLY,
				LEGACY_ZERO_PAYMENT
			),
			Arguments.of(
				"an automatic liability filed under an asset category",
				AUTOMATIC,
				MONTHLY,
				MONTHLY_PAYMENT
			),
			Arguments.of(
				"a manual liability without payment details filed under an asset category",
				MANUAL,
				null,
				null
			)
		);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("toleratedCategoryCombinations")
	void validateForCategory_acceptsEveryConsistentCombination(
		String scenario,
		LiabilityType type,
		PaymentFrequency frequency,
		BigDecimal amount,
		AssetType categoryType
	) {
		// GIVEN
		LiabilityConfiguration testSubject = new LiabilityConfiguration(type, frequency, amount);

		// WHEN / THEN
		assertThatCode(() -> testSubject.validateForCategory(categoryType)).doesNotThrowAnyException();
	}

	static Stream<Arguments> toleratedCategoryCombinations() {
		return Stream.of(
			Arguments.of(
				"a manual liability in a liability category",
				MANUAL,
				MONTHLY,
				LEGACY_ZERO_PAYMENT,
				LIABILITY_CATEGORY
			),
			Arguments.of(
				"an automatic liability in a liability category",
				AUTOMATIC,
				MONTHLY,
				MONTHLY_PAYMENT,
				LIABILITY_CATEGORY
			),
			Arguments.of("no liability type in an asset category", null, null, null, ASSET_CATEGORY),
			Arguments.of(
				"no liability type in a liability category",
				null,
				null,
				null,
				LIABILITY_CATEGORY
			),
			Arguments.of(
				"a manual liability without a category",
				MANUAL,
				MONTHLY,
				LEGACY_ZERO_PAYMENT,
				(AssetType) null
			),
			Arguments.of(
				"an automatic liability without a category",
				AUTOMATIC,
				MONTHLY,
				MONTHLY_PAYMENT,
				(AssetType) null
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
