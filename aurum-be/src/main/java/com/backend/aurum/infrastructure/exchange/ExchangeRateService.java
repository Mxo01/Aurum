package com.backend.aurum.infrastructure.exchange;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class ExchangeRateService {

	private final RestClient restClient = RestClient.create("https://api.frankfurter.app");

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
				log.warn(
					"ExchangeRateService#getRate - Empty response from API for {}/{} on {}, falling back to 1:1",
					from,
					to,
					date
				);
				return BigDecimal.ONE;
			}
			@SuppressWarnings("unchecked")
			Map<String, Number> rates = (Map<String, Number>) response.get("rates");
			BigDecimal rate = BigDecimal.valueOf(rates.get(to).doubleValue());
			log.debug("ExchangeRateService#getRate - Rate obtained: 1 {} = {} {}", from, rate, to);
			return rate;
		} catch (Exception e) {
			log.warn(
				"ExchangeRateService#getRate - Failed to fetch exchange rate for {}/{} on {}, falling back to 1:1. Error: {}",
				from,
				to,
				date,
				e.getMessage()
			);
			return BigDecimal.ONE;
		}
	}
}
