package com.backend.aurum.domain.shared.exception;

/**
 * A rule the input violates. The field names the input the violation concerns, as the client renders
 * it, and stays null when the rule is not about one input.
 */
public class ValidationException extends RuntimeException {

	private final String field;

	public ValidationException(String message) {
		this(null, message);
	}

	public ValidationException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String getField() {
		return field;
	}
}
