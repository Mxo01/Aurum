package com.backend.aurum.domain.shared.exception;

public class ExchangeRateUnavailableException extends RuntimeException {

	public ExchangeRateUnavailableException(String message) {
		super(message);
	}

	public ExchangeRateUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}
}
