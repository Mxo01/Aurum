package com.backend.aurum.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpClient;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

class ExchangeRateConfigTest {

	private static final String EXCHANGE_RATE_CLIENT_BEAN_NAME = "exchangeRateRestClient";

	private static final String REQUEST_FACTORY_FIELD = "clientRequestFactory";

	private static final String HTTP_CLIENT_FIELD = "httpClient";

	private static final String READ_TIMEOUT_FIELD = "readTimeout";

	private static final Duration EXPECTED_CONNECT_TIMEOUT = Duration.ofSeconds(2);

	private static final Duration EXPECTED_READ_TIMEOUT = Duration.ofSeconds(5);

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

	@Test
	void exchangeRateRestClient_setsConnectTimeoutOnHttpClient() {
		// GIVEN
		ExchangeRateConfig testSubject = new ExchangeRateConfig();
		JdkClientHttpRequestFactory expectedRequestFactory = requestFactoryOf(
			testSubject.exchangeRateRestClient()
		);

		// WHEN
		HttpClient expectedHttpClient = httpClientOf(expectedRequestFactory);

		// THEN
		assertThat(expectedHttpClient.connectTimeout()).contains(EXPECTED_CONNECT_TIMEOUT);
	}

	@Test
	void exchangeRateRestClient_setsReadTimeoutOnRequestFactory() {
		// GIVEN
		ExchangeRateConfig testSubject = new ExchangeRateConfig();
		JdkClientHttpRequestFactory expectedRequestFactory = requestFactoryOf(
			testSubject.exchangeRateRestClient()
		);

		// WHEN
		Object expectedReadTimeout = ReflectionTestUtils.getField(
			expectedRequestFactory,
			READ_TIMEOUT_FIELD
		);

		// THEN
		assertThat(expectedReadTimeout).isEqualTo(EXPECTED_READ_TIMEOUT);
	}

	private static JdkClientHttpRequestFactory requestFactoryOf(RestClient restClient) {
		Object requestFactory = ReflectionTestUtils.getField(restClient, REQUEST_FACTORY_FIELD);
		assertThat(requestFactory).isInstanceOf(JdkClientHttpRequestFactory.class);
		return (JdkClientHttpRequestFactory) requestFactory;
	}

	private static HttpClient httpClientOf(JdkClientHttpRequestFactory requestFactory) {
		Object httpClient = ReflectionTestUtils.getField(requestFactory, HTTP_CLIENT_FIELD);
		assertThat(httpClient).isInstanceOf(HttpClient.class);
		return (HttpClient) httpClient;
	}
}
