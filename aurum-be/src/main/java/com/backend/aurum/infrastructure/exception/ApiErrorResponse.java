package com.backend.aurum.infrastructure.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * The one shape every HTTP rejection answers with. The detail carries the field the violation
 * concerns, so the client places the sentence next to the input it belongs to instead of guessing
 * from the prose.
 */
public record ApiErrorResponse(int status, String error, List<ErrorDetail> errors) {
	/**
	 * A rejection that concerns no single input, and every handler-generated answer, omits the field.
	 */
	public record ErrorDetail(
		@JsonInclude(JsonInclude.Include.NON_NULL) String field,
		String message
	) {}

	public static ApiErrorResponse of(int status, String error, String message) {
		return of(status, error, List.of(new ErrorDetail(null, message)));
	}

	public static ApiErrorResponse of(int status, String error, List<ErrorDetail> errors) {
		return new ApiErrorResponse(status, error, errors);
	}
}
