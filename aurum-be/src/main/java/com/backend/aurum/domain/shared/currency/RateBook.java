package com.backend.aurum.domain.shared.currency;

import com.backend.aurum.domain.shared.exception.ExchangeRateUnavailableException;
import com.backend.aurum.domain.user.enums.Currency;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;
import java.util.NavigableMap;

/**
 * Immutable set of rates resolved once per request. Converting inside a loop
 * never touches the provider: a missing rate fails the request instead of
 * returning a number in the wrong currency.
 */
public final class RateBook {

	private static final int AMOUNT_SCALE = 4;

	private final Currency target;
	private final Map<Currency, NavigableMap<LocalDate, BigDecimal>> ratesBySource;

	RateBook(Currency target, Map<Currency, NavigableMap<LocalDate, BigDecimal>> ratesBySource) {
		this.target = target;
		this.ratesBySource = Map.copyOf(ratesBySource);
	}

	public Currency target() {
		return target;
	}

	public BigDecimal rate(Currency from, LocalDate date) {
		if (from == target) return BigDecimal.ONE;
		NavigableMap<LocalDate, BigDecimal> series = ratesBySource.get(from);
		Map.Entry<LocalDate, BigDecimal> entry = series == null ? null : series.floorEntry(date);
		if (entry == null) throw unavailable(from, date);
		return entry.getValue();
	}

	public BigDecimal convert(BigDecimal amount, Currency from, LocalDate date) {
		if (amount == null) return BigDecimal.ZERO;
		if (from == target) return amount.setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
		return amount.multiply(rate(from, date)).setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
	}

	private ExchangeRateUnavailableException unavailable(Currency from, LocalDate date) {
		return new ExchangeRateUnavailableException(
			"No exchange rate available from " +
				from.getValue() +
				" to " +
				target.getValue() +
				" on " +
				date
		);
	}
}
