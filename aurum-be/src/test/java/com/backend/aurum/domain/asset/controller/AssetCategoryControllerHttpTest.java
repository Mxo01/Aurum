package com.backend.aurum.domain.asset.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.backend.aurum.domain.asset.dto.CreateAssetCategoryDTO;
import com.backend.aurum.domain.asset.facade.AssetCategoryFacade;
import com.backend.aurum.domain.shared.exception.AccessDeniedException;
import com.backend.aurum.domain.shared.exception.ConflictException;
import com.backend.aurum.domain.shared.exception.ValidationException;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.model.UserPrincipal;
import com.backend.aurum.infrastructure.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class AssetCategoryControllerHttpTest {

	private static final String NAME_FIELD = "name";
	private static final String BLANK_NAME = "";
	private static final String MISSING_NAME_MESSAGE = "Category name is required";
	private static final String CATEGORY_IN_USE_REASON =
		"This category still has assets, move or delete them first";
	private static final String FOREIGN_CATEGORY_REASON = "You can only delete your own categories";
	private static final String UNEXPECTED_ERROR_MESSAGE = "An unexpected error occurred";
	private static final String PINNED_FIELD_VIOLATION_JSON =
		"{\"status\":400,\"error\":\"Bad Request\",\"errors\":[{" +
		"\"field\":\"name\",\"message\":\"Category name is required\"}]}";
	private static final String PINNED_CONFLICT_JSON =
		"{\"status\":409,\"error\":\"Conflict\",\"errors\":[{" +
		"\"message\":\"This category still has assets, move or delete them first\"}]}";
	private static final String PINNED_FORBIDDEN_JSON =
		"{\"status\":403,\"error\":\"Forbidden\",\"errors\":[{" +
		"\"message\":\"You can only delete your own categories\"}]}";

	private static final ObjectMapper JSON_SERIALIZER = new ObjectMapper();

	@Mock
	private AssetCategoryFacade categoryFacade;

	private AssetCategoryController testSubject;
	private MockMvc mockMvc;
	private UserPrincipal mockPrincipal;
	private UUID mockUserId;
	private UUID mockCategoryId;

	@BeforeEach
	void setUp() {
		mockUserId = UUID.randomUUID();
		mockCategoryId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		mockPrincipal = new UserPrincipal(mockUser, mock(Jwt.class));
		testSubject = new AssetCategoryController(categoryFacade);
		mockMvc = MockMvcBuilders.standaloneSetup(testSubject)
			.setControllerAdvice(new GlobalExceptionHandler())
			.setCustomArgumentResolvers(new AuthenticationPrincipalResolver(mockPrincipal))
			.build();
	}

	@Test
	void createCategory_answers400WithTheNameField_asThePinnedEnvelope() throws Exception {
		// GIVEN
		CreateAssetCategoryDTO mockRequest = new CreateAssetCategoryDTO();
		mockRequest.setName(BLANK_NAME);
		when(categoryFacade.createCategory(mockRequest, mockUserId)).thenThrow(
			new ValidationException(NAME_FIELD, MISSING_NAME_MESSAGE)
		);

		// WHEN
		ResultActions expectedResult = mockMvc.perform(
			post("/categories")
				.contentType(MediaType.APPLICATION_JSON)
				.content(JSON_SERIALIZER.writeValueAsString(mockRequest))
		);

		// THEN
		expectedResult
			.andExpect(status().isBadRequest())
			.andExpect(content().string(PINNED_FIELD_VIOLATION_JSON))
			.andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
			.andExpect(jsonPath("$.error").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
			.andExpect(jsonPath("$.errors[0].field").value(NAME_FIELD))
			.andExpect(jsonPath("$.errors[0].message").value(MISSING_NAME_MESSAGE))
			.andExpect(jsonPath("$.message").doesNotExist())
			.andExpect(jsonPath("$.timestamp").doesNotExist());
	}

	@Test
	void deleteCategory_answers409WithNoField_asThePinnedEnvelope() throws Exception {
		// GIVEN
		doThrow(new ConflictException(CATEGORY_IN_USE_REASON))
			.when(categoryFacade)
			.deleteCategory(mockCategoryId, mockUserId);

		// WHEN
		ResultActions expectedResult = mockMvc.perform(delete("/categories/{id}", mockCategoryId));

		// THEN
		expectedResult
			.andExpect(status().isConflict())
			.andExpect(content().string(PINNED_CONFLICT_JSON))
			.andExpect(jsonPath("$.status").value(HttpStatus.CONFLICT.value()))
			.andExpect(jsonPath("$.error").value(HttpStatus.CONFLICT.getReasonPhrase()))
			.andExpect(jsonPath("$.errors[0].field").doesNotExist())
			.andExpect(jsonPath("$.errors[0].message").value(CATEGORY_IN_USE_REASON))
			.andExpect(jsonPath("$.message").doesNotExist())
			.andExpect(jsonPath("$.timestamp").doesNotExist());
	}

	@Test
	void deleteCategory_answers403WithNoField_whenTheCategoryBelongsToAnotherUser() throws Exception {
		// GIVEN
		doThrow(new AccessDeniedException(FOREIGN_CATEGORY_REASON))
			.when(categoryFacade)
			.deleteCategory(mockCategoryId, mockUserId);

		// WHEN
		ResultActions expectedResult = mockMvc.perform(delete("/categories/{id}", mockCategoryId));

		// THEN
		expectedResult
			.andExpect(status().isForbidden())
			.andExpect(content().string(PINNED_FORBIDDEN_JSON))
			.andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
			.andExpect(jsonPath("$.error").value(HttpStatus.FORBIDDEN.getReasonPhrase()))
			.andExpect(jsonPath("$.errors[0].field").doesNotExist())
			.andExpect(jsonPath("$.errors[0].message").value(FOREIGN_CATEGORY_REASON))
			.andExpect(jsonPath("$.message").doesNotExist())
			.andExpect(jsonPath("$.timestamp").doesNotExist());
	}

	@Test
	void deleteCategory_returnsNoContent_whenFacadeDeletesCategory() throws Exception {
		// WHEN
		ResultActions expectedResult = mockMvc.perform(delete("/categories/{id}", mockCategoryId));

		// THEN
		expectedResult.andExpect(status().isNoContent());
		verify(categoryFacade).deleteCategory(mockCategoryId, mockUserId);
	}

	@Test
	void deleteCategory_returnsInternalServerError_whenFacadeFailsUnexpectedly() throws Exception {
		// GIVEN
		doThrow(new IllegalStateException())
			.when(categoryFacade)
			.deleteCategory(mockCategoryId, mockUserId);

		// WHEN
		ResultActions expectedResult = mockMvc.perform(delete("/categories/{id}", mockCategoryId));

		// THEN
		expectedResult
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.status").value(HttpStatus.INTERNAL_SERVER_ERROR.value()))
			.andExpect(jsonPath("$.errors[0].message").value(UNEXPECTED_ERROR_MESSAGE));
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
