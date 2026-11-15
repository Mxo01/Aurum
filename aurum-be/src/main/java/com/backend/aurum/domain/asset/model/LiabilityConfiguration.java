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
					"liabilityType",
					"Liability type is required when a payment frequency or amount is set"
				);
			}
			return;
		}

		if (amount != null && amount.signum() < 0) {
			throw new ValidationException("paymentAmount", "Payment amount cannot be negative");
		}

		if (amount != null && amount.stripTrailingZeros().scale() > MAX_AMOUNT_SCALE) {
			throw new ValidationException(
				"paymentAmount",
				"Payment amount cannot have more than 4 decimal places"
			);
		}

		if (type == LiabilityType.AUTOMATIC) {
			validateAutomatic();
		}

		if (type == LiabilityType.MANUAL && amount != null && amount.signum() > 0) {
			throw new ValidationException(
				"paymentAmount",
				"A manual liability cannot have a payment amount"
			);
		}
	}

	/**
	 * The dashboard classifies an asset by its category's type, so a liability filed under an ASSET
	 * category is summed as wealth and the debt disappears. The category lives on the parent entity,
	 * which only the service layer holds inside its transaction, so its type arrives as an argument.
	 */
	public void validateForCategory(AssetType categoryType) {
		if (type == null || categoryType == null || categoryType == AssetType.LIABILITY) {
			return;
		}
		throw new ValidationException(
			"liabilityType",
			"Liability type is only allowed on a liability category"
		);
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
			throw new ValidationException(
				"paymentFrequency",
				"Payment frequency is required for an automatic liability"
			);
		}

		if (amount == null || amount.signum() <= 0) {
			throw new ValidationException(
				"paymentAmount",
				"Payment amount must be greater than zero for an automatic liability"
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
