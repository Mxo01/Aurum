package com.backend.aurum.infrastructure.exchange;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

	@InjectMocks
	private ExchangeRateService testSubject;

	@Test
	void getRate_returnsOne_whenCurrenciesAreTheSame() {
		// GIVEN
		String mockCurrency = "EUR";
		LocalDate mockDate = LocalDate.now();

		// WHEN
		BigDecimal expectedRate = testSubject.getRate(mockCurrency, mockCurrency, mockDate);

		// THEN
		assertThat(expectedRate).isEqualByComparingTo(BigDecimal.ONE);
	}

	@Test
	void getRate_returnsFallback_whenApiFails() {
		// GIVEN
		String mockFrom = "XYZ";
		String mockTo = "ABC";
		LocalDate mockDate = LocalDate.of(1900, 1, 1);

		// WHEN — API will fail for unknown/historical date, expecting fallback to 1:1
		BigDecimal expectedRate = testSubject.getRate(mockFrom, mockTo, mockDate);

		// THEN
		assertThat(expectedRate).isEqualByComparingTo(BigDecimal.ONE);
	}
}
