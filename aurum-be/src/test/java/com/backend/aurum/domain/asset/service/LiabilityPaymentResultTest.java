package com.backend.aurum.domain.asset.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.backend.aurum.domain.asset.model.LiabilityPaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class LiabilityPaymentResultTest {

	private static final int ONE_PERIOD = 1;

	private static final int THREE_PERIODS = 3;

	private static final int FIVE_PERIODS = 5;

	private static final LocalDate NEXT_DUE_DATE = LocalDate.of(2026, 5, 20);

	private static final String SKIP_REASON = "no payment frequency";

	private static final BigDecimal APPLIED_AMOUNT = new BigDecimal("100");

	private static final BigDecimal CONFIGURED_PAYMENT_AMOUNT = new BigDecimal("720");

	private static final BigDecimal OUTSTANDING_BALANCE = new BigDecimal("0");

	private static Stream<Arguments> clampExpectations() {
		return Stream.of(
			Arguments.of("nothing applied", LiabilityPaymentResult.skipped(SKIP_REASON), false),
			Arguments.of(
				"no configured amount",
				LiabilityPaymentResult.paid(
					ONE_PERIOD,
					OUTSTANDING_BALANCE,
					NEXT_DUE_DATE,
					APPLIED_AMOUNT,
					null
				),
				false
			),
			Arguments.of(
				"exact payment for one period",
				LiabilityPaymentResult.paid(
					ONE_PERIOD,
					OUTSTANDING_BALANCE,
					NEXT_DUE_DATE,
					CONFIGURED_PAYMENT_AMOUNT,
					CONFIGURED_PAYMENT_AMOUNT
				),
				false
			),
			Arguments.of(
				"exact total over several periods",
				LiabilityPaymentResult.paid(
					THREE_PERIODS,
					OUTSTANDING_BALANCE,
					NEXT_DUE_DATE,
					CONFIGURED_PAYMENT_AMOUNT.multiply(BigDecimal.valueOf(THREE_PERIODS)),
					CONFIGURED_PAYMENT_AMOUNT
				),
				false
			),
			Arguments.of(
				"exact total at a different scale",
				LiabilityPaymentResult.paid(
					FIVE_PERIODS,
					OUTSTANDING_BALANCE,
					NEXT_DUE_DATE,
					new BigDecimal("1000.000"),
					new BigDecimal("200.00")
				),
				false
			),
			Arguments.of(
				"clamped closing period",
				LiabilityPaymentResult.paid(
					ONE_PERIOD,
					OUTSTANDING_BALANCE,
					NEXT_DUE_DATE,
					APPLIED_AMOUNT,
					CONFIGURED_PAYMENT_AMOUNT
				),
				true
			),
			Arguments.of(
				"clamped last of several periods",
				LiabilityPaymentResult.paid(
					THREE_PERIODS,
					OUTSTANDING_BALANCE,
					NEXT_DUE_DATE,
					new BigDecimal("1000"),
					new BigDecimal("400")
				),
				true
			)
		);
	}

	@Test
	void paid_withoutAmounts_reportsTheScheduleAndLeavesTheAmountsUnknown() {
		// WHEN
		LiabilityPaymentResult expectedResult = LiabilityPaymentResult.paid(
			ONE_PERIOD,
			OUTSTANDING_BALANCE,
			NEXT_DUE_DATE
		);

		// THEN
		assertThat(expectedResult.status()).isEqualTo(LiabilityPaymentStatus.PAID);
		assertThat(expectedResult.paymentsApplied()).isEqualTo(ONE_PERIOD);
		assertThat(expectedResult.outstandingBalance()).isEqualByComparingTo(OUTSTANDING_BALANCE);
		assertThat(expectedResult.nextDueDate()).isEqualTo(NEXT_DUE_DATE);
		assertThat(expectedResult.appliedAmount()).isNull();
		assertThat(expectedResult.configuredPaymentAmount()).isNull();
		assertThat(expectedResult.reason()).isNull();
		assertThat(expectedResult.isClamped()).isFalse();
	}

	@Test
	void paid_withAmounts_reportsAppliedAndConfiguredAmounts() {
		// WHEN
		LiabilityPaymentResult expectedResult = LiabilityPaymentResult.paid(
			ONE_PERIOD,
			OUTSTANDING_BALANCE,
			NEXT_DUE_DATE,
			APPLIED_AMOUNT,
			CONFIGURED_PAYMENT_AMOUNT
		);

		// THEN
		assertThat(expectedResult.status()).isEqualTo(LiabilityPaymentStatus.PAID);
		assertThat(expectedResult.paymentsApplied()).isEqualTo(ONE_PERIOD);
		assertThat(expectedResult.appliedAmount()).isEqualByComparingTo(APPLIED_AMOUNT);
		assertThat(expectedResult.configuredPaymentAmount()).isEqualByComparingTo(
			CONFIGURED_PAYMENT_AMOUNT
		);
		assertThat(expectedResult.outstandingBalance()).isEqualByComparingTo(OUTSTANDING_BALANCE);
		assertThat(expectedResult.nextDueDate()).isEqualTo(NEXT_DUE_DATE);
		assertThat(expectedResult.reason()).isNull();
	}

	@Test
	void skipped_reportsTheReasonAndNoAmounts() {
		// WHEN
		LiabilityPaymentResult expectedResult = LiabilityPaymentResult.skipped(SKIP_REASON);

		// THEN
		assertThat(expectedResult.status()).isEqualTo(LiabilityPaymentStatus.SKIPPED);
		assertThat(expectedResult.paymentsApplied()).isZero();
		assertThat(expectedResult.reason()).isEqualTo(SKIP_REASON);
		assertThat(expectedResult.appliedAmount()).isNull();
		assertThat(expectedResult.configuredPaymentAmount()).isNull();
		assertThat(expectedResult.outstandingBalance()).isNull();
		assertThat(expectedResult.nextDueDate()).isNull();
		assertThat(expectedResult.isClamped()).isFalse();
	}

	@Test
	void notDue_reportsOnlyTheNextDueDate() {
		// WHEN
		LiabilityPaymentResult expectedResult = LiabilityPaymentResult.notDue(NEXT_DUE_DATE);

		// THEN
		assertThat(expectedResult.status()).isEqualTo(LiabilityPaymentStatus.NOT_DUE);
		assertThat(expectedResult.paymentsApplied()).isZero();
		assertThat(expectedResult.nextDueDate()).isEqualTo(NEXT_DUE_DATE);
		assertThat(expectedResult.appliedAmount()).isNull();
		assertThat(expectedResult.configuredPaymentAmount()).isNull();
		assertThat(expectedResult.outstandingBalance()).isNull();
		assertThat(expectedResult.reason()).isNull();
		assertThat(expectedResult.isClamped()).isFalse();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("clampExpectations")
	void isClamped_reportsWhetherTheScheduleAskedForMoreThanTheBalance(
		String stubbedCase,
		LiabilityPaymentResult mockResult,
		boolean expectedClamped
	) {
		// WHEN
		boolean expectedResult = mockResult.isClamped();

		// THEN
		assertThat(expectedResult).isEqualTo(expectedClamped);
	}
}
