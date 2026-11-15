package com.backend.aurum.domain.asset.model;

import com.backend.aurum.domain.shared.exception.ValidationException;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * The liability block of an asset as one rule object: the three columns are meaningless apart, so
 * the whole configuration is validated and replaced as a unit. MANUAL keeps a legacy frequency
 * (production holds MANUAL + MONTHLY with a zero amount) but rejects any positive amount.
 */
public record LiabilityConfiguration(
	LiabilityType type,
	PaymentFrequency frequency,
	BigDecimal amount
) {
	private static final int MAX_AMOUNT_SCALE = 4;

	public static LiabilityConfiguration of(Asset asset) {
		return new LiabilityConfiguration(
			asset.getLiabilityType(),
			asset.getPaymentFrequency(),
			asset.getPaymentAmount()
		);
	}

	public boolean isAutomatic() {
		return type == LiabilityType.AUTOMATIC;
	}

	public void validate() {
		if (type == null) {
			if (frequency != null || amount != null) {
				throw new ValidationException(
					"liabilityType is required when paymentFrequency or paymentAmount is provided"
				);
			}
			return;
		}

		if (amount != null && amount.signum() < 0) {
			throw new ValidationException("paymentAmount cannot be negative");
		}

		if (amount != null && amount.stripTrailingZeros().scale() > MAX_AMOUNT_SCALE) {
			throw new ValidationException("paymentAmount supports at most 4 decimals");
		}

		if (type == LiabilityType.AUTOMATIC) {
			validateAutomatic();
		}

		if (type == LiabilityType.MANUAL && amount != null && amount.signum() > 0) {
			throw new ValidationException("A MANUAL liability cannot carry a positive paymentAmount");
		}
	}

	/**
	 * Compared by value, not by scale: 200 and 200.00 are the same payment and must not re-derive a
	 * schedule that is still valid.
	 */
	@Override
	public boolean equals(Object other) {
		if (this == other) return true;
		if (!(other instanceof LiabilityConfiguration configuration)) return false;
		return (
			type == configuration.type &&
			frequency == configuration.frequency &&
			amountEquals(amount, configuration.amount)
		);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, frequency, amount == null ? null : amount.stripTrailingZeros());
	}

	private void validateAutomatic() {
		if (frequency == null) {
			throw new ValidationException("paymentFrequency is required for an AUTOMATIC liability");
		}

		if (amount == null || amount.signum() <= 0) {
			throw new ValidationException(
				"paymentAmount must be greater than 0 for an AUTOMATIC liability"
			);
		}
	}

	private boolean amountEquals(BigDecimal current, BigDecimal updated) {
		if (current == null || updated == null) {
			return current == updated;
		}
		return current.compareTo(updated) == 0;
	}
}
