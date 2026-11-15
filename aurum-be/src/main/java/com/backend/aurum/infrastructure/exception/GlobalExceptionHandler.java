package com.backend.aurum.infrastructure.exception;

import com.backend.aurum.domain.shared.exception.AccessDeniedException;
import com.backend.aurum.domain.shared.exception.ConflictException;
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
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JacksonException.Reference;
import tools.jackson.core.exc.StreamReadException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final String UNRECOGNIZED_STATUS_REASON = "Request failed";
	private static final String INVALID_BODY_REASON = "Request body is invalid";
	private static final String UNREADABLE_JSON_REASON = "Request body is not valid JSON";
	private static final String MISSING_BODY_REASON = "Request body is required";
	private static final String INVALID_VALUE_REASON = "Select a valid value";
	private static final String INVALID_CURRENCY_REASON = "Select a currency";
	private static final String INVALID_LANGUAGE_REASON = "Select a language";
	private static final String NOT_A_VALID_VALUE_REASON = "Not a valid value";
	private static final String REQUIRED_VALUE_REASON = "This value is required";
	private static final String MISSING_PICTURE_REASON = "Choose a picture to upload";
	private static final String PICTURE_TOO_LARGE_REASON =
		"The picture is too large, choose a smaller one";
	private static final String UNSUPPORTED_FORMAT_REASON = "This format is not supported";
	private static final String UNSUPPORTED_ACTION_REASON = "This action is not supported here";
	private static final String CURRENCY_FIELD = "currency";
	private static final String LOCALE_FIELD = "locale";
	private static final String PICTURE_PART_NAME = "file";
	private static final int MAX_LOGGED_CAUSE_DEPTH = 4;

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

	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException ex) {
		log.warn("Conflict: {}", ex.getMessage());
		return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
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

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
		log.warn("Unreadable request body: {}", exceptionTypes(ex));
		if (ex.getCause() == null) {
			return buildResponse(HttpStatus.BAD_REQUEST, MISSING_BODY_REASON);
		}
		JacksonException failure = findBindingFailure(ex.getCause());
		if (failure == null || failure instanceof StreamReadException || failure.getPath().isEmpty()) {
			return buildResponse(HttpStatus.BAD_REQUEST, UNREADABLE_JSON_REASON);
		}
		String field = lastPropertyName(failure.getPath());
		return buildResponse(HttpStatus.BAD_REQUEST, new ErrorDetail(field, invalidValueReason(field)));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
		MethodArgumentTypeMismatchException ex
	) {
		log.warn("Parameter '{}' rejected: {}", ex.getName(), exceptionTypes(ex));
		return buildResponse(HttpStatus.BAD_REQUEST, ex.getName(), NOT_A_VALID_VALUE_REASON);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ApiErrorResponse> handleMissingParameter(
		MissingServletRequestParameterException ex
	) {
		log.warn("Missing request parameter: {}", ex.getParameterName());
		return buildResponse(HttpStatus.BAD_REQUEST, ex.getParameterName(), REQUIRED_VALUE_REASON);
	}

	@ExceptionHandler(MissingServletRequestPartException.class)
	public ResponseEntity<ApiErrorResponse> handleMissingPart(MissingServletRequestPartException ex) {
		log.warn("Missing request part: {}", ex.getRequestPartName());
		return buildResponse(HttpStatus.BAD_REQUEST, PICTURE_PART_NAME, MISSING_PICTURE_REASON);
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ApiErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
		log.warn("Upload rejected, size exceeded: {}", ex.getMessage());
		return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE, PICTURE_TOO_LARGE_REASON);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
		log.warn("Request rejected: {}", exceptionTypes(ex));
		return buildResponse(HttpStatus.BAD_REQUEST, INVALID_BODY_REASON);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ApiErrorResponse> handleUnsupportedFormat(
		HttpMediaTypeNotSupportedException ex
	) {
		log.warn("Unsupported request format: {}", exceptionTypes(ex));
		return buildResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, UNSUPPORTED_FORMAT_REASON);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiErrorResponse> handleUnsupportedAction(
		HttpRequestMethodNotSupportedException ex
	) {
		log.warn("Unsupported request action: {}", exceptionTypes(ex));
		return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, UNSUPPORTED_ACTION_REASON);
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException ex) {
		HttpStatusCode statusCode = ex.getStatusCode();
		String reasonPhrase = resolveReasonPhrase(statusCode);
		log.warn("Request rejected with status {}: {}", statusCode.value(), ex.getReason());
		return buildResponse(statusCode.value(), reasonPhrase, List.of(noField(reasonPhrase)));
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

	// A domain @JsonCreator throws IllegalArgumentException, so the most specific cause
	// carries no path; the path lives on the mapping failure the converter wraps it in.
	private static JacksonException findBindingFailure(Throwable cause) {
		for (Throwable node = cause; node != null; node = node.getCause()) {
			if (node instanceof JacksonException failure) {
				return failure;
			}
		}
		return null;
	}

	// Client-controlled text (Jackson messages, rejected parameter values) must never
	// reach the log: only the class names of a bounded cause chain are safe to print.
	private static String exceptionTypes(Throwable failure) {
		List<String> types = new ArrayList<>();
		for (
			Throwable node = failure;
			node != null && types.size() < MAX_LOGGED_CAUSE_DEPTH;
			node = node.getCause()
		) {
			types.add(node.getClass().getSimpleName());
		}
		return String.join(", ", types);
	}

	private static String lastPropertyName(List<Reference> path) {
		return path.get(path.size() - 1).getPropertyName();
	}

	private static String invalidValueReason(String field) {
		if (CURRENCY_FIELD.equals(field)) {
			return INVALID_CURRENCY_REASON;
		}
		if (LOCALE_FIELD.equals(field)) {
			return INVALID_LANGUAGE_REASON;
		}
		return INVALID_VALUE_REASON;
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
