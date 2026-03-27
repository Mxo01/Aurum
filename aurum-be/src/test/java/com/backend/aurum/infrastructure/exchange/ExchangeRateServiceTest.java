package com.backend.aurum.infrastructure.exchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
	void getRate_throwsRuntimeException_whenApiFails() {
		// GIVEN
		String mockFrom = "XYZ";
		String mockTo = "ABC";
		LocalDate mockDate = LocalDate.of(1900, 1, 1);

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.getRate(mockFrom, mockTo, mockDate)).isInstanceOf(
			RuntimeException.class
		);
	}
}
