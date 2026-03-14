package com.backend.aurum.domain.user.enums;

public enum Currency {
	EUR("EUR"),
	USD("USD");

	private final String value;

	Currency(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}
