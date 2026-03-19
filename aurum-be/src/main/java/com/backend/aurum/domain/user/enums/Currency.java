package com.backend.aurum.domain.user.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Currency {
	AUD("AUD"),
	BGN("BGN"),
	BRL("BRL"),
	CAD("CAD"),
	CHF("CHF"),
	CNY("CNY"),
	CZK("CZK"),
	DKK("DKK"),
	EUR("EUR"),
	GBP("GBP"),
	HKD("HKD"),
	HUF("HUF"),
	IDR("IDR"),
	ILS("ILS"),
	INR("INR"),
	ISK("ISK"),
	JPY("JPY"),
	KRW("KRW"),
	MXN("MXN"),
	MYR("MYR"),
	NOK("NOK"),
	NZD("NZD"),
	PHP("PHP"),
	PLN("PLN"),
	RON("RON"),
	SEK("SEK"),
	SGD("SGD"),
	THB("THB"),
	TRY("TRY"),
	USD("USD"),
	ZAR("ZAR");

	private final String value;

	Currency(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}

	@JsonCreator
	public static Currency fromValue(String value) {
		for (Currency currency : values()) {
			if (currency.value.equalsIgnoreCase(value)) {
				return currency;
			}
		}
		throw new IllegalArgumentException("Unknown currency: " + value);
	}
}
