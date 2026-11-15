package com.backend.aurum.domain.shared.validation;

import com.backend.aurum.domain.shared.exception.ValidationException;
import java.math.BigDecimal;

/**
 * The bounds every stored amount shares. The money columns are {@code numeric(19,4)} and the
 * exchange-rate column is {@code numeric(19,6)}: a value with more decimals is rounded away by the
 * database while the response echoes the rounded value, and a value past 19 digits fails the insert
 * as a server error. Both are refused here, once, instead of drifting per domain.
 */
public final class AmountRules {

	/** numeric(19,4) keeps four decimals; a fifth is rounded instead of refused. */
	public static final int MAX_SCALE = 4;

	/** The 19 total digits of a money column, minus the 4 decimals it keeps. */
	public static final int MAX_INTEGER_DIGITS = 15;

	/** The precision of every money column: {@code numeric(19,4)} and {@code numeric(19,6)}. */
	public static final int MAX_PRECISION = MAX_INTEGER_DIGITS + MAX_SCALE;

	/** snapshots.exchange_rate_to_base is {@code numeric(19,6)}. */
	public static final int MAX_EXCHANGE_RATE_SCALE = 6;

	/** A rate past this multiplies any amount beyond the range a portfolio can mean. */
	public static final BigDecimal MAX_EXCHANGE_RATE = new BigDecimal("1000000");

	private AmountRules() {}

	/**
	 * A missing amount is a required-field violation with its own sentence, so a caller rejects null
	 * before asking the rules about the value.
	 */
	public static void requireNonNegative(BigDecimal amount, String field, String label) {
		if (amount.signum() < 0) {
			throw new ValidationException(field, label + " cannot be negative");
		}
	}

	public static void requirePositive(BigDecimal amount, String field, String label) {
		if (amount.signum() <= 0) {
			throw new ValidationException(field, label + " must be greater than zero");
		}
	}

	/** Scale and magnitude of a money field: {@code numeric(19,4)}. */
	public static void requireAmount(BigDecimal amount, String field, String label) {
		requireFitsColumn(amount, field, label, MAX_SCALE);
	}

	/** Scale and magnitude of the exchange-rate field: {@code numeric(19,6)}, within a sane band. */
	public static void requireExchangeRate(BigDecimal rate, String field, String label) {
		requireFitsColumn(rate, field, label, MAX_EXCHANGE_RATE_SCALE);
		if (rate.compareTo(MAX_EXCHANGE_RATE) > 0) {
			throw new ValidationException(field, label + " is too large");
		}
	}

	private static void requireFitsColumn(
		BigDecimal amount,
		String field,
		String label,
		int maxScale
	) {
		// Compared on the stripped value so 1.500000 is the rate 1.5 and not a six-decimal one.
		if (amount.stripTrailingZeros().scale() > maxScale) {
			throw new ValidationException(
				field,
				label + " cannot have more than " + maxScale + " decimal places"
			);
		}
		// Digits the column would have to keep: past the precision, the insert overflows.
		if (integerDigits(amount) > MAX_PRECISION - maxScale) {
			throw new ValidationException(field, label + " is too large");
		}
	}

	private static int integerDigits(BigDecimal amount) {
		return amount.precision() - amount.scale();
	}
}
