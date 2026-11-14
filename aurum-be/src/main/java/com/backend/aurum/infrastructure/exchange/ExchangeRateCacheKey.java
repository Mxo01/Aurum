package com.backend.aurum.infrastructure.exchange;

import java.time.LocalDate;
import java.util.Collection;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Builds the keys of {@link ExchangeRateService}. It is a bean so the SpEL in
 * {@code @Cacheable} stays declarative: a key is evaluated before the method
 * body, so it must never throw (a null or empty collection is a key, not an
 * error) and must not depend on the caller's collection order or type.
 */
@Component("exchangeRateCacheKey")
public class ExchangeRateCacheKey {

	private static final String MISSING_VALUE = "?";
	private static final String FIELD_SEPARATOR = "|";
	private static final String DATE_SEPARATOR = ",";
	private static final String SINGLE_PREFIX = "rate";
	private static final String SERIES_PREFIX = "rates";

	/**
	 * The single-date key is prefixed differently from the series key on purpose:
	 * both methods share one cache, and their cached values have different types.
	 */
	public String single(String from, String to, LocalDate date) {
		return (
			SINGLE_PREFIX +
			FIELD_SEPARATOR +
			normalize(from) +
			FIELD_SEPARATOR +
			normalize(to) +
			FIELD_SEPARATOR +
			normalize(date)
		);
	}

	public String series(String from, String to, Collection<?> dates) {
		return (
			SERIES_PREFIX +
			FIELD_SEPARATOR +
			normalize(from) +
			FIELD_SEPARATOR +
			normalize(to) +
			FIELD_SEPARATOR +
			joinDates(dates)
		);
	}

	private String joinDates(Collection<?> dates) {
		if (dates == null) return MISSING_VALUE;
		return dates
			.stream()
			.filter(LocalDate.class::isInstance)
			.map(LocalDate.class::cast)
			.collect(Collectors.toCollection(TreeSet::new))
			.stream()
			.map(LocalDate::toString)
			.collect(Collectors.joining(DATE_SEPARATOR));
	}

	private String normalize(Object value) {
		return value == null ? MISSING_VALUE : value.toString();
	}
}
