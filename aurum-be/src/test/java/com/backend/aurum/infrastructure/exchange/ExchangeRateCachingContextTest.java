package com.backend.aurum.infrastructure.exchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

import com.backend.aurum.domain.shared.exception.ExchangeRateUnavailableException;
import com.backend.aurum.domain.shared.port.ExchangeRateProvider;
import com.backend.aurum.infrastructure.config.CacheConfig;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@SpringJUnitConfig(ExchangeRateCachingContextTest.Config.class)
class ExchangeRateCachingContextTest {

	private static final String CACHE_NAME = "exchangeRates";
	private static final String FROM_CURRENCY = "GBP";
	private static final String TO_CURRENCY = "EUR";
	private static final String OTHER_CURRENCY = "CHF";
	private static final LocalDate FIRST_DATE = LocalDate.of(2026, 1, 15);
	private static final LocalDate SECOND_DATE = LocalDate.of(2026, 6, 15);
	private static final String FIRST_PAIR_SERIES_KEY = "rates|GBP|EUR|2026-01-15,2026-06-15";
	private static final String FIRST_PAIR_SINGLE_KEY = "rate|GBP|EUR|2026-06-15";
	private static final String SINGLE_DATE_SERIES_KEY = "rates|GBP|EUR|2026-06-15";
	private static final String OTHER_PAIR_SERIES_KEY = "rates|CHF|EUR|2026-01-15,2026-06-15";
	private static final String MISSING_DATES_KEY = "rates|GBP|EUR|?";
	private static final BigDecimal EARLY_RATE = new BigDecimal("0.8500");
	private static final BigDecimal LATE_RATE = new BigDecimal("0.9000");
	private static final Map<String, Object> STUBBED_RESPONSE = Map.of(
		"rates",
		Map.of(
			"2026-01-10",
			Map.of(TO_CURRENCY, EARLY_RATE),
			"2026-06-10",
			Map.of(TO_CURRENCY, LATE_RATE)
		)
	);

	@Autowired
	private ExchangeRateProvider testSubject;

	@Autowired
	private Config config;

	@Autowired
	private CacheManager cacheManager;

	@BeforeEach
	void setUp() {
		reset(
			config.mockRestClient,
			config.mockUriSpec,
			config.mockHeadersSpec,
			config.mockResponseSpec
		);
		doReturn(config.mockUriSpec).when(config.mockRestClient).get();
		doReturn(config.mockHeadersSpec).when(config.mockUriSpec).uri(anyString(), any(Object[].class));
		doReturn(config.mockResponseSpec).when(config.mockHeadersSpec).retrieve();
		config.providerCalls.set(0);
		exchangeRatesCache().clear();
	}

	@Test
	void getRates_callsProviderOnce_whenTheSameDateSetIsRequestedInAnotherOrder() {
		// GIVEN
		stubProviderResponse();
		Collection<LocalDate> mockRepeatedDates = List.of(SECOND_DATE, FIRST_DATE, SECOND_DATE);

		// WHEN
		Map<LocalDate, BigDecimal> expectedRates = testSubject.getRates(
			FROM_CURRENCY,
			TO_CURRENCY,
			List.of(FIRST_DATE, SECOND_DATE)
		);
		Map<LocalDate, BigDecimal> expectedRepeatedRates = testSubject.getRates(
			FROM_CURRENCY,
			TO_CURRENCY,
			mockRepeatedDates
		);

		// THEN
		assertThat(expectedRepeatedRates).isEqualTo(expectedRates);
		assertThat(config.providerCalls).hasValue(1);
		assertThat(exchangeRatesCache().get(FIRST_PAIR_SERIES_KEY)).isNotNull();
	}

