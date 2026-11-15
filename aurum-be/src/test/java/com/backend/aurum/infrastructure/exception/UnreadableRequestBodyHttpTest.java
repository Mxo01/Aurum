package com.backend.aurum.infrastructure.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.backend.aurum.domain.asset.controller.AssetController;
import com.backend.aurum.domain.asset.controller.SnapshotController;
import com.backend.aurum.domain.asset.facade.AssetFacade;
import com.backend.aurum.domain.asset.facade.SnapshotFacade;
import com.backend.aurum.domain.cashflow.controller.CashFlowController;
import com.backend.aurum.domain.cashflow.facade.CashFlowFacade;
import com.backend.aurum.domain.user.controller.UserController;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.model.UserPrincipal;
import com.backend.aurum.domain.user.service.UserService;
import java.util.UUID;
import java.util.stream.Stream;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@ExtendWith(MockitoExtension.class)
class UnreadableRequestBodyHttpTest {

	private static final String ASSETS_PATH = "/assets";
	private static final String REJECTED_UUID = "not-a-uuid";
	private static final String CASHFLOWS_PATH = "/cashflows/{year}";
	private static final String REJECTED_YEAR = "not-a-year";
	private static final String SNAPSHOTS_BULK_PATH = "/snapshots/bulk";
	private static final String USER_CURRENCY_PATH = "/users/currency";
	private static final String USER_LOCALE_PATH = "/users/locale";
	private static final String USER_PICTURE_PATH = "/users/picture";
	private static final String TOO_LARGE_UPLOAD_PATH = "/uploads";
	private static final long MAX_UPLOAD_BYTES = 2L * 1024L * 1024L;

	private static final String MALFORMED_JSON_BODY = "{\"name\":";
	private static final String TRUNCATED_OBJECT_BODY = "{\"name\":\"x\"";
	private static final String TRUNCATED_ARRAY_BODY = "[1,";
	private static final String TRUNCATED_NESTED_BODY = "{\"a\":{,";
	private static final String ARRAY_INSTEAD_OF_OBJECT_BODY = "[]";
	private static final String SCALAR_INSTEAD_OF_OBJECT_BODY = "\"x\"";
	private static final String UNREADABLE_FORMAT_BODY = "plain text";
	private static final String UNKNOWN_LIABILITY_TYPE_BODY = "{\"liabilityType\":\"SOMETIMES\"}";
	private static final String UNKNOWN_PAYMENT_FREQUENCY_BODY = "{\"paymentFrequency\":\"DAILY\"}";
	private static final String UNKNOWN_CURRENCY_BODY = "{\"currency\":\"XYZ\"}";
	private static final String UNKNOWN_LOCALE_BODY = "{\"locale\":\"xx-XX\"}";
	private static final String NO_SNAPSHOTS_SELECTED = "[]";
	private static final String FORGED_LINE_TEXT = "WARN forged entry";
	private static final String ESCAPED_LINE_BREAK_BODY =
		"{\"currency\":\"EU\\r\\n" + FORGED_LINE_TEXT + "\"}";

	private static final String ID_FIELD = "id";
	private static final String YEAR_FIELD = "year";
	private static final String ASSET_ID_FIELD = "assetId";
	private static final String FILE_FIELD = "file";
	private static final String LIABILITY_TYPE_FIELD = "liabilityType";
	private static final String PAYMENT_FREQUENCY_FIELD = "paymentFrequency";
	private static final String CURRENCY_FIELD = "currency";
	private static final String LOCALE_FIELD = "locale";

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
	private static final String UNREADABLE_BODY_LOG_PREFIX = "Unreadable request body: ";
	private static final String CLASS_NAME_PATTERN = "[A-Za-z][A-Za-z0-9]*";
	private static final String CAUSE_CLASS_CHAIN_PATTERN =
		"^" + UNREADABLE_BODY_LOG_PREFIX + CLASS_NAME_PATTERN + "(, " + CLASS_NAME_PATTERN + "){0,3}$";

	@Mock
	private AssetFacade assetFacade;

	@Mock
	private SnapshotFacade snapshotFacade;

	@Mock
	private CashFlowFacade cashFlowFacade;

	@Mock
	private UserService userService;

	private MockMvc mockMvc;

	private ListAppender<ILoggingEvent> capturedLogLines;

	private Logger handlerLogger;

	private Level stubbedLogLevel;

