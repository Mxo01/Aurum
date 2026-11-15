package com.backend.aurum.infrastructure.exchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.backend.aurum.domain.shared.currency.RateBook;
import com.backend.aurum.domain.shared.exception.ExchangeRateUnavailableException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

class ExchangeRateServiceTest {

	private static final String FROM_CURRENCY = "GBP";
	private static final String TO_CURRENCY = "EUR";
	private static final String OTHER_CURRENCY = "CHF";
	private static final LocalDate REQUESTED_DATE = LocalDate.of(2026, 11, 14);
	private static final LocalDate QUOTED_DATE = LocalDate.of(2026, 11, 10);
	private static final LocalDate LATER_QUOTED_DATE = LocalDate.of(2026, 11, 20);
	private static final LocalDate OLDEST_ACCEPTED_QUOTE_DATE = REQUESTED_DATE.minusDays(
		RateBook.MAX_RATE_AGE_DAYS
	);
	private static final LocalDate STALE_QUOTE_DATE = REQUESTED_DATE.minusDays(
		RateBook.MAX_RATE_AGE_DAYS + 1
	);
	private static final String STALE_QUOTE_MESSAGE = "is stale";
	private static final BigDecimal EARLIER_RATE = new BigDecimal("1.1900");
	private static final BigDecimal REQUESTED_RATE = new BigDecimal("1.1968");
	private static final String RATES_FIELD = "rates";
	private static final String UNRELATED_FIELD = "amount";

	private final RestClient mockRestClient = mock();
	private final RestClient.RequestHeadersUriSpec<?> mockUriSpec = mock();
	private final RestClient.RequestHeadersSpec<?> mockHeadersSpec = mock();
	private final RestClient.ResponseSpec mockResponseSpec = mock();

	private ExchangeRateService testSubject;

	@BeforeEach
	void setUp() {
		doReturn(mockUriSpec).when(mockRestClient).get();
		doReturn(mockHeadersSpec).when(mockUriSpec).uri(anyString(), any(Object[].class));
		doReturn(mockResponseSpec).when(mockHeadersSpec).retrieve();
		testSubject = new ExchangeRateService(mockRestClient);
	}

	@Test
	void getRate_returnsOneWithoutCallingProvider_whenCurrenciesAreEqual() {
		// WHEN
		BigDecimal expectedRate = testSubject.getRate(FROM_CURRENCY, FROM_CURRENCY, REQUESTED_DATE);

		// THEN
		assertThat(expectedRate).isEqualByComparingTo(BigDecimal.ONE);
		verifyNoInteractions(mockRestClient);
	}

	@Test
	void getRate_returnsRateOfPreviousTradingDay_whenRequestedDateHasNoQuote() {
		// GIVEN
		stubProviderResponse(stubbedResponse(QUOTED_DATE, EARLIER_RATE));

		// WHEN
		BigDecimal expectedRate = testSubject.getRate(FROM_CURRENCY, TO_CURRENCY, REQUESTED_DATE);

		// THEN
		assertThat(expectedRate).isEqualByComparingTo(EARLIER_RATE);
	}

