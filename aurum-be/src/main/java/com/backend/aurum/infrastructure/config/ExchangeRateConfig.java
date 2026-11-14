package com.backend.aurum.infrastructure.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class ExchangeRateConfig {

	private static final String BASE_URL = "https://api.frankfurter.dev/v1";

	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);

	private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

	@Bean
	public RestClient exchangeRateRestClient() {
		HttpClient httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(READ_TIMEOUT);
		return RestClient.builder().baseUrl(BASE_URL).requestFactory(requestFactory).build();
	}
}
