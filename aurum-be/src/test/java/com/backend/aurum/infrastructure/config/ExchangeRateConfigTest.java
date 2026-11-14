package com.backend.aurum.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.client.RestClient;

class ExchangeRateConfigTest {

	private static final String EXCHANGE_RATE_CLIENT_BEAN_NAME = "exchangeRateRestClient";

	@Test
	void exchangeRateRestClient_registersRestClientBean_whenContextStarts() {
		// GIVEN
		try (
			AnnotationConfigApplicationContext testSubject = new AnnotationConfigApplicationContext()
		) {
			testSubject.register(ExchangeRateConfig.class);

			// WHEN
			testSubject.refresh();

			// THEN
			assertThat(testSubject.getBeanNamesForType(RestClient.class)).containsExactly(
				EXCHANGE_RATE_CLIENT_BEAN_NAME
			);
			assertThat(testSubject.getBean(EXCHANGE_RATE_CLIENT_BEAN_NAME)).isInstanceOf(
				RestClient.class
			);
		}
	}
}
