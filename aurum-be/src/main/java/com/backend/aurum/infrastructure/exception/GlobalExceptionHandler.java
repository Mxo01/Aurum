package com.backend.aurum.infrastructure.exception;

import com.backend.aurum.domain.shared.exception.AccessDeniedException;
import com.backend.aurum.domain.shared.exception.ExchangeRateUnavailableException;
import com.backend.aurum.domain.shared.exception.NotFoundException;
import com.backend.aurum.domain.shared.exception.ValidationException;
import com.backend.aurum.infrastructure.exception.ApiErrorResponse.ErrorDetail;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final String UNRECOGNIZED_STATUS_REASON = "Request failed";
	private static final String INVALID_BODY_REASON = "Request body is invalid";

	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleNotFound(NotFoundException ex) {
		log.warn("Resource not found: {}", ex.getMessage());
		return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex) {
		log.warn("Access denied: {}", ex.getMessage());
		return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
	}

	@ExceptionHandler(ValidationException.class)
	public ResponseEntity<ApiErrorResponse> handleValidation(ValidationException ex) {
		log.warn("Validation failed: {}", ex.getMessage());
		return buildResponse(HttpStatus.BAD_REQUEST, ex.getField(), ex.getMessage());
	}

	@ExceptionHandler(ExchangeRateUnavailableException.class)
	public ResponseEntity<ApiErrorResponse> handleExchangeRateUnavailable(
		ExchangeRateUnavailableException ex
	) {
		log.error("Exchange rate provider unavailable: {}", ex.getMessage());
		return buildResponse(
			HttpStatus.SERVICE_UNAVAILABLE,
			"Exchange rates are temporarily unavailable, please retry later"
		);
	}

	@ExceptionHandler(OptimisticLockingFailureException.class)
	public ResponseEntity<ApiErrorResponse> handleOptimisticLock(
		OptimisticLockingFailureException ex
	) {
		log.warn("Concurrent modification rejected: {}", ex.getMessage());
		return buildResponse(
			HttpStatus.CONFLICT,
			"This record was changed by another request, reload it and retry"
		);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleInvalidBody(MethodArgumentNotValidException ex) {
		List<ErrorDetail> details = new ArrayList<>();
		ex
			.getBindingResult()
			.getFieldErrors()
			.forEach(error -> details.add(new ErrorDetail(error.getField(), error.getDefaultMessage())));
		ex
			.getBindingResult()
			.getGlobalErrors()
			.forEach(error -> details.add(new ErrorDetail(null, error.getDefaultMessage())));
		if (details.isEmpty()) {
			details.add(new ErrorDetail(null, INVALID_BODY_REASON));
		}
		log.warn(
			"Invalid request body: {}",
			details.stream().map(ErrorDetail::message).collect(Collectors.joining("; "))
		);
		return buildResponse(HttpStatus.BAD_REQUEST, details);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
		log.warn("Bad request: {}", ex.getMessage());
		return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException ex) {
		HttpStatusCode statusCode = ex.getStatusCode();
		String reasonPhrase = resolveReasonPhrase(statusCode);
		String message = ex.getReason() != null ? ex.getReason() : reasonPhrase;
		log.warn("Request rejected with status {}: {}", statusCode.value(), message);
		return buildResponse(statusCode.value(), reasonPhrase, List.of(noField(message)));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex) {
		log.error("Unhandled exception", ex);
		return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
	}

	private static String resolveReasonPhrase(HttpStatusCode statusCode) {
		HttpStatus status = HttpStatus.resolve(statusCode.value());
		return status != null ? status.getReasonPhrase() : UNRECOGNIZED_STATUS_REASON;
	}

	private static ErrorDetail noField(String message) {
		return new ErrorDetail(null, message);
	}

	private static ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String message) {
		return buildResponse(status, noField(message));
	}

	private static ResponseEntity<ApiErrorResponse> buildResponse(
		HttpStatus status,
		String field,
		String message
	) {
		return buildResponse(status, new ErrorDetail(field, message));
	}

	private static ResponseEntity<ApiErrorResponse> buildResponse(
		HttpStatus status,
		ErrorDetail detail
	) {
		return buildResponse(status, List.of(detail));
	}

	private static ResponseEntity<ApiErrorResponse> buildResponse(
		HttpStatus status,
		List<ErrorDetail> details
	) {
		return buildResponse(status.value(), status.getReasonPhrase(), details);
	}

	private static ResponseEntity<ApiErrorResponse> buildResponse(
		int statusValue,
		String reasonPhrase,
		List<ErrorDetail> details
	) {
		return ResponseEntity.status(statusValue).body(
			ApiErrorResponse.of(statusValue, reasonPhrase, details)
		);
	}
}
