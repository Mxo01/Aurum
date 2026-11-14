package com.backend.aurum.infrastructure.exchange;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ExchangeRateCacheKeyTest {

	private static final String FROM_CURRENCY = "GBP";
	private static final String TO_CURRENCY = "EUR";
	private static final String OTHER_CURRENCY = "CHF";
	private static final LocalDate FIRST_DATE = LocalDate.of(2026, 11, 14);
	private static final LocalDate SECOND_DATE = LocalDate.of(2026, 10, 1);
	private static final String MISSING_MARKER = "?";
	private static final String RATE_PREFIX = "rate|";
	private static final String SERIES_PREFIX = "rates|";

	private final ExchangeRateCacheKey testSubject = new ExchangeRateCacheKey();

	@Test
	void single_buildsRateKey_withPrefixPairAndDate() {
		// WHEN
		String expectedKey = testSubject.single(FROM_CURRENCY, TO_CURRENCY, FIRST_DATE);

		// THEN
		assertThat(expectedKey).isEqualTo(RATE_PREFIX + "GBP|EUR|2026-11-14");
	}

	@Test
	void single_marksEveryMissingField_whenArgumentsAreNull() {
		// WHEN
		String expectedKey = testSubject.single(null, null, null);

		// THEN
		assertThat(expectedKey).isEqualTo(
			RATE_PREFIX + MISSING_MARKER + "|" + MISSING_MARKER + "|" + MISSING_MARKER
		);
	}

	@Test
	void series_sortsAndDeduplicatesDates_whenInputIsUnorderedAndRepeated() {
		// GIVEN
		Collection<LocalDate> mockDates = List.of(FIRST_DATE, SECOND_DATE, FIRST_DATE);

		// WHEN
		String expectedKey = testSubject.series(FROM_CURRENCY, TO_CURRENCY, mockDates);

		// THEN
		assertThat(expectedKey).isEqualTo(SERIES_PREFIX + "GBP|EUR|2026-10-01,2026-11-14");
	}

	@Test
	void series_sharesKey_whenDateCollectionsDifferOnlyInOrderAndRepetition() {
		// GIVEN
		String mockOrderedKey = testSubject.series(
			FROM_CURRENCY,
			TO_CURRENCY,
			List.of(SECOND_DATE, FIRST_DATE)
		);
		Collection<LocalDate> mockShuffledDates = Set.of(FIRST_DATE, SECOND_DATE);

		// WHEN
		String expectedKey = testSubject.series(FROM_CURRENCY, TO_CURRENCY, mockShuffledDates);

		// THEN
		assertThat(expectedKey).isEqualTo(mockOrderedKey);
	}

	@Test
	void series_marksMissingDates_whenCollectionIsNull() {
		// WHEN
		String expectedKey = testSubject.series(FROM_CURRENCY, TO_CURRENCY, null);

		// THEN
		assertThat(expectedKey).isEqualTo(SERIES_PREFIX + "GBP|EUR|" + MISSING_MARKER);
	}

	@Test
	void series_returnsEmptyDateSegment_whenCollectionIsEmpty() {
		// WHEN
		String expectedKey = testSubject.series(FROM_CURRENCY, TO_CURRENCY, List.of());

		// THEN
		assertThat(expectedKey).isEqualTo(SERIES_PREFIX + "GBP|EUR|");
	}

	@Test
	void series_ignoresEntriesThatAreNotDates_whenCollectionIsUntyped() {
		// GIVEN
		Collection<Object> mockDates = List.of(FIRST_DATE, FIRST_DATE.toString());

		// WHEN
		String expectedKey = testSubject.series(FROM_CURRENCY, TO_CURRENCY, mockDates);

		// THEN
		assertThat(expectedKey).isEqualTo(SERIES_PREFIX + "GBP|EUR|2026-11-14");
	}

	@Test
	void series_differsByPair_whenDatesMatch() {
		// GIVEN
		Collection<LocalDate> mockDates = List.of(FIRST_DATE);

		// WHEN
		String expectedKey = testSubject.series(FROM_CURRENCY, TO_CURRENCY, mockDates);
		String expectedOtherKey = testSubject.series(OTHER_CURRENCY, TO_CURRENCY, mockDates);

		// THEN
		assertThat(expectedKey).isNotEqualTo(expectedOtherKey);
	}

	@Test
	void series_differsByDateSet_whenPairMatches() {
		// WHEN
		String expectedKey = testSubject.series(FROM_CURRENCY, TO_CURRENCY, List.of(FIRST_DATE));
		String expectedOtherKey = testSubject.series(
			FROM_CURRENCY,
			TO_CURRENCY,
			List.of(FIRST_DATE, SECOND_DATE)
		);

		// THEN
		assertThat(expectedKey).isNotEqualTo(expectedOtherKey);
	}

	@Test
	void singleAndSeries_neverCollide_onTheSamePairAndDate() {
		// WHEN
		String expectedSingleKey = testSubject.single(FROM_CURRENCY, TO_CURRENCY, FIRST_DATE);
		String expectedSeriesKey = testSubject.series(FROM_CURRENCY, TO_CURRENCY, List.of(FIRST_DATE));

		// THEN
		assertThat(expectedSingleKey).startsWith(RATE_PREFIX).isNotEqualTo(expectedSeriesKey);
		assertThat(expectedSeriesKey).startsWith(SERIES_PREFIX);
	}
}
