package com.backend.aurum.domain.shared.currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.backend.aurum.domain.shared.exception.ExchangeRateUnavailableException;
import com.backend.aurum.domain.user.enums.Currency;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class RateBookTest {

	private static final Currency TARGET_CURRENCY = Currency.USD;
	private static final Currency SOURCE_CURRENCY = Currency.EUR;
	private static final Currency OTHER_SOURCE_CURRENCY = Currency.GBP;
	private static final LocalDate REFERENCE_DATE = LocalDate.of(2026, 11, 14);
	private static final LocalDate EARLIER_DATE = LocalDate.of(2026, 10, 1);
	private static final LocalDate LATER_DATE = LocalDate.of(2026, 12, 1);
	private static final BigDecimal EARLIER_RATE = new BigDecimal("1.2000");
	private static final BigDecimal LATER_RATE = new BigDecimal("1.2345");
	private static final BigDecimal REFERENCE_RATE = new BigDecimal("1.2500");

	@Test
	void target_returnsCurrencyOfTheBook() {
		// GIVEN
		RateBook testSubject = bookWithSeries(
			Map.of(SOURCE_CURRENCY, seriesOf(Map.of(EARLIER_DATE, EARLIER_RATE)))
		);

		// WHEN
		Currency expectedTarget = testSubject.target();

		// THEN
		assertThat(expectedTarget).isEqualTo(TARGET_CURRENCY);
	}

	@Test
	void rate_returnsOne_whenSourceIsTheTarget() {
		// GIVEN
		RateBook testSubject = bookWithSeries(Map.of());

		// WHEN
		BigDecimal expectedRate = testSubject.rate(TARGET_CURRENCY, REFERENCE_DATE);

		// THEN
		assertThat(expectedRate).isEqualByComparingTo(BigDecimal.ONE);
	}

	@Test
	void rate_returnsExactRate_whenSeriesHasTheDate() {
		// GIVEN
		RateBook testSubject = bookWithSeries(
			Map.of(SOURCE_CURRENCY, seriesOf(Map.of(REFERENCE_DATE, REFERENCE_RATE)))
		);

		// WHEN
		BigDecimal expectedRate = testSubject.rate(SOURCE_CURRENCY, REFERENCE_DATE);

		// THEN
		assertThat(expectedRate).isEqualByComparingTo(REFERENCE_RATE);
	}

	@Test
	void rate_returnsPreviousRate_whenSeriesHasNoQuoteForTheDate() {
		// GIVEN
		RateBook testSubject = bookWithSeries(
			Map.of(SOURCE_CURRENCY, seriesOf(Map.of(EARLIER_DATE, EARLIER_RATE, LATER_DATE, LATER_RATE)))
		);

		// WHEN
		BigDecimal expectedRate = testSubject.rate(SOURCE_CURRENCY, REFERENCE_DATE);

		// THEN
		assertThat(expectedRate).isEqualByComparingTo(EARLIER_RATE);
	}

	@Test
	void rate_throwsUnavailable_whenSourceHasNoSeries() {
		// GIVEN
		RateBook testSubject = bookWithSeries(Map.of());

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.rate(SOURCE_CURRENCY, REFERENCE_DATE))
			.isInstanceOf(ExchangeRateUnavailableException.class)
			.hasMessageContaining(SOURCE_CURRENCY.getValue());
	}

	@Test
	void rate_throwsUnavailable_whenNoQuotePrecedesTheDate() {
		// GIVEN
		RateBook testSubject = bookWithSeries(
			Map.of(SOURCE_CURRENCY, seriesOf(Map.of(LATER_DATE, LATER_RATE)))
		);

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.rate(SOURCE_CURRENCY, REFERENCE_DATE)).isInstanceOf(
			ExchangeRateUnavailableException.class
		);
	}

	@Test
	void convert_returnsZero_whenAmountIsMissing() {
		// GIVEN
		RateBook testSubject = bookWithSeries(Map.of());

		// WHEN
		BigDecimal expectedAmount = testSubject.convert(null, SOURCE_CURRENCY, REFERENCE_DATE);

		// THEN
		assertThat(expectedAmount).isZero();
	}

	@Test
	void convert_scalesIdentity_whenSourceIsTheTarget() {
		// GIVEN
		RateBook testSubject = bookWithSeries(Map.of());
		BigDecimal mockAmount = new BigDecimal("100");

		// WHEN
		BigDecimal expectedAmount = testSubject.convert(mockAmount, TARGET_CURRENCY, REFERENCE_DATE);

		// THEN
		assertThat(expectedAmount).isEqualTo(new BigDecimal("100.0000"));
	}

	@Test
	void convert_multipliesAndScales_whenSourceIsForeign() {
		// GIVEN
		RateBook testSubject = bookWithSeries(
			Map.of(SOURCE_CURRENCY, seriesOf(Map.of(REFERENCE_DATE, LATER_RATE)))
		);
		BigDecimal mockAmount = new BigDecimal("100");

		// WHEN
		BigDecimal expectedAmount = testSubject.convert(mockAmount, SOURCE_CURRENCY, REFERENCE_DATE);

		// THEN
		assertThat(expectedAmount).isEqualTo(new BigDecimal("123.4500"));
	}

	@Test
	void convert_roundsHalfUp_whenTheFifthDecimalIsFive() {
		// GIVEN
		RateBook testSubject = bookWithSeries(Map.of());
		BigDecimal mockAmount = new BigDecimal("1.00005");

		// WHEN
		BigDecimal expectedAmount = testSubject.convert(mockAmount, TARGET_CURRENCY, REFERENCE_DATE);

		// THEN
		assertThat(expectedAmount).isEqualTo(new BigDecimal("1.0001"));
	}

	@Test
	void convert_throwsUnavailable_whenRateIsMissing() {
		// GIVEN
		RateBook testSubject = bookWithSeries(Map.of());
		BigDecimal mockAmount = new BigDecimal("100");

		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.convert(mockAmount, OTHER_SOURCE_CURRENCY, REFERENCE_DATE)
		).isInstanceOf(ExchangeRateUnavailableException.class);
	}

	private RateBook bookWithSeries(
		Map<Currency, NavigableMap<LocalDate, BigDecimal>> stubbedRatesBySource
	) {
		return new RateBook(TARGET_CURRENCY, stubbedRatesBySource);
	}

	private NavigableMap<LocalDate, BigDecimal> seriesOf(
		Map<LocalDate, BigDecimal> stubbedRatesByDate
	) {
		return new TreeMap<>(stubbedRatesByDate);
	}
}