	@Test
	void getRates_callsProviderAgain_whenTheCurrencyPairIsNew() {
		// GIVEN
		stubProviderResponse();
		Collection<LocalDate> mockDates = List.of(FIRST_DATE, SECOND_DATE);

		// WHEN
		testSubject.getRates(FROM_CURRENCY, TO_CURRENCY, mockDates);
		testSubject.getRates(OTHER_CURRENCY, TO_CURRENCY, mockDates);

		// THEN
		assertThat(config.providerCalls).hasValue(2);
		assertThat(exchangeRatesCache().get(FIRST_PAIR_SERIES_KEY)).isNotNull();
		assertThat(exchangeRatesCache().get(OTHER_PAIR_SERIES_KEY)).isNotNull();
	}

	@Test
	void getRates_throwsUnavailableAndCachesNothing_whenProviderFails() {
		// GIVEN
		doThrow(new ResourceAccessException("connection refused"))
			.when(config.mockResponseSpec)
			.body(any(ParameterizedTypeReference.class));

		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.getRates(FROM_CURRENCY, TO_CURRENCY, List.of(FIRST_DATE))
		).isInstanceOf(ExchangeRateUnavailableException.class);
		assertThat(exchangeRatesCache().get("rates|GBP|EUR|2026-01-15")).isNull();
		assertThat(config.providerCalls).hasValue(0);

		// GIVEN
		stubProviderResponse();

		// WHEN
		Map<LocalDate, BigDecimal> expectedRates = testSubject.getRates(
			FROM_CURRENCY,
			TO_CURRENCY,
			List.of(FIRST_DATE)
		);

		// THEN
		assertThat(expectedRates).hasSize(1);
		assertThat(config.providerCalls).hasValue(1);
	}

	@Test
	void getRateAndGetRates_shareTheCacheWithoutColliding_whenPairAndDateMatch() {
		// GIVEN
		stubProviderResponse();

		// WHEN
		BigDecimal expectedRate = testSubject.getRate(FROM_CURRENCY, TO_CURRENCY, SECOND_DATE);
		Map<LocalDate, BigDecimal> expectedRates = testSubject.getRates(
			FROM_CURRENCY,
			TO_CURRENCY,
			List.of(SECOND_DATE)
		);

		// THEN
		assertThat(expectedRate).isEqualByComparingTo(LATE_RATE);
		assertThat(expectedRates.get(SECOND_DATE)).isEqualByComparingTo(LATE_RATE);
		assertThat(config.providerCalls).hasValue(2);
		assertThat(exchangeRatesCache().get(FIRST_PAIR_SINGLE_KEY).get()).isInstanceOf(
			BigDecimal.class
		);
		assertThat(exchangeRatesCache().get(SINGLE_DATE_SERIES_KEY).get()).isInstanceOf(Map.class);
	}

	@Test
	void getRates_buildsAKeyInsteadOfFailing_whenDatesAreNull() {
		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.getRates(FROM_CURRENCY, TO_CURRENCY, null)).isInstanceOf(
			ExchangeRateUnavailableException.class
		);
		assertThat(config.providerCalls).hasValue(0);
		assertThat(exchangeRatesCache().get(MISSING_DATES_KEY)).isNull();
	}

	private void stubProviderResponse() {
		doAnswer(invocation -> {
			config.providerCalls.incrementAndGet();
			return STUBBED_RESPONSE;
		})
			.when(config.mockResponseSpec)
			.body(any(ParameterizedTypeReference.class));
	}

	private Cache exchangeRatesCache() {
		return cacheManager.getCache(CACHE_NAME);
	}

	@Configuration
	@Import({ CacheConfig.class, ExchangeRateCacheKey.class, ExchangeRateService.class })
	static class Config {

		final RestClient mockRestClient = mock();
		final RestClient.RequestHeadersUriSpec<?> mockUriSpec = mock();
		final RestClient.RequestHeadersSpec<?> mockHeadersSpec = mock();
		final RestClient.ResponseSpec mockResponseSpec = mock();
		final AtomicInteger providerCalls = new AtomicInteger();

		@Bean
		RestClient mockRestClient() {
			return mockRestClient;
		}
	}
}