	@Test
	void getRate_throwsUnavailable_whenProviderTransportFails() {
		// GIVEN
		doThrow(new ResourceAccessException("connection refused"))
			.when(mockResponseSpec)
			.body(any(ParameterizedTypeReference.class));

		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.getRate(FROM_CURRENCY, TO_CURRENCY, REQUESTED_DATE)
		).isInstanceOf(ExchangeRateUnavailableException.class);
	}

	@Test
	void getRate_throwsUnavailable_whenResponseBodyIsMissing() {
		// GIVEN
		stubProviderResponse(null);

		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.getRate(FROM_CURRENCY, TO_CURRENCY, REQUESTED_DATE)
		).isInstanceOf(ExchangeRateUnavailableException.class);
	}

	@Test
	void getRate_throwsUnavailable_whenResponseCarriesNoRatesField() {
		// GIVEN
		stubProviderResponse(Map.of(UNRELATED_FIELD, Map.of(QUOTED_DATE.toString(), 1)));

		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.getRate(FROM_CURRENCY, TO_CURRENCY, REQUESTED_DATE)
		).isInstanceOf(ExchangeRateUnavailableException.class);
	}

	@Test
	void getRate_throwsUnavailable_whenNoQuotePrecedesTheRequestedDate() {
		// GIVEN
		stubProviderResponse(stubbedResponse(LATER_QUOTED_DATE, EARLIER_RATE));

		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.getRate(FROM_CURRENCY, TO_CURRENCY, REQUESTED_DATE)
		).isInstanceOf(ExchangeRateUnavailableException.class);
	}

	@Test
	void getRate_throwsUnavailable_whenTheOnlyQuoteIsStale() {
		// GIVEN
		stubProviderResponse(stubbedResponse(STALE_QUOTE_DATE, EARLIER_RATE));

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.getRate(FROM_CURRENCY, TO_CURRENCY, REQUESTED_DATE))
			.isInstanceOf(ExchangeRateUnavailableException.class)
			.hasMessageContaining(STALE_QUOTE_MESSAGE);
	}

	@Test
	void getRate_rethrowsSameException_whenProviderAlreadyReportsUnavailable() {
		// GIVEN
		ExchangeRateUnavailableException mockFailure = new ExchangeRateUnavailableException(
			"provider unavailable"
		);
		doThrow(mockFailure).when(mockResponseSpec).body(any(ParameterizedTypeReference.class));

		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.getRate(FROM_CURRENCY, TO_CURRENCY, REQUESTED_DATE)
		).isSameAs(mockFailure);
	}

	@Test
	void getRates_returnsIdentityRatesWithoutCallingProvider_whenCurrenciesAreEqual() {
		// GIVEN
		Collection<LocalDate> mockDates = List.of(REQUESTED_DATE, QUOTED_DATE);

		// WHEN
		Map<LocalDate, BigDecimal> expectedRates = testSubject.getRates(
			FROM_CURRENCY,
			FROM_CURRENCY,
			mockDates
		);

		// THEN
		assertThat(expectedRates).containsOnlyKeys(QUOTED_DATE, REQUESTED_DATE);
		assertThat(expectedRates.values()).allSatisfy(rate ->
			assertThat(rate).isEqualByComparingTo(BigDecimal.ONE)
		);
		verifyNoInteractions(mockRestClient);
	}

	@Test
	void getRates_returnsEmptyMapWithoutCallingProvider_whenNoDateIsRequested() {
		// WHEN
		Map<LocalDate, BigDecimal> expectedRates = testSubject.getRates(
			FROM_CURRENCY,
			TO_CURRENCY,
			List.of()
		);

		// THEN
		assertThat(expectedRates).isEmpty();
		verifyNoInteractions(mockRestClient);
	}

	@Test
	void getRates_ignoresNullDates_whenCollectionContainsThem() {
		// GIVEN
		stubProviderResponse(stubbedResponse(QUOTED_DATE, EARLIER_RATE));
		Collection<LocalDate> mockDates = Arrays.asList(null, REQUESTED_DATE);

		// WHEN
		Map<LocalDate, BigDecimal> expectedRates = testSubject.getRates(
			FROM_CURRENCY,
			TO_CURRENCY,
			mockDates
		);

		// THEN
		assertThat(expectedRates).containsOnlyKeys(REQUESTED_DATE);
	}

	@Test
	void getRates_returnsFloorRateForEveryDate_whenQuotesCoverTheRange() {
		// GIVEN
		stubProviderResponse(
			Map.of(
				RATES_FIELD,
				Map.of(
					QUOTED_DATE.toString(),
					Map.of(TO_CURRENCY, EARLIER_RATE),
					REQUESTED_DATE.toString(),
					Map.of(TO_CURRENCY, REQUESTED_RATE)
				)
			)
		);

		// WHEN
		Map<LocalDate, BigDecimal> expectedRates = testSubject.getRates(
			FROM_CURRENCY,
			TO_CURRENCY,
			List.of(REQUESTED_DATE, QUOTED_DATE, REQUESTED_DATE)
		);

		// THEN
		assertThat(expectedRates).containsOnlyKeys(QUOTED_DATE, REQUESTED_DATE);
		assertThat(expectedRates.get(QUOTED_DATE)).isEqualByComparingTo(EARLIER_RATE);
		assertThat(expectedRates.get(REQUESTED_DATE)).isEqualByComparingTo(REQUESTED_RATE);
	}

	@Test
	void getRates_throwsUnavailable_whenDatesAreNull() {
		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.getRates(FROM_CURRENCY, TO_CURRENCY, null)).isInstanceOf(
			ExchangeRateUnavailableException.class
		);
	}

	@Test
	void getRates_throwsUnavailable_whenProviderTransportFails() {
		// GIVEN
		doThrow(new ResourceAccessException("connection refused"))
			.when(mockResponseSpec)
			.body(any(ParameterizedTypeReference.class));

		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.getRates(FROM_CURRENCY, TO_CURRENCY, List.of(REQUESTED_DATE))
		).isInstanceOf(ExchangeRateUnavailableException.class);
	}

	@Test
	void getRates_throwsUnavailable_whenResponseCarriesNoUsableQuote() {
		// GIVEN
		stubProviderResponse(
			Map.of(
				RATES_FIELD,
				Map.of(
					QUOTED_DATE.toString(),
					Map.of(OTHER_CURRENCY, EARLIER_RATE),
					REQUESTED_DATE.toString(),
					EARLIER_RATE
				)
			)
		);

		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.getRates(FROM_CURRENCY, TO_CURRENCY, List.of(REQUESTED_DATE))
		).isInstanceOf(ExchangeRateUnavailableException.class);
	}

	@Test
	void getRates_throwsUnavailable_whenQuotedRateIsNotPositive() {
		// GIVEN
		stubProviderResponse(stubbedResponse(QUOTED_DATE, BigDecimal.ZERO));

		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.getRates(FROM_CURRENCY, TO_CURRENCY, List.of(REQUESTED_DATE))
		).isInstanceOf(ExchangeRateUnavailableException.class);
	}

	@Test
	void getRates_acceptsAQuoteExactlySevenDaysOlder() {
		// GIVEN
		stubProviderResponse(stubbedResponse(OLDEST_ACCEPTED_QUOTE_DATE, EARLIER_RATE));

		// WHEN
		Map<LocalDate, BigDecimal> expectedRates = testSubject.getRates(
			FROM_CURRENCY,
			TO_CURRENCY,
			List.of(REQUESTED_DATE)
		);

		// THEN
		assertThat(expectedRates.get(REQUESTED_DATE)).isEqualByComparingTo(EARLIER_RATE);
	}

	@Test
	void getRates_throwsUnavailable_whenAQuoteIsEightDaysOlder() {
		// GIVEN
		stubProviderResponse(stubbedResponse(STALE_QUOTE_DATE, EARLIER_RATE));

		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.getRates(FROM_CURRENCY, TO_CURRENCY, List.of(REQUESTED_DATE))
		)
			.isInstanceOf(ExchangeRateUnavailableException.class)
			.hasMessageContaining(STALE_QUOTE_MESSAGE);
	}

	@Test
	void getRates_throwsUnavailable_whenSomeRequestedDateHasNoEarlierQuote() {
		// GIVEN
		stubProviderResponse(stubbedResponse(LATER_QUOTED_DATE, EARLIER_RATE));

		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.getRates(FROM_CURRENCY, TO_CURRENCY, List.of(REQUESTED_DATE))
		).isInstanceOf(ExchangeRateUnavailableException.class);
	}

	@Test
	void getRates_rethrowsSameException_whenProviderAlreadyReportsUnavailable() {
		// GIVEN
		ExchangeRateUnavailableException mockFailure = new ExchangeRateUnavailableException(
			"provider unavailable"
		);
		doThrow(mockFailure).when(mockResponseSpec).body(any(ParameterizedTypeReference.class));

		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.getRates(FROM_CURRENCY, TO_CURRENCY, List.of(REQUESTED_DATE))
		).isSameAs(mockFailure);
	}

	private void stubProviderResponse(Object stubbedResponse) {
		doReturn(stubbedResponse).when(mockResponseSpec).body(any(ParameterizedTypeReference.class));
	}

	private Map<String, Object> stubbedResponse(LocalDate quotedDate, BigDecimal rate) {
		return Map.of(RATES_FIELD, Map.of(quotedDate.toString(), Map.of(TO_CURRENCY, rate)));
	}
}
