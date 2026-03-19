package com.backend.aurum.infrastructure.exchange;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Service
public class ExchangeRateService {

	private final RestClient restClient = RestClient.create("https://api.frankfurter.app");

	public BigDecimal getRate(String from, String to, LocalDate date) {
		if (from.equals(to))
			return BigDecimal.ONE;
		try {
			Map<String, Object> response = restClient.get()
					.uri("/{date}?from={from}&to={to}", date, from, to)
					.retrieve()
					.body(new ParameterizedTypeReference<>() {
					});
			@SuppressWarnings("unchecked")
			Map<String, Number> rates = (Map<String, Number>) response.get("rates");
			return BigDecimal.valueOf(rates.get(to).doubleValue());
		} catch (Exception e) {
			return BigDecimal.ONE;
		}
	}
}
