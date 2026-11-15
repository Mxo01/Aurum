package com.backend.aurum.infrastructure.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.backend.aurum.domain.asset.dto.UpdateAssetDTO;
import com.backend.aurum.domain.shared.exception.AccessDeniedException;
import com.backend.aurum.domain.shared.exception.ExchangeRateUnavailableException;
import com.backend.aurum.domain.shared.exception.NotFoundException;
import com.backend.aurum.domain.shared.exception.ValidationException;
import com.backend.aurum.infrastructure.exception.ApiErrorResponse.ErrorDetail;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
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
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

	private static final String NOT_FOUND_MESSAGE = "Asset not found";
	private static final String ACCESS_DENIED_MESSAGE = "Asset does not belong to user";
	private static final String VALIDATION_MESSAGE = "Invalid amount";
	private static final String ILLEGAL_ARGUMENT_MESSAGE = "Bad input";
	private static final String GENERIC_MESSAGE = "An unexpected error occurred";
	private static final String EXCHANGE_RATE_UNAVAILABLE_MESSAGE =
		"Exchange rates are temporarily unavailable, please retry later";
	private static final String CONCURRENT_WRITE_MESSAGE =
		"This record was changed by another request, reload it and retry";
	private static final String STALE_ROW_DETAIL =
		"Row was updated or deleted by another transaction";
	private static final String CONFLICT_REASON =
		"This category still has assets, move or delete them first";
	private static final String DEFAULT_CATEGORY_FORBIDDEN_REASON =
		"A default category cannot be deleted";
	private static final String UNRECOGNIZED_STATUS_REASON = "Request failed";
	private static final int UNRECOGNIZED_STATUS_CODE = 599;
	private static final String INVALID_BODY_REASON = "Request body is invalid";
	private static final String UPDATE_BODY_NAME = "updateAssetDTO";
	private static final String NAME_FIELD = "name";
	private static final String NAME_MESSAGE = "must not be blank";
	private static final String PAYMENT_AMOUNT_FIELD = "paymentAmount";
	private static final String PAYMENT_AMOUNT_MESSAGE = "Payment amount cannot be negative";
	private static final String GLOBAL_ERROR_MESSAGE = "Payment amount must match the liability type";
	private static final String PINNED_FIELD_VIOLATION_JSON =
		"{\"status\":400,\"error\":\"Bad Request\",\"errors\":[{" +
		"\"field\":\"paymentAmount\",\"message\":\"Payment amount cannot be negative\"}]}";
	private static final String PINNED_FIELDLESS_JSON =
		"{\"status\":503,\"error\":\"Service Unavailable\",\"errors\":[{" +
		"\"message\":\"Exchange rates are temporarily unavailable, please retry later\"}]}";

	private static final ObjectMapper JSON_SERIALIZER = new ObjectMapper();

	@InjectMocks
	private GlobalExceptionHandler testSubject;

	@ParameterizedTest(name = "{0}")
	@MethodSource("handlerFailures")
	void answersTheErrorEnvelope_withTheStatusAndReasonPhraseOfTheFailure(FailureCase failureCase) {
		// GIVEN
		Function<GlobalExceptionHandler, ResponseEntity<ApiErrorResponse>> mockHandler =
			failureCase.handler();

		// WHEN
		ResponseEntity<ApiErrorResponse> expectedResponse = mockHandler.apply(testSubject);

		// THEN
		assertThat(expectedResponse.getStatusCode().value()).isEqualTo(failureCase.expectedStatus());
		ApiErrorResponse expectedBody = expectedResponse.getBody();
		assertThat(expectedBody).isNotNull();
		assertThat(expectedBody.status()).isEqualTo(failureCase.expectedStatus());
		assertThat(expectedBody.error()).isEqualTo(failureCase.expectedError());
		assertThat(expectedBody.errors()).containsExactly(failureCase.expectedDetail());
	}

	static Stream<FailureCase> handlerFailures() {
		NotFoundException mockNotFoundException = new NotFoundException(NOT_FOUND_MESSAGE);
		AccessDeniedException mockAccessDeniedException = new AccessDeniedException(
			ACCESS_DENIED_MESSAGE
		);
		ValidationException mockFieldViolation = new ValidationException(
			PAYMENT_AMOUNT_FIELD,
			VALIDATION_MESSAGE
		);
		ValidationException mockFieldlessViolation = new ValidationException(VALIDATION_MESSAGE);
		ExchangeRateUnavailableException mockExchangeRateUnavailableException =
			new ExchangeRateUnavailableException("frankfurter refused the connection");
		OptimisticLockingFailureException mockStaleRowException = new OptimisticLockingFailureException(
			STALE_ROW_DETAIL
		);
		IllegalArgumentException mockIllegalArgumentException = new IllegalArgumentException(
			ILLEGAL_ARGUMENT_MESSAGE
		);
		ResponseStatusException mockConflictException = new ResponseStatusException(
			HttpStatus.CONFLICT,
			CONFLICT_REASON
		);
		ResponseStatusException mockForbiddenException = new ResponseStatusException(
			HttpStatus.FORBIDDEN,
			DEFAULT_CATEGORY_FORBIDDEN_REASON
		);
		ResponseStatusException mockReasonlessException = new ResponseStatusException(
			HttpStatus.NOT_FOUND
		);
		ResponseStatusException mockUnrecognizedException = new ResponseStatusException(
			HttpStatusCode.valueOf(UNRECOGNIZED_STATUS_CODE)
		);
		Exception mockUnexpectedException = new Exception("Internal details");

		return Stream.of(
			new FailureCase(
				"404 not found",
				handler -> handler.handleNotFound(mockNotFoundException),
				HttpStatus.NOT_FOUND.value(),
				HttpStatus.NOT_FOUND.getReasonPhrase(),
				new ErrorDetail(null, NOT_FOUND_MESSAGE)
			),
			new FailureCase(
				"403 access denied",
				handler -> handler.handleAccessDenied(mockAccessDeniedException),
				HttpStatus.FORBIDDEN.value(),
				HttpStatus.FORBIDDEN.getReasonPhrase(),
				new ErrorDetail(null, ACCESS_DENIED_MESSAGE)
			),
			new FailureCase(
				"400 validation naming an input",
				handler -> handler.handleValidation(mockFieldViolation),
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				new ErrorDetail(PAYMENT_AMOUNT_FIELD, VALIDATION_MESSAGE)
			),
			new FailureCase(
				"400 validation without an input",
				handler -> handler.handleValidation(mockFieldlessViolation),
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				new ErrorDetail(null, VALIDATION_MESSAGE)
			),
			new FailureCase(
				"400 illegal argument",
				handler -> handler.handleIllegalArgument(mockIllegalArgumentException),
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				new ErrorDetail(null, ILLEGAL_ARGUMENT_MESSAGE)
			),
			new FailureCase(
				"503 exchange rate unavailable",
				handler -> handler.handleExchangeRateUnavailable(mockExchangeRateUnavailableException),
				HttpStatus.SERVICE_UNAVAILABLE.value(),
				HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
				new ErrorDetail(null, EXCHANGE_RATE_UNAVAILABLE_MESSAGE)
			),
			new FailureCase(
				"409 stale write",
				handler -> handler.handleOptimisticLock(mockStaleRowException),
				HttpStatus.CONFLICT.value(),
				HttpStatus.CONFLICT.getReasonPhrase(),
				new ErrorDetail(null, CONCURRENT_WRITE_MESSAGE)
			),
			new FailureCase(
				"409 response status with a reason",
				handler -> handler.handleResponseStatus(mockConflictException),
				HttpStatus.CONFLICT.value(),
				HttpStatus.CONFLICT.getReasonPhrase(),
				new ErrorDetail(null, CONFLICT_REASON)
			),
			new FailureCase(
				"403 response status with a reason",
				handler -> handler.handleResponseStatus(mockForbiddenException),
				HttpStatus.FORBIDDEN.value(),
				HttpStatus.FORBIDDEN.getReasonPhrase(),
				new ErrorDetail(null, DEFAULT_CATEGORY_FORBIDDEN_REASON)
			),
			new FailureCase(
				"response status without a reason falls back to the status phrase",
				handler -> handler.handleResponseStatus(mockReasonlessException),
				HttpStatus.NOT_FOUND.value(),
				HttpStatus.NOT_FOUND.getReasonPhrase(),
				new ErrorDetail(null, HttpStatus.NOT_FOUND.getReasonPhrase())
			),
			new FailureCase(
				"response status with an unrecognized code falls back to the generic phrase",
				handler -> handler.handleResponseStatus(mockUnrecognizedException),
				UNRECOGNIZED_STATUS_CODE,
				UNRECOGNIZED_STATUS_REASON,
				new ErrorDetail(null, UNRECOGNIZED_STATUS_REASON)
			),
			new FailureCase(
				"500 unexpected failure",
				handler -> handler.handleGeneric(mockUnexpectedException),
				HttpStatus.INTERNAL_SERVER_ERROR.value(),
				HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
				new ErrorDetail(null, GENERIC_MESSAGE)
			)
		);
	}

	@Test
	void serializesAFieldViolation_asThePinnedEnvelopeByteForByte() throws JsonProcessingException {
		// GIVEN
		ValidationException mockException = new ValidationException(
			PAYMENT_AMOUNT_FIELD,
			PAYMENT_AMOUNT_MESSAGE
		);

		// WHEN
		ApiErrorResponse expectedBody = testSubject.handleValidation(mockException).getBody();

		// THEN
		assertThat(JSON_SERIALIZER.writeValueAsString(expectedBody)).isEqualTo(
			PINNED_FIELD_VIOLATION_JSON
		);
	}

	@Test
	void serializesAFieldlessRejection_asThePinnedEnvelope_withoutTheFieldKey()
		throws JsonProcessingException {
		// GIVEN
		ExchangeRateUnavailableException mockException = new ExchangeRateUnavailableException(
			"frankfurter refused the connection"
		);

		// WHEN
		ApiErrorResponse expectedBody = testSubject
			.handleExchangeRateUnavailable(mockException)
			.getBody();

		// THEN
		assertThat(JSON_SERIALIZER.writeValueAsString(expectedBody)).isEqualTo(PINNED_FIELDLESS_JSON);
	}

	@Test
	void handleInvalidBody_answers400_withOneDetailPerInvalidField_inTheOrderTheyArrive() {
		// GIVEN
		MethodArgumentNotValidException mockException = invalidBodyException(
			List.of(
				new FieldErrorSpec(NAME_FIELD, NAME_MESSAGE),
				new FieldErrorSpec(PAYMENT_AMOUNT_FIELD, PAYMENT_AMOUNT_MESSAGE)
			),
			List.of()
		);

		// WHEN
		ResponseEntity<ApiErrorResponse> expectedResponse = testSubject.handleInvalidBody(
			mockException
		);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		ApiErrorResponse expectedBody = expectedResponse.getBody();
		assertThat(expectedBody).isNotNull();
		assertThat(expectedBody.error()).isEqualTo(HttpStatus.BAD_REQUEST.getReasonPhrase());
		assertThat(expectedBody.errors()).containsExactly(
			new ErrorDetail(NAME_FIELD, NAME_MESSAGE),
			new ErrorDetail(PAYMENT_AMOUNT_FIELD, PAYMENT_AMOUNT_MESSAGE)
		);
	}

	@Test
	void handleInvalidBody_answers400_withAFieldlessDetail_forAGlobalError() {
		// GIVEN
		MethodArgumentNotValidException mockException = invalidBodyException(
			List.of(),
			List.of(GLOBAL_ERROR_MESSAGE)
		);

		// WHEN
		ResponseEntity<ApiErrorResponse> expectedResponse = testSubject.handleInvalidBody(
			mockException
		);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		ApiErrorResponse expectedBody = expectedResponse.getBody();
		assertThat(expectedBody).isNotNull();
		assertThat(expectedBody.errors()).containsExactly(new ErrorDetail(null, GLOBAL_ERROR_MESSAGE));
	}

	@Test
	void handleInvalidBody_answers400_withTheGenericReason_whenNoErrorIsBound() {
		// GIVEN
		MethodArgumentNotValidException mockException = invalidBodyException(List.of(), List.of());

		// WHEN
		ResponseEntity<ApiErrorResponse> expectedResponse = testSubject.handleInvalidBody(
			mockException
		);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		ApiErrorResponse expectedBody = expectedResponse.getBody();
		assertThat(expectedBody).isNotNull();
		assertThat(expectedBody.error()).isEqualTo(HttpStatus.BAD_REQUEST.getReasonPhrase());
		assertThat(expectedBody.errors()).containsExactly(new ErrorDetail(null, INVALID_BODY_REASON));
	}

	private record FieldErrorSpec(String field, String message) {}

	private record FailureCase(
		String scenario,
		Function<GlobalExceptionHandler, ResponseEntity<ApiErrorResponse>> handler,
		int expectedStatus,
		String expectedError,
		ErrorDetail expectedDetail
	) {}

	private static MethodArgumentNotValidException invalidBodyException(
		List<FieldErrorSpec> fieldErrors,
		List<String> globalErrors
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
		globalErrors.forEach(globalError ->
			bindingResult.addError(new ObjectError(UPDATE_BODY_NAME, globalError))
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
