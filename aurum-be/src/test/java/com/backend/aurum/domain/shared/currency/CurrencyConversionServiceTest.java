package com.backend.aurum.domain.shared.currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyNoInteractions;

import com.backend.aurum.domain.shared.port.ExchangeRateProvider;
import com.backend.aurum.domain.user.enums.Currency;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CurrencyConversionServiceTest {

	private static final Currency TARGET_CURRENCY = Currency.USD;
	private static final Currency EURO = Currency.EUR;
	private static final Currency POUND = Currency.GBP;
	private static final LocalDate REFERENCE_DATE = LocalDate.of(2026, 11, 14);
	private static final LocalDate EARLIER_DATE = LocalDate.of(2026, 10, 1);
	private static final BigDecimal EURO_RATE = new BigDecimal("1.0800");
	private static final BigDecimal POUND_RATE = new BigDecimal("1.2700");

	@Mock
	private ExchangeRateProvider exchangeRateProvider;

	@InjectMocks
	private CurrencyConversionService testSubject;

	@Test
	void book_throwsNullPointer_whenTargetCurrencyIsMissing() {
		// GIVEN
		Collection<Currency> mockSources = List.of(EURO);

		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.book(null, mockSources, List.of(REFERENCE_DATE))
		).isInstanceOf(NullPointerException.class);
	}

	@Test
	void book_skipsNullAndTargetCurrencies_whenResolvingSources() {
		// GIVEN
		Collection<Currency> mockSources = Arrays.asList(null, TARGET_CURRENCY, null);
		Collection<LocalDate> mockDates = List.of(REFERENCE_DATE);

		// WHEN
		RateBook expectedBook = testSubject.book(TARGET_CURRENCY, mockSources, mockDates);

		// THEN
		assertThat(expectedBook.target()).isEqualTo(TARGET_CURRENCY);
		assertThat(expectedBook.rate(TARGET_CURRENCY, REFERENCE_DATE)).isEqualByComparingTo(
			BigDecimal.ONE
		);
		verifyNoInteractions(exchangeRateProvider);
	}

	@Test
	void book_returnsBookWithoutProviderCalls_whenNoSourceNeedsConversion() {
		// WHEN
		RateBook expectedBook = testSubject.book(TARGET_CURRENCY, List.of(), List.of(REFERENCE_DATE));

		// THEN
		assertThat(expectedBook.target()).isEqualTo(TARGET_CURRENCY);
		verifyNoInteractions(exchangeRateProvider);
	}

	@Test
	void book_fetchesEachDistinctSourceOnce_whenSourcesAreRepeated() {
		// GIVEN
		Collection<Currency> mockSources = List.of(EURO, EURO, POUND);
		Collection<LocalDate> mockDates = List.of(REFERENCE_DATE);
		Map<LocalDate, BigDecimal> mockEuroSeries = new HashMap<>(Map.of(EARLIER_DATE, EURO_RATE));
		Map<LocalDate, BigDecimal> mockPoundSeries = Map.of(EARLIER_DATE, POUND_RATE);
		AtomicInteger mockEuroCalls = new AtomicInteger();
		doAnswer(invocation -> {
			mockEuroCalls.incrementAndGet();
			return mockEuroSeries;
		})
			.when(exchangeRateProvider)
			.getRates(EURO.getValue(), TARGET_CURRENCY.getValue(), mockDates);
		doReturn(mockPoundSeries)
			.when(exchangeRateProvider)
			.getRates(POUND.getValue(), TARGET_CURRENCY.getValue(), mockDates);

		// WHEN
		RateBook expectedBook = testSubject.book(TARGET_CURRENCY, mockSources, mockDates);

		// THEN
		assertThat(mockEuroCalls).hasValue(1);
		assertThat(expectedBook.rate(EURO, REFERENCE_DATE)).isEqualByComparingTo(EURO_RATE);
		assertThat(expectedBook.rate(POUND, REFERENCE_DATE)).isEqualByComparingTo(POUND_RATE);
	}

	@Test
	void book_resolvesRateOfThePreviousTradingDay_whenSeriesHasNoQuoteForTheDate() {
		// GIVEN
		Collection<Currency> mockSources = List.of(EURO);
		Collection<LocalDate> mockDates = List.of(REFERENCE_DATE);
		doReturn(Map.of(EARLIER_DATE, EURO_RATE))
			.when(exchangeRateProvider)
			.getRates(EURO.getValue(), TARGET_CURRENCY.getValue(), mockDates);

		// WHEN
		RateBook expectedBook = testSubject.book(TARGET_CURRENCY, mockSources, mockDates);

		// THEN
		assertThat(expectedBook.rate(EURO, REFERENCE_DATE)).isEqualByComparingTo(EURO_RATE);
	}

	@Test
	void rate_returnsOneWithoutCallingProvider_whenCurrenciesAreEqual() {
		// WHEN
		BigDecimal expectedRate = testSubject.rate(EURO, EURO, REFERENCE_DATE);

		// THEN
		assertThat(expectedRate).isEqualByComparingTo(BigDecimal.ONE);
		verifyNoInteractions(exchangeRateProvider);
	}

	@Test
	void rate_delegatesToProvider_whenCurrenciesDiffer() {
		// GIVEN
		doReturn(EURO_RATE)
			.when(exchangeRateProvider)
			.getRate(EURO.getValue(), TARGET_CURRENCY.getValue(), REFERENCE_DATE);

		// WHEN
		BigDecimal expectedRate = testSubject.rate(EURO, TARGET_CURRENCY, REFERENCE_DATE);

		// THEN
		assertThat(expectedRate).isEqualByComparingTo(EURO_RATE);
	}
}
