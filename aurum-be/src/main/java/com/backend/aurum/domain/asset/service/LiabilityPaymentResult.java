package com.backend.aurum.domain.asset.service;

import com.backend.aurum.domain.asset.model.LiabilityPaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * What one automatic liability payment attempt did, in the vocabulary the run log and the
 * verification need.
 *
 * <p>{@code outstandingBalance} and {@code nextDueDate} are null only when nothing is known about
 * the balance: a skipped asset was never paid. {@code nextDueDate} is also null after the asset
 * settled, so the caller reads a zero balance as "settled" rather than "due today".
 *
 * <p>{@code appliedAmount} is what the run actually took off the balance, {@code
 * configuredPaymentAmount} is what the asset pays per period. The two differ when the configured
 * payment no longer fits the remaining balance: the closing period is clamped, and the shortfall is
 * reported here instead of vanishing.
 *
 * @param reason why the asset was skipped, null for every other status
 * @param appliedAmount what the run took off the balance, null when nothing was applied
 * @param configuredPaymentAmount the per-period payment the asset is configured with, null when
 *     nothing was applied
 */
public record LiabilityPaymentResult(
	LiabilityPaymentStatus status,
	int paymentsApplied,
	BigDecimal appliedAmount,
	BigDecimal configuredPaymentAmount,
	BigDecimal outstandingBalance,
	LocalDate nextDueDate,
	String reason
) {
	/**
	 * A paid result without the amounts, for callers that do not report them.
	 */
	public static LiabilityPaymentResult paid(
		int paymentsApplied,
		BigDecimal outstandingBalance,
		LocalDate nextDueDate
	) {
		return paid(paymentsApplied, outstandingBalance, nextDueDate, null, null);
	}

	public static LiabilityPaymentResult paid(
		int paymentsApplied,
		BigDecimal outstandingBalance,
		LocalDate nextDueDate,
		BigDecimal appliedAmount,
		BigDecimal configuredPaymentAmount
	) {
		return new LiabilityPaymentResult(
			LiabilityPaymentStatus.PAID,
			paymentsApplied,
			appliedAmount,
			configuredPaymentAmount,
			outstandingBalance,
			nextDueDate,
			null
		);
	}

	public static LiabilityPaymentResult skipped(String reason) {
		return new LiabilityPaymentResult(
			LiabilityPaymentStatus.SKIPPED,
			0,
			null,
			null,
			null,
			null,
			reason
		);
	}

	public static LiabilityPaymentResult notDue(LocalDate nextDueDate) {
		return new LiabilityPaymentResult(
			LiabilityPaymentStatus.NOT_DUE,
			0,
			null,
			null,
			null,
			nextDueDate,
			null
		);
	}

	/**
	 * True when the schedule asked for more than the balance could pay, so the run is not allowed
	 * to report the configured amount as if it had been applied.
	 */
	public boolean isClamped() {
		if (appliedAmount == null || configuredPaymentAmount == null) {
			return false;
		}
		BigDecimal configuredTotal = configuredPaymentAmount.multiply(
			BigDecimal.valueOf(paymentsApplied)
		);
		return appliedAmount.compareTo(configuredTotal) < 0;
	}
}
