package com.backend.aurum.infrastructure.exchange;

import com.backend.aurum.domain.shared.exception.ExchangeRateUnavailableException;
import com.backend.aurum.domain.shared.port.ExchangeRateProvider;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService implements ExchangeRateProvider {

	private static final String RATES_FIELD = "rates";
	private static final String DATE_RANGE_SEPARATOR = "..";
	private static final int SEARCH_BACK_DAYS = 7;

	private final RestClient restClient;

	@Cacheable(value = "exchangeRates", key = "@exchangeRateCacheKey.single(#from, #to, #date)")
	@Override
	public BigDecimal getRate(String from, String to, LocalDate date) {
		if (Objects.equals(from, to)) return BigDecimal.ONE;
		try {
			return effectiveRates(from, to, Collections.singleton(date)).get(date);
		} catch (RuntimeException e) {
			throw failure(from, to, date, e);
		}
	}

	@Cacheable(value = "exchangeRates", key = "@exchangeRateCacheKey.series(#from, #to, #dates)")
	@Override
	public Map<LocalDate, BigDecimal> getRates(String from, String to, Collection<LocalDate> dates) {
		try {
			TreeSet<LocalDate> requested = dates
				.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toCollection(TreeSet::new));
			if (requested.isEmpty()) return Map.of();
			if (Objects.equals(from, to)) {
				Map<LocalDate, BigDecimal> identityRates = new LinkedHashMap<>();
				for (LocalDate date : requested) {
					identityRates.put(date, BigDecimal.ONE);
				}
				return identityRates;
			}
			return effectiveRates(from, to, requested);
		} catch (RuntimeException e) {
			throw failure(from, to, dates, e);
		}
	}

	private RuntimeException failure(String from, String to, Object dates, RuntimeException cause) {
		log.warn(
			"Exchange rate lookup failed for {}/{} on dates {}: {}",
			from,
			to,
			dates,
			cause.getMessage()
		);
		if (cause instanceof ExchangeRateUnavailableException unavailable) return unavailable;
		return new ExchangeRateUnavailableException(
			"Exchange rate lookup failed for " + from + "/" + to + " on " + dates,
			cause
		);
	}

	private Map<LocalDate, BigDecimal> effectiveRates(
		String from,
		String to,
		Collection<LocalDate> requested
	) {
		NavigableMap<LocalDate, BigDecimal> series = fetchSeries(
			from,
			to,
			Collections.min(requested),
			Collections.max(requested)
		);
		Map<LocalDate, BigDecimal> resolved = new LinkedHashMap<>();
		for (LocalDate date : new TreeSet<>(requested)) {
			Map.Entry<LocalDate, BigDecimal> entry = series.floorEntry(date);
			if (entry == null) {
				throw new ExchangeRateUnavailableException(
					"Exchange rate not available for " + from + "/" + to + " on " + date
				);
			}
			resolved.put(date, entry.getValue());
		}
		return resolved;
	}

	private NavigableMap<LocalDate, BigDecimal> fetchSeries(
		String from,
		String to,
		LocalDate first,
		LocalDate last
	) {
		String range = first.minusDays(SEARCH_BACK_DAYS) + DATE_RANGE_SEPARATOR + last;
		log.debug(
			"ExchangeRateService#fetchSeries - Fetching exchange rates from={} to={} range={}",
			from,
			to,
			range
		);
		Map<String, Object> response;
		try {
			response = restClient
				.get()
				.uri("/{range}?from={from}&to={to}", range, from, to)
				.retrieve()
				.body(new ParameterizedTypeReference<>() {});
		} catch (RestClientException e) {
			throw new ExchangeRateUnavailableException(
				"Failed to fetch exchange rates for " +
					from +
					"/" +
					to +
					" between " +
					first +
					" and " +
					last,
				e
			);
		}

		if (response == null || !(response.get(RATES_FIELD) instanceof Map<?, ?> quotesByDate)) {
			throw new ExchangeRateUnavailableException(
				"Empty exchange rate response for " + from + "/" + to + " between " + first + " and " + last
			);
		}

		NavigableMap<LocalDate, BigDecimal> series = new TreeMap<>();
		for (Map.Entry<?, ?> entry : quotesByDate.entrySet()) {
			if (!(entry.getValue() instanceof Map<?, ?> quotes)) continue;
			if (!(quotes.get(to) instanceof Number quote)) continue;
			BigDecimal rate = BigDecimal.valueOf(quote.doubleValue());
			if (rate.compareTo(BigDecimal.ZERO) <= 0) continue;
			series.put(LocalDate.parse(String.valueOf(entry.getKey())), rate);
		}

		if (series.isEmpty()) {
			throw new ExchangeRateUnavailableException(
				"Exchange rate not available for " + from + "/" + to + " between " + first + " and " + last
			);
		}

		log.debug(
			"ExchangeRateService#fetchSeries - Obtained {} rate(s) for {} -> {}",
			series.size(),
			from,
			to
		);
		return series;
	}
}
