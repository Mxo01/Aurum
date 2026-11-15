package com.backend.aurum.domain.asset.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.backend.aurum.domain.asset.dto.AssetDTO;
import com.backend.aurum.domain.asset.dto.CreateAssetDTO;
import com.backend.aurum.domain.asset.dto.UpdateAssetDTO;
import com.backend.aurum.domain.asset.facade.AssetFacade;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.model.UserPrincipal;
import com.backend.aurum.infrastructure.exception.GlobalExceptionHandler;
import java.util.List;
import java.util.UUID;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
class AssetControllerTest {

	private static final String ASSET_PATH = "/assets/{id}";
	private static final String PAYMENT_AMOUNT_FIELD = "paymentAmount";
	private static final String PAYMENT_AMOUNT_MESSAGE = "Payment amount cannot be negative";
	private static final int SINGLE_ERROR_COUNT = 1;
	private static final String NEGATIVE_PAYMENT_AMOUNT_BODY =
		"{\"name\":\"Renamed asset\",\"paymentAmount\":-1}";
	private static final String NULL_LIABILITY_TYPE_BODY =
		"{\"name\":\"Renamed asset\",\"liabilityType\":null}";

	@Mock
	private AssetFacade assetFacade;

	@InjectMocks
	private AssetController testSubject;

	private MockMvc mockMvc;
	private UserPrincipal mockPrincipal;
	private UUID mockUserId;

	@BeforeEach
	void setUp() {
		mockUserId = UUID.randomUUID();
		mockPrincipal = buildPrincipal(mockUserId);
		mockMvc = MockMvcBuilders.standaloneSetup(testSubject)
			.setControllerAdvice(new GlobalExceptionHandler())
			.setCustomArgumentResolvers(new AuthenticationPrincipalResolver(mockPrincipal))
			.build();
	}

	private UserPrincipal buildPrincipal(UUID userId) {
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), userId).create();
		return new UserPrincipal(mockUser, mock(Jwt.class));
	}

	@Test
	void getAllAssets_returnsOkWithDtosFromFacade() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UserPrincipal mockPrincipal = buildPrincipal(mockUserId);
		List<AssetDTO> stubbedDtos = Instancio.ofList(AssetDTO.class).size(3).create();
		when(assetFacade.getAllAssets(mockUserId)).thenReturn(stubbedDtos);

		// WHEN
		ResponseEntity<List<AssetDTO>> expectedResponse = testSubject.getAllAssets(mockPrincipal);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(expectedResponse.getBody()).isEqualTo(stubbedDtos);
	}

	@Test
	void getAssetById_returnsOkWithDtoFromFacade() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UUID mockAssetId = UUID.randomUUID();
		UserPrincipal mockPrincipal = buildPrincipal(mockUserId);
		AssetDTO stubbedDto = Instancio.create(AssetDTO.class);
		when(assetFacade.getAsset(mockAssetId, mockUserId)).thenReturn(stubbedDto);

		// WHEN
		ResponseEntity<AssetDTO> expectedResponse = testSubject.getAssetById(
			mockAssetId,
			mockPrincipal
		);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(expectedResponse.getBody()).isEqualTo(stubbedDto);
	}

	@Test
	void createAsset_returnsOkWithDtoFromFacade() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UserPrincipal mockPrincipal = buildPrincipal(mockUserId);
		CreateAssetDTO mockDto = Instancio.create(CreateAssetDTO.class);
		AssetDTO stubbedDto = Instancio.create(AssetDTO.class);
		when(assetFacade.createAsset(mockDto, mockUserId)).thenReturn(stubbedDto);

		// WHEN
		ResponseEntity<AssetDTO> expectedResponse = testSubject.createAsset(mockDto, mockPrincipal);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(expectedResponse.getBody()).isEqualTo(stubbedDto);
	}

	@Test
	void updateAsset_returnsOkWithDtoFromFacade() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UUID mockAssetId = UUID.randomUUID();
		UserPrincipal mockPrincipal = buildPrincipal(mockUserId);
		UpdateAssetDTO mockDto = Instancio.create(UpdateAssetDTO.class);
		AssetDTO stubbedDto = Instancio.create(AssetDTO.class);
		when(assetFacade.updateAssetLight(mockAssetId, mockDto, mockUserId)).thenReturn(stubbedDto);

		// WHEN
		ResponseEntity<AssetDTO> expectedResponse = testSubject.updateAsset(
			mockAssetId,
			mockDto,
			mockPrincipal
		);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(expectedResponse.getBody()).isEqualTo(stubbedDto);
	}

	@Test
	void updateAsset_returnsBadRequestWithTheFieldMessage_whenThePaymentAmountIsNegative()
		throws Exception {
		// WHEN
		ResultActions expectedResult = mockMvc.perform(
			put(ASSET_PATH, UUID.randomUUID())
				.contentType(MediaType.APPLICATION_JSON)
				.content(NEGATIVE_PAYMENT_AMOUNT_BODY)
		);

		// THEN
		expectedResult
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
			.andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
			.andExpect(jsonPath("$.errors.length()").value(SINGLE_ERROR_COUNT))
			.andExpect(jsonPath("$.errors[0].field").value(PAYMENT_AMOUNT_FIELD))
			.andExpect(jsonPath("$.errors[0].message").value(PAYMENT_AMOUNT_MESSAGE))
			.andExpect(jsonPath("$.message").doesNotExist())
			.andExpect(jsonPath("$.timestamp").doesNotExist());
		verifyNoInteractions(assetFacade);
	}

	@Test
	void updateAsset_reportsTheLiabilityTypeAsProvided_whenTheBodyCarriesItAsNull() throws Exception {
		// GIVEN
		UUID mockAssetId = UUID.randomUUID();
		AssetDTO stubbedDto = Instancio.create(AssetDTO.class);
		ArgumentCaptor<UpdateAssetDTO> captor = ArgumentCaptor.forClass(UpdateAssetDTO.class);
		when(
			assetFacade.updateAssetLight(eq(mockAssetId), captor.capture(), eq(mockUserId))
		).thenReturn(stubbedDto);

		// WHEN
		ResultActions expectedResult = mockMvc.perform(
			put(ASSET_PATH, mockAssetId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(NULL_LIABILITY_TYPE_BODY)
		);

		// THEN
		expectedResult.andExpect(status().isOk());
		UpdateAssetDTO expectedDto = captor.getValue();
		assertThat(expectedDto.isLiabilityTypeProvided()).isTrue();
		assertThat(expectedDto.getLiabilityType()).isNull();
	}

	@Test
	void deleteAsset_returnsNoContent() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UUID mockAssetId = UUID.randomUUID();
		UserPrincipal mockPrincipal = buildPrincipal(mockUserId);

		// WHEN
		ResponseEntity<Void> expectedResponse = testSubject.deleteAsset(mockAssetId, mockPrincipal);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		verify(assetFacade).deleteAsset(mockAssetId, mockUserId);
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
