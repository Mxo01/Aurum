package com.backend.aurum.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ExchangeRateConfig {

	private static final String BASE_URL = "https://api.frankfurter.dev/v1";

	@Bean
	public RestClient exchangeRateRestClient() {
		return RestClient.create(BASE_URL);
	}
}
