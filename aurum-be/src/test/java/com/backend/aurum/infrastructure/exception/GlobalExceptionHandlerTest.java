package com.backend.aurum.infrastructure.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.backend.aurum.domain.asset.dto.UpdateAssetDTO;
import com.backend.aurum.domain.shared.exception.AccessDeniedException;
import com.backend.aurum.domain.shared.exception.ConflictException;
import com.backend.aurum.domain.shared.exception.ExchangeRateUnavailableException;
import com.backend.aurum.domain.shared.exception.NotFoundException;
import com.backend.aurum.domain.shared.exception.ValidationException;
import com.backend.aurum.infrastructure.exception.ApiErrorResponse.ErrorDetail;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.exc.MismatchedInputException;

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
	private static final String LIABILITY_TYPE_FIELD = "liabilityType";
	private static final String ID_PARAMETER = "id";
	private static final String ASSET_ID_PARAMETER = "assetId";
	private static final String PICTURE_PART = "file";
	private static final String REJECTED_PARAMETER_VALUE = "not-a-uuid";
	private static final String REJECTED_METHOD = "PATCH";
	private static final String UUID_PARAMETER_TYPE = "UUID";
	private static final long MAX_UPLOAD_BYTES = 2L * 1024L * 1024L;
	private static final String UNREADABLE_BODY_DETAIL =
		"JSON parse error: Unexpected character ('}' (code 125))";
	private static final String MISMATCHED_VALUE_DETAIL =
		"Cannot deserialize value, expected one of the accepted values";
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
	private static final String NOT_FOUND_REASON = "The requested resource does not exist";
	private static final String PINNED_NOT_FOUND_JSON =
		"{\"status\":404,\"error\":\"Not Found\",\"errors\":[{" +
		"\"message\":\"The requested resource does not exist\"}]}";
	private static final String UNKNOWN_REQUEST_URI = "http://localhost/unknown-resource";
	private static final String UNKNOWN_RESOURCE_PATH = "/unknown-resource";
	private static final String RUNTIME_FAILURE_DETAIL = "Asset repository refused the connection";

	private static final String UNREADABLE_BODY_LOG_PREFIX = "Unreadable request body: ";
	private static final String REJECTED_PARAMETER_LOG_PREFIX = "Parameter 'id' rejected: ";
	private static final String UNREADABLE_JSON_LOG_LINE =
		UNREADABLE_BODY_LOG_PREFIX + "HttpMessageNotReadableException, StreamReadException";
	private static final String REJECTED_PARAMETER_LOG_LINE =
		REJECTED_PARAMETER_LOG_PREFIX + "MethodArgumentTypeMismatchException";
	private static final String BOUNDED_CAUSE_CHAIN =
		"Request rejected: IllegalArgumentException, IllegalStateException, " +
		"IllegalStateException, IllegalStateException";

	private static final ObjectMapper JSON_SERIALIZER = new ObjectMapper();

	@InjectMocks
	private GlobalExceptionHandler testSubject;

	private ListAppender<ILoggingEvent> capturedLogLines;

	private Logger handlerLogger;

	private Level stubbedLogLevel;

	@BeforeEach
	void captureLogLines() {
		handlerLogger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
		stubbedLogLevel = handlerLogger.getLevel();
		handlerLogger.setLevel(Level.WARN);
		capturedLogLines = new ListAppender<>();
		capturedLogLines.start();
		handlerLogger.addAppender(capturedLogLines);
	}

	@AfterEach
	void releaseLogLines() {
		handlerLogger.detachAppender(capturedLogLines);
		handlerLogger.setLevel(stubbedLogLevel);
	}

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
		ConflictException mockConflictFromDomain = new ConflictException(CONFLICT_REASON);
		ExchangeRateUnavailableException mockExchangeRateUnavailableException =
			new ExchangeRateUnavailableException("frankfurter refused the connection");
		OptimisticLockingFailureException mockStaleRowException = new OptimisticLockingFailureException(
			STALE_ROW_DETAIL
		);
		IllegalArgumentException mockIllegalArgumentException = new IllegalArgumentException(
			ILLEGAL_ARGUMENT_MESSAGE
		);
		HttpMediaTypeNotSupportedException mockUnsupportedFormatException =
			new HttpMediaTypeNotSupportedException(
				MediaType.TEXT_PLAIN,
				List.of(MediaType.APPLICATION_JSON)
			);
		HttpRequestMethodNotSupportedException mockUnsupportedActionException =
			new HttpRequestMethodNotSupportedException(REJECTED_METHOD);
		MethodArgumentTypeMismatchException mockTypeMismatchException =
			new MethodArgumentTypeMismatchException(
				REJECTED_PARAMETER_VALUE,
				UUID.class,
				ID_PARAMETER,
				idParameter(),
				null
			);
		MissingServletRequestParameterException mockMissingParameterException =
			new MissingServletRequestParameterException(ASSET_ID_PARAMETER, UUID_PARAMETER_TYPE);
		MissingServletRequestPartException mockMissingPartException =
			new MissingServletRequestPartException(PICTURE_PART);
		MaxUploadSizeExceededException mockTooLargeException = new MaxUploadSizeExceededException(
			MAX_UPLOAD_BYTES
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
		RuntimeException mockUnexpectedRuntimeException = new RuntimeException(RUNTIME_FAILURE_DETAIL);

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
				"409 conflict from the domain",
				handler -> handler.handleConflict(mockConflictFromDomain),
				HttpStatus.CONFLICT.value(),
				HttpStatus.CONFLICT.getReasonPhrase(),
				new ErrorDetail(null, CONFLICT_REASON)
			),
			new FailureCase(
				"400 illegal argument without echoing its text",
				handler -> handler.handleIllegalArgument(mockIllegalArgumentException),
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				new ErrorDetail(null, INVALID_BODY_REASON)
			),
			new FailureCase(
				"400 path variable that does not parse",
				handler -> handler.handleTypeMismatch(mockTypeMismatchException),
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				new ErrorDetail(ID_PARAMETER, NOT_A_VALID_VALUE_REASON)
			),
			new FailureCase(
				"415 format that no endpoint reads",
				handler -> handler.handleUnsupportedFormat(mockUnsupportedFormatException),
				HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
				HttpStatus.UNSUPPORTED_MEDIA_TYPE.getReasonPhrase(),
				new ErrorDetail(null, UNSUPPORTED_FORMAT_REASON)
			),
			new FailureCase(
				"405 method that no endpoint answers",
				handler -> handler.handleUnsupportedAction(mockUnsupportedActionException),
				HttpStatus.METHOD_NOT_ALLOWED.value(),
				HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase(),
				new ErrorDetail(null, UNSUPPORTED_ACTION_REASON)
			),
			new FailureCase(
				"400 request parameter that is missing",
				handler -> handler.handleMissingParameter(mockMissingParameterException),
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				new ErrorDetail(ASSET_ID_PARAMETER, REQUIRED_VALUE_REASON)
			),
			new FailureCase(
				"400 upload part that is missing",
				handler -> handler.handleMissingPart(mockMissingPartException),
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				new ErrorDetail(PICTURE_PART, MISSING_PICTURE_REASON)
			),
			new FailureCase(
				"413 upload larger than the limit",
				handler -> handler.handleMaxUploadSize(mockTooLargeException),
				HttpStatus.PAYLOAD_TOO_LARGE.value(),
				HttpStatus.PAYLOAD_TOO_LARGE.getReasonPhrase(),
				new ErrorDetail(null, PICTURE_TOO_LARGE_REASON)
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
				"409 response status does not echo its reason",
				handler -> handler.handleResponseStatus(mockConflictException),
				HttpStatus.CONFLICT.value(),
				HttpStatus.CONFLICT.getReasonPhrase(),
				new ErrorDetail(null, HttpStatus.CONFLICT.getReasonPhrase())
			),
			new FailureCase(
				"403 response status does not echo its reason",
				handler -> handler.handleResponseStatus(mockForbiddenException),
				HttpStatus.FORBIDDEN.value(),
				HttpStatus.FORBIDDEN.getReasonPhrase(),
				new ErrorDetail(null, HttpStatus.FORBIDDEN.getReasonPhrase())
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
			),
			new FailureCase(
				"500 unexpected runtime failure keeps the catch-all contract",
				handler -> handler.handleGeneric(mockUnexpectedRuntimeException),
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
	void handleNoResource_answers404_withTheFixedSentence_andNotTheGenericEnvelope()
		throws JsonProcessingException {
		// GIVEN
		NoResourceFoundException mockException = new NoResourceFoundException(
			HttpMethod.GET,
			UNKNOWN_REQUEST_URI,
			UNKNOWN_RESOURCE_PATH
		);

		// WHEN
		ResponseEntity<ApiErrorResponse> expectedResponse = testSubject.handleNoResource(mockException);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		ApiErrorResponse expectedBody = expectedResponse.getBody();
		assertThat(expectedBody).isNotNull();
		assertThat(expectedBody.status()).isEqualTo(HttpStatus.NOT_FOUND.value());
		assertThat(expectedBody.error()).isEqualTo(HttpStatus.NOT_FOUND.getReasonPhrase());
		assertThat(expectedBody.errors()).containsExactly(new ErrorDetail(null, NOT_FOUND_REASON));
		assertThat(JSON_SERIALIZER.writeValueAsString(expectedBody)).isEqualTo(PINNED_NOT_FOUND_JSON);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("unreadableBodies")
	void handleUnreadableBody_answers400_withTheSentenceOfTheRejectedInput(
		UnreadableBodyCase bodyCase
	) {
		// WHEN
		ResponseEntity<ApiErrorResponse> expectedResponse = testSubject.handleUnreadableBody(
			bodyCase.exception()
		);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		ApiErrorResponse expectedBody = expectedResponse.getBody();
		assertThat(expectedBody).isNotNull();
		assertThat(expectedBody.status()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(expectedBody.error()).isEqualTo(HttpStatus.BAD_REQUEST.getReasonPhrase());
		assertThat(expectedBody.errors()).containsExactly(bodyCase.expectedDetail());
	}

	static Stream<UnreadableBodyCase> unreadableBodies() {
		return Stream.of(
			new UnreadableBodyCase(
				"missing body",
				unreadableBody(null),
				new ErrorDetail(null, MISSING_BODY_REASON)
			),
			new UnreadableBodyCase(
				"unparseable json",
				unreadableBody(new IllegalStateException(UNREADABLE_BODY_DETAIL)),
				new ErrorDetail(null, UNREADABLE_JSON_REASON)
			),
			new UnreadableBodyCase(
				"stream read failure",
				unreadableBody(new StreamReadException(UNREADABLE_BODY_DETAIL)),
				new ErrorDetail(null, UNREADABLE_JSON_REASON)
			),
			new UnreadableBodyCase(
				"unknown enum value",
				unreadableBody(mismatchedInput(LIABILITY_TYPE_FIELD)),
				new ErrorDetail(LIABILITY_TYPE_FIELD, INVALID_VALUE_REASON)
			),
			new UnreadableBodyCase(
				"unknown currency",
				unreadableBody(mismatchedInput(CURRENCY_FIELD)),
				new ErrorDetail(CURRENCY_FIELD, INVALID_CURRENCY_REASON)
			),
			new UnreadableBodyCase(
				"unknown language",
				unreadableBody(mismatchedInput(LOCALE_FIELD)),
				new ErrorDetail(LOCALE_FIELD, INVALID_LANGUAGE_REASON)
			),
			new UnreadableBodyCase(
				"value rejected before the input is known",
				unreadableBody(mismatchedInput(null)),
				new ErrorDetail(null, UNREADABLE_JSON_REASON)
			),
			new UnreadableBodyCase(
				"value rejected by a nameless element",
				unreadableBody(indexedMismatch()),
				new ErrorDetail(null, INVALID_VALUE_REASON)
			)
		);
	}

	@Test
	void handleUnreadableBody_logsOnlyTheCauseClasses_andNeverTheRejectedText() {
		// GIVEN
		HttpMessageNotReadableException mockException = unreadableBody(
			new StreamReadException(UNREADABLE_BODY_DETAIL)
		);

		// WHEN
		testSubject.handleUnreadableBody(mockException);

		// THEN
		assertThat(capturedLogLines.list)
			.singleElement()
			.extracting(ILoggingEvent::getFormattedMessage)
			.isEqualTo(UNREADABLE_JSON_LOG_LINE);
	}

	@Test
	void handleTypeMismatch_logsOnlyTheCauseClasses_andNeverTheRejectedValue() {
		// GIVEN
		MethodArgumentTypeMismatchException mockException = new MethodArgumentTypeMismatchException(
			REJECTED_PARAMETER_VALUE,
			UUID.class,
			ID_PARAMETER,
			idParameter(),
			null
		);

		// WHEN
		testSubject.handleTypeMismatch(mockException);

		// THEN
		assertThat(capturedLogLines.list)
			.singleElement()
			.extracting(ILoggingEvent::getFormattedMessage)
			.isEqualTo(REJECTED_PARAMETER_LOG_LINE);
	}

	@Test
	void handleIllegalArgument_boundsTheLoggedCauseChain_andNeverLogsItsText() {
		// GIVEN
		Throwable mockOldestCause = new IllegalStateException(ILLEGAL_ARGUMENT_MESSAGE);
		Throwable mockThirdCause = new IllegalStateException(ILLEGAL_ARGUMENT_MESSAGE, mockOldestCause);
		Throwable mockSecondCause = new IllegalStateException(ILLEGAL_ARGUMENT_MESSAGE, mockThirdCause);
		Throwable mockFirstCause = new IllegalStateException(ILLEGAL_ARGUMENT_MESSAGE, mockSecondCause);
		IllegalArgumentException mockException = new IllegalArgumentException(
			ILLEGAL_ARGUMENT_MESSAGE,
			mockFirstCause
		);

		// WHEN
		testSubject.handleIllegalArgument(mockException);

		// THEN
		assertThat(capturedLogLines.list)
			.singleElement()
			.extracting(ILoggingEvent::getFormattedMessage)
			.isEqualTo(BOUNDED_CAUSE_CHAIN);
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

	private record UnreadableBodyCase(
		String scenario,
		HttpMessageNotReadableException exception,
		ErrorDetail expectedDetail
	) {}

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

	private static MethodParameter idParameter() {
		try {
			return new MethodParameter(AssetByIdEndpoint.class.getDeclaredMethod("get", UUID.class), 0);
		} catch (NoSuchMethodException exception) {
			throw new IllegalStateException(exception);
		}
	}

	private static HttpMessageNotReadableException unreadableBody(Throwable cause) {
		HttpInputMessage mockInputMessage = mock(HttpInputMessage.class);
		return cause == null
			? new HttpMessageNotReadableException(UNREADABLE_BODY_DETAIL, mockInputMessage)
			: new HttpMessageNotReadableException(UNREADABLE_BODY_DETAIL, cause, mockInputMessage);
	}

	private static MismatchedInputException mismatchedInput(String field) {
		MismatchedInputException mockMismatch = MismatchedInputException.from(
			null,
			UpdateAssetDTO.class,
			MISMATCHED_VALUE_DETAIL
		);
		if (field != null) {
			mockMismatch.prependPath(new Object(), field);
		}
		return mockMismatch;
	}

	private static MismatchedInputException indexedMismatch() {
		MismatchedInputException mockMismatch = MismatchedInputException.from(
			null,
			UpdateAssetDTO.class,
			MISMATCHED_VALUE_DETAIL
		);
		mockMismatch.prependPath(new Object(), 0);
		return mockMismatch;
	}

	private static final class UpdateBodyEndpoint {

		void update(UpdateAssetDTO dto) {}
	}

	private static final class AssetByIdEndpoint {

		void get(UUID id) {}
	}
}
