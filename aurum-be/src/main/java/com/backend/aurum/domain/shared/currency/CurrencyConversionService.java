package com.backend.aurum.domain.shared.currency;

import com.backend.aurum.domain.shared.port.ExchangeRateProvider;
import com.backend.aurum.domain.user.enums.Currency;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrencyConversionService {

	private final ExchangeRateProvider exchangeRateProvider;

	public RateBook book(Currency target, Collection<Currency> sources, Collection<LocalDate> dates) {
		Currency resolvedTarget = Objects.requireNonNull(target, "target currency is required");
		Map<Currency, NavigableMap<LocalDate, BigDecimal>> ratesBySource = new EnumMap<>(
			Currency.class
		);
		for (Currency source : new LinkedHashSet<>(sources)) {
			if (source == null || source == resolvedTarget) continue;
			ratesBySource.put(
				source,
				new TreeMap<>(
					exchangeRateProvider.getRates(source.getValue(), resolvedTarget.getValue(), dates)
				)
			);
		}
		return new RateBook(resolvedTarget, ratesBySource);
	}

	public BigDecimal rate(Currency from, Currency to, LocalDate date) {
		if (from == to) return BigDecimal.ONE;
		return exchangeRateProvider.getRate(from.getValue(), to.getValue(), date);
	}
}
