package com.backend.aurum.domain.shared.port;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;

/**
 * Exchange rates are a business need, not a transport detail: the domain asks
 * for a rate on a date and does not care whether it comes from an HTTP API, a
 * cache, or a fixed table. The implementation lives in infrastructure.
 */
public interface ExchangeRateProvider {
	BigDecimal getRate(String from, String to, LocalDate date);

	/**
	 * Returns the rate in effect on each requested date — the last trading day at
	 * or before it — in a single provider call covering the whole range.
	 */
	Map<LocalDate, BigDecimal> getRates(String from, String to, Collection<LocalDate> dates);
}
