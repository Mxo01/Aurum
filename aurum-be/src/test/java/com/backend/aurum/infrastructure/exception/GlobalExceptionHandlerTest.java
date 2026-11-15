package com.backend.aurum.infrastructure.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.backend.aurum.domain.asset.dto.UpdateAssetDTO;
import com.backend.aurum.domain.shared.exception.AccessDeniedException;
import com.backend.aurum.domain.shared.exception.ExchangeRateUnavailableException;
import com.backend.aurum.domain.shared.exception.NotFoundException;
import com.backend.aurum.domain.shared.exception.ValidationException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

	private static final String EXCHANGE_RATE_UNAVAILABLE_MESSAGE =
		"Exchange rates are temporarily unavailable, please retry later";
	private static final String CONCURRENT_WRITE_MESSAGE =
		"This record was changed by another request, reload it and retry";
	private static final String STALE_ROW_DETAIL =
		"Row was updated or deleted by another transaction";
	private static final String CONFLICT_REASON =
		"Category still holds 2 asset(s); move or delete them first";
	private static final String DEFAULT_CATEGORY_FORBIDDEN_REASON =
		"Default categories cannot be deleted";
	private static final String UNRECOGNIZED_STATUS_REASON = "Request failed";
	private static final int UNRECOGNIZED_STATUS_CODE = 599;
	private static final String INVALID_BODY_REASON = "Request body is invalid";
	private static final String UPDATE_BODY_NAME = "updateAssetDTO";
	private static final String NAME_FIELD = "name";
	private static final String NAME_MESSAGE = "must not be blank";
	private static final String PAYMENT_AMOUNT_FIELD = "paymentAmount";
	private static final String PAYMENT_AMOUNT_MESSAGE = "must be greater than or equal to 0";
	private static final String FIELD_MESSAGE_SEPARATOR = " ";
	private static final String FIELD_ERROR_SEPARATOR = "; ";
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
	void handleOptimisticLock_returns409_withTheRetryMessage() {
		// GIVEN
		OptimisticLockingFailureException mockException = new OptimisticLockingFailureException(
			STALE_ROW_DETAIL
		);

		// WHEN
		ResponseEntity<Map<String, Object>> expectedResponse = testSubject.handleOptimisticLock(
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
		assertThat(expectedResponse.getBody()).containsEntry("message", CONCURRENT_WRITE_MESSAGE);
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

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidBodyInputs")
	void handleInvalidBody_answers400_withTheFieldErrorsJoinedIntoOneMessage(
		String scenario,
		List<FieldErrorSpec> mockFieldErrors,
		String expectedMessage
	) {
		// GIVEN
		MethodArgumentNotValidException mockException = invalidBodyException(mockFieldErrors);

		// WHEN
		ResponseEntity<Map<String, Object>> expectedResponse = testSubject.handleInvalidBody(
			mockException
		);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(expectedResponse.getBody()).containsOnlyKeys(RESPONSE_FIELDS);
		assertThat(expectedResponse.getBody()).containsEntry("status", HttpStatus.BAD_REQUEST.value());
		assertThat(expectedResponse.getBody()).containsEntry(
			"error",
			HttpStatus.BAD_REQUEST.getReasonPhrase()
		);
		assertThat(expectedResponse.getBody()).containsEntry("message", expectedMessage);
		assertThat(expectedResponse.getBody()).doesNotContainValue(null);
	}

	static Stream<Arguments> invalidBodyInputs() {
		return Stream.of(
			Arguments.of(
				"a single field error names its field",
				List.of(new FieldErrorSpec(PAYMENT_AMOUNT_FIELD, PAYMENT_AMOUNT_MESSAGE)),
				PAYMENT_AMOUNT_FIELD + FIELD_MESSAGE_SEPARATOR + PAYMENT_AMOUNT_MESSAGE
			),
			Arguments.of(
				"two field errors are joined in the order they arrive",
				List.of(
					new FieldErrorSpec(NAME_FIELD, NAME_MESSAGE),
					new FieldErrorSpec(PAYMENT_AMOUNT_FIELD, PAYMENT_AMOUNT_MESSAGE)
				),
				NAME_FIELD +
					FIELD_MESSAGE_SEPARATOR +
					NAME_MESSAGE +
					FIELD_ERROR_SEPARATOR +
					PAYMENT_AMOUNT_FIELD +
					FIELD_MESSAGE_SEPARATOR +
					PAYMENT_AMOUNT_MESSAGE
			),
			Arguments.of(
				"no field error falls back to the generic reason",
				List.<FieldErrorSpec>of(),
				INVALID_BODY_REASON
			)
		);
	}

	private record FieldErrorSpec(String field, String message) {}

	private static MethodArgumentNotValidException invalidBodyException(
		List<FieldErrorSpec> fieldErrors
	) {
		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(
			new UpdateAssetDTO(),
			UPDATE_BODY_NAME
		);
		fieldErrors.forEach(fieldError ->
			bindingResult.addError(
				new FieldError(UPDATE_BODY_NAME, fieldError.field(), fieldError.message())
			)
		);
		return new MethodArgumentNotValidException(updateBodyParameter(), bindingResult);
	}

	private static MethodParameter updateBodyParameter() {
		try {
			return new MethodParameter(
				UpdateBodyEndpoint.class.getDeclaredMethod("update", UpdateAssetDTO.class),
				0
			);
		} catch (NoSuchMethodException exception) {
			throw new IllegalStateException(exception);
		}
	}

	private static final class UpdateBodyEndpoint {

		void update(UpdateAssetDTO dto) {}
	}
}
