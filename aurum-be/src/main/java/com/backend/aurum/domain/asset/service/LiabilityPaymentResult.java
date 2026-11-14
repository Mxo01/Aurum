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
 * @param reason why the asset was skipped, null for every other status
 */
public record LiabilityPaymentResult(
	LiabilityPaymentStatus status,
	int paymentsApplied,
	BigDecimal outstandingBalance,
	LocalDate nextDueDate,
	String reason
) {
	public static LiabilityPaymentResult paid(
		int paymentsApplied,
		BigDecimal outstandingBalance,
		LocalDate nextDueDate
	) {
		return new LiabilityPaymentResult(
			LiabilityPaymentStatus.PAID,
			paymentsApplied,
			outstandingBalance,
			nextDueDate,
			null
		);
	}

	public static LiabilityPaymentResult skipped(String reason) {
		return new LiabilityPaymentResult(LiabilityPaymentStatus.SKIPPED, 0, null, null, reason);
	}

	public static LiabilityPaymentResult notDue(LocalDate nextDueDate) {
		return new LiabilityPaymentResult(LiabilityPaymentStatus.NOT_DUE, 0, null, nextDueDate, null);
	}
}
