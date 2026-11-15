package com.backend.aurum.domain.asset.validation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.backend.aurum.domain.asset.controller.SnapshotController;
import com.backend.aurum.domain.asset.dto.CreateSnapshotDTO;
import com.backend.aurum.domain.asset.facade.SnapshotFacade;
import com.backend.aurum.domain.asset.mapper.SnapshotMapper;
import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.asset.service.AssetService;
import com.backend.aurum.domain.asset.service.SnapshotService;
import com.backend.aurum.domain.shared.currency.CurrencyConversionService;
import com.backend.aurum.domain.shared.port.ExchangeRateProvider;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.model.UserPrincipal;
import com.backend.aurum.domain.user.repository.UserRepository;
import com.backend.aurum.infrastructure.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@ExtendWith(MockitoExtension.class)
class SnapshotValidationServiceHttpTest {

	private static final UUID REQUESTING_USER_ID = UUID.randomUUID();

	private static final UUID OTHER_USER_ID = UUID.randomUUID();

	private static final LocalDate REFERENCE_DATE = LocalDate.of(2026, 4, 20);

	private static final BigDecimal VALID_AMOUNT = new BigDecimal("500");

	private static final BigDecimal NEGATIVE_AMOUNT = new BigDecimal("-1");

	private static final BigDecimal VALID_EXCHANGE_RATE = new BigDecimal("1.5");

	private static final String SNAPSHOTS_PATH = "/snapshots";

	private static final String AMOUNT_FIELD = "amountOriginalCurrency";

	private static final String NEGATIVE_AMOUNT_MESSAGE = "Amount cannot be negative";

	private static final String FOREIGN_ASSET_MESSAGE = "This asset does not belong to you";

	private static final String PINNED_AMOUNT_VIOLATION_JSON =
		"{\"status\":400,\"error\":\"Bad Request\",\"errors\":[{" +
		"\"field\":\"amountOriginalCurrency\",\"message\":\"Amount cannot be negative\"}]}";

	private static final String PINNED_FORBIDDEN_JSON =
		"{\"status\":403,\"error\":\"Forbidden\",\"errors\":[{" +
		"\"message\":\"This asset does not belong to you\"}]}";

	private static final ObjectMapper JSON_SERIALIZER = JsonMapper.builder()
		.addModule(new JavaTimeModule())
		.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
		.build();

	@Mock
	private AssetRepository assetRepository;

	@Mock
	private Clock clock;

	@Mock
	private SnapshotService snapshotService;

	@Mock
	private SnapshotMapper snapshotMapper;

	@Mock
	private AssetService assetService;

	@Mock
	private ExchangeRateProvider exchangeRateProvider;

	@Mock
	private CurrencyConversionService currencyConversionService;

	@Mock
	private UserRepository userRepository;

	private MockMvc mockMvc;
	private final UserPrincipal mockPrincipal = buildPrincipal(REQUESTING_USER_ID);

	@BeforeEach
	void setUp() {
		SnapshotValidationService testSubject = new SnapshotValidationService(assetRepository, clock);
		SnapshotFacade snapshotFacade = new SnapshotFacade(
			snapshotService,
			snapshotMapper,
			testSubject,
			assetService,
			exchangeRateProvider,
			currencyConversionService,
			userRepository
		);
		mockMvc = MockMvcBuilders.standaloneSetup(new SnapshotController(snapshotFacade))
			.setControllerAdvice(new GlobalExceptionHandler())
			.setCustomArgumentResolvers(new AuthenticationPrincipalResolver(mockPrincipal))
			.build();
	}

	@Test
	void createSnapshot_answers400WithTheAmountField_asThePinnedEnvelope() throws Exception {
		// GIVEN
		CreateSnapshotDTO mockRequest = buildRequest(
			UUID.randomUUID(),
			NEGATIVE_AMOUNT,
			VALID_EXCHANGE_RATE
		);

		// WHEN
		ResultActions expectedResult = mockMvc.perform(
			post(SNAPSHOTS_PATH)
				.contentType(MediaType.APPLICATION_JSON)
				.content(JSON_SERIALIZER.writeValueAsString(mockRequest))
		);

		// THEN
		expectedResult
			.andExpect(status().isBadRequest())
			.andExpect(content().string(PINNED_AMOUNT_VIOLATION_JSON))
			.andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
			.andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
			.andExpect(jsonPath("$.errors[0].field").value(AMOUNT_FIELD))
			.andExpect(jsonPath("$.errors[0].message").value(NEGATIVE_AMOUNT_MESSAGE))
			.andExpect(jsonPath("$.message").doesNotExist())
			.andExpect(jsonPath("$.timestamp").doesNotExist());
	}

	@Test
	void createSnapshot_answers403WithNoField_whenTheAssetBelongsToAnotherUser() throws Exception {
		// GIVEN
		Asset mockAsset = buildAsset(OTHER_USER_ID);
		CreateSnapshotDTO mockRequest = buildRequest(
			mockAsset.getId(),
			VALID_AMOUNT,
			VALID_EXCHANGE_RATE
		);
		when(assetRepository.findById(mockAsset.getId())).thenReturn(Optional.of(mockAsset));

		// WHEN
		ResultActions expectedResult = mockMvc.perform(
			post(SNAPSHOTS_PATH)
				.contentType(MediaType.APPLICATION_JSON)
				.content(JSON_SERIALIZER.writeValueAsString(mockRequest))
		);

		// THEN
		expectedResult
			.andExpect(status().isForbidden())
			.andExpect(content().string(PINNED_FORBIDDEN_JSON))
			.andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
			.andExpect(jsonPath("$.error").value(HttpStatus.FORBIDDEN.getReasonPhrase()))
			.andExpect(jsonPath("$.errors[0].field").doesNotExist())
			.andExpect(jsonPath("$.errors[0].message").value(FOREIGN_ASSET_MESSAGE))
			.andExpect(jsonPath("$.message").doesNotExist())
			.andExpect(jsonPath("$.timestamp").doesNotExist());
	}

	private static CreateSnapshotDTO buildRequest(
		UUID assetId,
		BigDecimal amountOriginalCurrency,
		BigDecimal exchangeRateToBase
	) {
		CreateSnapshotDTO dto = new CreateSnapshotDTO();
		dto.setAssetId(assetId);
		dto.setReferenceDate(REFERENCE_DATE);
		dto.setAmountOriginalCurrency(amountOriginalCurrency);
		dto.setExchangeRateToBase(exchangeRateToBase);
		return dto;
	}

	private static Asset buildAsset(UUID ownerId) {
		User mockOwner = Instancio.of(User.class).set(Select.field(User::getId), ownerId).create();
		Asset mockAsset = new Asset();
		mockAsset.setId(UUID.randomUUID());
		mockAsset.setUser(mockOwner);
		return mockAsset;
	}

	private static UserPrincipal buildPrincipal(UUID userId) {
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), userId).create();
		return new UserPrincipal(mockUser, mock(Jwt.class));
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
