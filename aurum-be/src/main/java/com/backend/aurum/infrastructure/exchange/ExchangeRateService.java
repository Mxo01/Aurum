package com.backend.aurum.infrastructure.exchange;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class ExchangeRateService {

	private final RestClient restClient = RestClient.create("https://api.frankfurter.app");

	@Cacheable(value = "exchangeRates", key = "#from + '-' + #to + '-' + #date")
	public BigDecimal getRate(String from, String to, LocalDate date) {
		if (from.equals(to)) return BigDecimal.ONE;
		log.debug(
			"ExchangeRateService#getRate - Fetching exchange rate from={} to={} on date={}",
			from,
			to,
			date
		);
		try {
			Map<String, Object> response = restClient
				.get()
				.uri("/{date}?from={from}&to={to}", date, from, to)
				.retrieve()
				.body(new ParameterizedTypeReference<>() {});
			if (response == null) {
				throw new RuntimeException(
					"Empty response from exchange rate API for " + from + "/" + to + " on " + date
				);
			}
			@SuppressWarnings("unchecked")
			Map<String, Number> rates = (Map<String, Number>) response.get("rates");
			if (rates == null || rates.get(to) == null) {
				throw new RuntimeException(
					"Exchange rate not available for " + from + "/" + to + " on " + date
				);
			}
			BigDecimal rate = BigDecimal.valueOf(rates.get(to).doubleValue());
			if (rate.compareTo(BigDecimal.ZERO) <= 0) {
				throw new RuntimeException(
					"Invalid exchange rate (<=0) for " + from + "/" + to + " on " + date + ": " + rate
				);
			}
			log.debug("ExchangeRateService#getRate - Rate obtained: 1 {} = {} {}", from, rate, to);
			return rate;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(
				"Failed to fetch exchange rate for " +
					from +
					"/" +
					to +
					" on " +
					date +
					": " +
					e.getMessage(),
				e
			);
		}
	}
}