	@BeforeEach
	void setUp() {
		User mockUser = Instancio.of(User.class)
			.set(Select.field(User::getId), UUID.randomUUID())
			.create();
		UserPrincipal mockPrincipal = new UserPrincipal(mockUser, mock(Jwt.class));
		mockMvc = MockMvcBuilders.standaloneSetup(
			new AssetController(assetFacade),
			new SnapshotController(snapshotFacade),
			new CashFlowController(cashFlowFacade),
			new UserController(userService),
			new OversizedUploadEndpoint()
		)
			.setControllerAdvice(new GlobalExceptionHandler())
			.setCustomArgumentResolvers(new AuthenticationPrincipalResolver(mockPrincipal))
			.build();
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
	@MethodSource("bodyMistakes")
	void answers400WithTheSentenceOfTheInput_whenTheBodyCannotBeRead(BodyMistake mistake)
		throws Exception {
		// WHEN
		ResultActions expectedResult = mockMvc.perform(mistake.request());

		// THEN
		expectedResult
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
			.andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
			.andExpect(jsonPath("$.errors[0].message").value(mistake.expectedMessage()));
		if (mistake.expectedField() == null) {
			expectedResult.andExpect(jsonPath("$.errors[0].field").doesNotExist());
		} else {
			expectedResult.andExpect(jsonPath("$.errors[0].field").value(mistake.expectedField()));
		}
	}

	static Stream<BodyMistake> bodyMistakes() {
		return Stream.of(
			new BodyMistake(
				"malformed json",
				post(ASSETS_PATH).contentType(MediaType.APPLICATION_JSON).content(MALFORMED_JSON_BODY),
				null,
				UNREADABLE_JSON_REASON
			),
			new BodyMistake(
				"truncated object",
				post(ASSETS_PATH).contentType(MediaType.APPLICATION_JSON).content(TRUNCATED_OBJECT_BODY),
				null,
				UNREADABLE_JSON_REASON
			),
			new BodyMistake(
				"truncated array",
				post(ASSETS_PATH).contentType(MediaType.APPLICATION_JSON).content(TRUNCATED_ARRAY_BODY),
				null,
				UNREADABLE_JSON_REASON
			),
			new BodyMistake(
				"truncated nested object",
				post(ASSETS_PATH).contentType(MediaType.APPLICATION_JSON).content(TRUNCATED_NESTED_BODY),
				null,
				UNREADABLE_JSON_REASON
			),
			new BodyMistake(
				"array instead of an object",
				post(ASSETS_PATH)
					.contentType(MediaType.APPLICATION_JSON)
					.content(ARRAY_INSTEAD_OF_OBJECT_BODY),
				null,
				UNREADABLE_JSON_REASON
			),
			new BodyMistake(
				"scalar instead of an object",
				post(ASSETS_PATH)
					.contentType(MediaType.APPLICATION_JSON)
					.content(SCALAR_INSTEAD_OF_OBJECT_BODY),
				null,
				UNREADABLE_JSON_REASON
			),
			new BodyMistake(
				"missing body",
				post(ASSETS_PATH).contentType(MediaType.APPLICATION_JSON),
				null,
				MISSING_BODY_REASON
			),
			new BodyMistake(
				"unknown enum value",
				post(ASSETS_PATH)
					.contentType(MediaType.APPLICATION_JSON)
					.content(UNKNOWN_LIABILITY_TYPE_BODY),
				LIABILITY_TYPE_FIELD,
				INVALID_VALUE_REASON
			),
			new BodyMistake(
				"unknown payment frequency",
				post(ASSETS_PATH)
					.contentType(MediaType.APPLICATION_JSON)
					.content(UNKNOWN_PAYMENT_FREQUENCY_BODY),
				PAYMENT_FREQUENCY_FIELD,
				INVALID_VALUE_REASON
			),
			new BodyMistake(
				"unknown currency",
				put(USER_CURRENCY_PATH)
					.contentType(MediaType.APPLICATION_JSON)
					.content(UNKNOWN_CURRENCY_BODY),
				CURRENCY_FIELD,
				INVALID_CURRENCY_REASON
			),
			new BodyMistake(
				"unknown language",
				put(USER_LOCALE_PATH).contentType(MediaType.APPLICATION_JSON).content(UNKNOWN_LOCALE_BODY),
				LOCALE_FIELD,
				INVALID_LANGUAGE_REASON
			)
		);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("requestMistakes")
	void answers400WithTheFieldOfTheInput_whenTheRequestDoesNotMatchTheEndpoint(
		RequestMistake mistake
	) throws Exception {
		// WHEN
		ResultActions expectedResult = mockMvc.perform(mistake.request());

		// THEN
		expectedResult
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
			.andExpect(jsonPath("$.errors[0].field").value(mistake.expectedField()))
			.andExpect(jsonPath("$.errors[0].message").value(mistake.expectedMessage()));
	}

	static Stream<RequestMistake> requestMistakes() {
		return Stream.of(
			new RequestMistake(
				"path variable that is not a uuid",
				get(ASSETS_PATH + "/{id}", REJECTED_UUID),
				ID_FIELD,
				NOT_A_VALID_VALUE_REASON
			),
			new RequestMistake(
				"path variable that is not a number",
				get(CASHFLOWS_PATH, REJECTED_YEAR),
				YEAR_FIELD,
				NOT_A_VALID_VALUE_REASON
			),
			new RequestMistake(
				"missing request parameter",
				delete(SNAPSHOTS_BULK_PATH)
					.contentType(MediaType.APPLICATION_JSON)
					.content(NO_SNAPSHOTS_SELECTED),
				ASSET_ID_FIELD,
				REQUIRED_VALUE_REASON
			),
			new RequestMistake(
				"missing upload part",
				multipart(USER_PICTURE_PATH),
				FILE_FIELD,
				MISSING_PICTURE_REASON
			)
		);
	}

	@Test
	void answers413WithoutAField_whenTheUploadExceedsTheLimit() throws Exception {
		// GIVEN
		RequestBuilder oversizedUpload = multipart(TOO_LARGE_UPLOAD_PATH);

		// WHEN
		ResultActions expectedResult = mockMvc.perform(oversizedUpload);

		// THEN
		expectedResult
			.andExpect(status().isPayloadTooLarge())
			.andExpect(jsonPath("$.status").value(HttpStatus.PAYLOAD_TOO_LARGE.value()))
			.andExpect(jsonPath("$.error").value(HttpStatus.PAYLOAD_TOO_LARGE.getReasonPhrase()))
			.andExpect(jsonPath("$.errors[0].field").doesNotExist())
			.andExpect(jsonPath("$.errors[0].message").value(PICTURE_TOO_LARGE_REASON));
	}

	@Test
	void answers415WithoutAField_whenNoEndpointReadsTheContentType() throws Exception {
		// GIVEN
		RequestBuilder unreadableFormat = post(ASSETS_PATH)
			.contentType(MediaType.TEXT_PLAIN)
			.content(UNREADABLE_FORMAT_BODY);

		// WHEN
		ResultActions expectedResult = mockMvc.perform(unreadableFormat);

		// THEN
		expectedResult
			.andExpect(status().isUnsupportedMediaType())
			.andExpect(jsonPath("$.status").value(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value()))
			.andExpect(jsonPath("$.error").value(HttpStatus.UNSUPPORTED_MEDIA_TYPE.getReasonPhrase()))
			.andExpect(jsonPath("$.errors[0].field").doesNotExist())
			.andExpect(jsonPath("$.errors[0].message").value(UNSUPPORTED_FORMAT_REASON));
	}

	@Test
	void answers405WithoutAField_whenNoEndpointAnswersTheMethod() throws Exception {
		// GIVEN
		RequestBuilder unsupportedMethod = patch(ASSETS_PATH);

		// WHEN
		ResultActions expectedResult = mockMvc.perform(unsupportedMethod);

		// THEN
		expectedResult
			.andExpect(status().isMethodNotAllowed())
			.andExpect(jsonPath("$.status").value(HttpStatus.METHOD_NOT_ALLOWED.value()))
			.andExpect(jsonPath("$.error").value(HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase()))
			.andExpect(jsonPath("$.errors[0].field").doesNotExist())
			.andExpect(jsonPath("$.errors[0].message").value(UNSUPPORTED_ACTION_REASON));
	}

	@Test
	void logsOnlyTheCauseClasses_whenTheRejectedValueHidesLineBreaks() throws Exception {
		// GIVEN
		RequestBuilder lineBreakingBody = put(USER_CURRENCY_PATH)
			.contentType(MediaType.APPLICATION_JSON)
			.content(ESCAPED_LINE_BREAK_BODY);

		// WHEN
		ResultActions expectedResult = mockMvc.perform(lineBreakingBody);

		// THEN
		expectedResult
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value(CURRENCY_FIELD))
			.andExpect(jsonPath("$.errors[0].message").value(INVALID_CURRENCY_REASON));
		assertThat(capturedLogLines.list)
			.singleElement()
			.satisfies(logLine ->
				assertThat(logLine.getFormattedMessage())
					.matches(CAUSE_CLASS_CHAIN_PATTERN)
					.doesNotContain(FORGED_LINE_TEXT)
			);
	}

	private record BodyMistake(
		String scenario,
		RequestBuilder request,
		String expectedField,
		String expectedMessage
	) {}

	private record RequestMistake(
		String scenario,
		RequestBuilder request,
		String expectedField,
		String expectedMessage
	) {}

	@RestController
	private static final class OversizedUploadEndpoint {

		@PostMapping(TOO_LARGE_UPLOAD_PATH)
		public void upload() {
			throw new MaxUploadSizeExceededException(MAX_UPLOAD_BYTES);
		}
	}

	private static final class AuthenticationPrincipalResolver
		implements HandlerMethodArgumentResolver
	{

		private final UserPrincipal principal;

		private AuthenticationPrincipalResolver(UserPrincipal principal) {
			this.principal = principal;
		}

		@Override
		public boolean supportsParameter(MethodParameter parameter) {
			return (
				parameter.hasParameterAnnotation(AuthenticationPrincipal.class) &&
				UserPrincipal.class.equals(parameter.getParameterType())
			);
		}

		@Override
		public Object resolveArgument(
			MethodParameter parameter,
			ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest,
			WebDataBinderFactory binderFactory
		) {
			return principal;
		}
	}
}
