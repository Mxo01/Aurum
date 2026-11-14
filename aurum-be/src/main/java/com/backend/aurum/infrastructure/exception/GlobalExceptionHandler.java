package com.backend.aurum.infrastructure.exception;

import com.backend.aurum.domain.shared.exception.AccessDeniedException;
import com.backend.aurum.domain.shared.exception.ExchangeRateUnavailableException;
import com.backend.aurum.domain.shared.exception.NotFoundException;
import com.backend.aurum.domain.shared.exception.ValidationException;
import java.time.Instant;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final String UNRECOGNIZED_STATUS_REASON = "Request failed";

	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException ex) {
		log.warn("Resource not found: {}", ex.getMessage());
		return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
		log.warn("Access denied: {}", ex.getMessage());
		return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
	}

	@ExceptionHandler(ValidationException.class)
	public ResponseEntity<Map<String, Object>> handleValidation(ValidationException ex) {
		log.warn("Validation failed: {}", ex.getMessage());
		return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(ExchangeRateUnavailableException.class)
	public ResponseEntity<Map<String, Object>> handleExchangeRateUnavailable(
		ExchangeRateUnavailableException ex
	) {
		log.error("Exchange rate provider unavailable: {}", ex.getMessage());
		return buildResponse(
			HttpStatus.SERVICE_UNAVAILABLE,
			"Exchange rates are temporarily unavailable, please retry later"
		);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
		log.warn("Bad request: {}", ex.getMessage());
		return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
		HttpStatusCode statusCode = ex.getStatusCode();
		String reasonPhrase = resolveReasonPhrase(statusCode);
		String message = ex.getReason() != null ? ex.getReason() : reasonPhrase;
		log.warn("Request rejected with status {}: {}", statusCode.value(), message);
		return buildResponse(statusCode.value(), reasonPhrase, message);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
		log.error("Unhandled exception", ex);
		return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
	}

	private static String resolveReasonPhrase(HttpStatusCode statusCode) {
		HttpStatus status = HttpStatus.resolve(statusCode.value());
		return status != null ? status.getReasonPhrase() : UNRECOGNIZED_STATUS_REASON;
	}

	private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
		return buildResponse(status.value(), status.getReasonPhrase(), message);
	}

	private ResponseEntity<Map<String, Object>> buildResponse(
		int statusValue,
		String reasonPhrase,
		String message
	) {
		return ResponseEntity.status(statusValue).body(
			Map.of(
				"status",
				statusValue,
				"error",
				reasonPhrase,
				"message",
				message,
				"timestamp",
				Instant.now().toString()
			)
		);
	}
}
