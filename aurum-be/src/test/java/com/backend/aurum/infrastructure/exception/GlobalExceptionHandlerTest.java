package com.backend.aurum.infrastructure.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.backend.aurum.domain.shared.exception.AccessDeniedException;
import com.backend.aurum.domain.shared.exception.ExchangeRateUnavailableException;
import com.backend.aurum.domain.shared.exception.NotFoundException;
import com.backend.aurum.domain.shared.exception.ValidationException;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

	private static final String EXCHANGE_RATE_UNAVAILABLE_MESSAGE =
		"Exchange rates are temporarily unavailable, please retry later";
	private static final String CONFLICT_REASON =
		"Category still holds 2 asset(s); move or delete them first";
	private static final String DEFAULT_CATEGORY_FORBIDDEN_REASON =
		"Default categories cannot be deleted";
	private static final String UNRECOGNIZED_STATUS_REASON = "Request failed";
	private static final int UNRECOGNIZED_STATUS_CODE = 599;
	private static final Set<String> RESPONSE_FIELDS = Set.of(
		"status",
		"error",
		"message",
		"timestamp"
	);

	@InjectMocks
	private GlobalExceptionHandler testSubject;

	@Test
	void handleResponseStatus_returns409_withTheConflictReason() {
		// GIVEN
		ResponseStatusException mockException = new ResponseStatusException(
			HttpStatus.CONFLICT,
			CONFLICT_REASON
		);

		// WHEN
		ResponseEntity<Map<String, Object>> expectedResponse = testSubject.handleResponseStatus(
			mockException
		);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(expectedResponse.getBody()).containsOnlyKeys(RESPONSE_FIELDS);
		assertThat(expectedResponse.getBody()).containsEntry("status", HttpStatus.CONFLICT.value());
		assertThat(expectedResponse.getBody()).containsEntry(
			"error",
			HttpStatus.CONFLICT.getReasonPhrase()
		);
		assertThat(expectedResponse.getBody()).containsEntry("message", CONFLICT_REASON);
		assertThat(expectedResponse.getBody()).doesNotContainValue(null);
	}

	@Test
	void handleResponseStatus_returns403_withTheForbiddenReason() {
		// GIVEN
		ResponseStatusException mockException = new ResponseStatusException(
			HttpStatus.FORBIDDEN,
			DEFAULT_CATEGORY_FORBIDDEN_REASON
		);

		// WHEN
		ResponseEntity<Map<String, Object>> expectedResponse = testSubject.handleResponseStatus(
			mockException
		);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(expectedResponse.getBody()).containsEntry("status", HttpStatus.FORBIDDEN.value());
		assertThat(expectedResponse.getBody()).containsEntry(
			"error",
			HttpStatus.FORBIDDEN.getReasonPhrase()
		);
		assertThat(expectedResponse.getBody()).containsEntry(
			"message",
			DEFAULT_CATEGORY_FORBIDDEN_REASON
		);
	}

	@Test
	void handleResponseStatus_fallsBackToTheReasonPhrase_whenTheExceptionHasNoReason() {
		// GIVEN
		ResponseStatusException mockException = new ResponseStatusException(HttpStatus.NOT_FOUND);

		// WHEN
		ResponseEntity<Map<String, Object>> expectedResponse = testSubject.handleResponseStatus(
			mockException
		);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(expectedResponse.getBody()).containsEntry(
			"message",
			HttpStatus.NOT_FOUND.getReasonPhrase()
		);
		assertThat(expectedResponse.getBody()).containsEntry(
			"error",
			HttpStatus.NOT_FOUND.getReasonPhrase()
		);
		assertThat(expectedResponse.getBody()).doesNotContainValue(null);
	}

	@Test
	void handleResponseStatus_fallsBackToTheGenericReason_whenTheStatusCodeIsUnrecognized() {
		// GIVEN
		ResponseStatusException mockException = new ResponseStatusException(
			HttpStatusCode.valueOf(UNRECOGNIZED_STATUS_CODE)
		);

		// WHEN
		ResponseEntity<Map<String, Object>> expectedResponse = testSubject.handleResponseStatus(
			mockException
		);

		// THEN
		assertThat(expectedResponse.getStatusCode().value()).isEqualTo(UNRECOGNIZED_STATUS_CODE);
		assertThat(expectedResponse.getBody()).containsEntry("status", UNRECOGNIZED_STATUS_CODE);
		assertThat(expectedResponse.getBody()).containsEntry("error", UNRECOGNIZED_STATUS_REASON);
		assertThat(expectedResponse.getBody()).containsEntry("message", UNRECOGNIZED_STATUS_REASON);
		assertThat(expectedResponse.getBody()).doesNotContainValue(null);
	}

	@Test
	void handleExchangeRateUnavailable_returns503_withTheDocumentedMessage() {
		// GIVEN
		ExchangeRateUnavailableException mockException = new ExchangeRateUnavailableException(
			"frankfurter refused the connection"
		);

		// WHEN
		ResponseEntity<Map<String, Object>> expectedResponse =
			testSubject.handleExchangeRateUnavailable(mockException);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
		assertThat(expectedResponse.getBody()).containsEntry("status", 503);
		assertThat(expectedResponse.getBody()).containsEntry(
			"error",
			HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase()
		);
		assertThat(expectedResponse.getBody()).containsEntry(
			"message",
			EXCHANGE_RATE_UNAVAILABLE_MESSAGE
		);
		assertThat(expectedResponse.getBody()).containsOnlyKeys(RESPONSE_FIELDS);
	}

	@Test
	void handleNotFound_returns404() {
		// GIVEN
		NotFoundException mockException = new NotFoundException("Asset not found");

		// WHEN
		ResponseEntity<Map<String, Object>> expectedResponse = testSubject.handleNotFound(
			mockException
		);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(expectedResponse.getBody()).containsEntry("message", "Asset not found");
		assertThat(expectedResponse.getBody()).containsEntry("status", 404);
	}

	@Test
	void handleAccessDenied_returns403() {
		// GIVEN
		AccessDeniedException mockException = new AccessDeniedException(
			"Asset does not belong to user"
		);

		// WHEN
		ResponseEntity<Map<String, Object>> expectedResponse = testSubject.handleAccessDenied(
			mockException
		);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(expectedResponse.getBody()).containsEntry(
			"message",
			"Asset does not belong to user"
		);
		assertThat(expectedResponse.getBody()).containsEntry("status", 403);
	}

	@Test
	void handleValidation_returns400() {
		// GIVEN
		ValidationException mockException = new ValidationException("Invalid amount");

		// WHEN
		ResponseEntity<Map<String, Object>> expectedResponse = testSubject.handleValidation(
			mockException
		);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(expectedResponse.getBody()).containsEntry("message", "Invalid amount");
		assertThat(expectedResponse.getBody()).containsEntry("status", 400);
	}

	@Test
	void handleIllegalArgument_returns400() {
		// GIVEN
		IllegalArgumentException mockException = new IllegalArgumentException("Bad input");

		// WHEN
		ResponseEntity<Map<String, Object>> expectedResponse = testSubject.handleIllegalArgument(
			mockException
		);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(expectedResponse.getBody()).containsEntry("message", "Bad input");
	}

	@Test
	void handleGeneric_returns500_withoutExposingInternalMessage() {
		// GIVEN
		Exception mockException = new Exception("Internal details");

		// WHEN
		ResponseEntity<Map<String, Object>> expectedResponse = testSubject.handleGeneric(mockException);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(expectedResponse.getBody()).containsEntry("message", "An unexpected error occurred");
		assertThat(expectedResponse.getBody()).containsEntry("status", 500);
	}
}
