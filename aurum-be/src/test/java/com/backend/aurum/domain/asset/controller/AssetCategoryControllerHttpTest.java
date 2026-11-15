package com.backend.aurum.domain.asset.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.backend.aurum.domain.asset.facade.AssetCategoryFacade;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.model.UserPrincipal;
import com.backend.aurum.infrastructure.exception.GlobalExceptionHandler;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AssetCategoryControllerHttpTest {

	private static final String CATEGORY_IN_USE_REASON =
		"Category still holds 2 asset(s); move or delete them first";
	private static final String ACCESS_DENIED_REASON = "Cannot delete another user's category";
	private static final String UNEXPECTED_ERROR_MESSAGE = "An unexpected error occurred";

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
	void deleteCategory_returnsConflictWithReason_whenCategoryStillHoldsAssets() throws Exception {
		// GIVEN
		doThrow(new ResponseStatusException(HttpStatus.CONFLICT, CATEGORY_IN_USE_REASON))
			.when(categoryFacade)
			.deleteCategory(mockCategoryId, mockUserId);

		// WHEN
		ResultActions expectedResult = mockMvc.perform(delete("/categories/{id}", mockCategoryId));

		// THEN
		expectedResult
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.status").value(HttpStatus.CONFLICT.value()))
			.andExpect(jsonPath("$.error").value(HttpStatus.CONFLICT.getReasonPhrase()))
			.andExpect(jsonPath("$.errors[0].message").value(CATEGORY_IN_USE_REASON))
			.andExpect(jsonPath("$.message").doesNotExist())
			.andExpect(jsonPath("$.timestamp").doesNotExist());
	}

	@Test
	void deleteCategory_returnsForbiddenWithReason_whenFacadeDeniesAccess() throws Exception {
		// GIVEN
		doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, ACCESS_DENIED_REASON))
			.when(categoryFacade)
			.deleteCategory(mockCategoryId, mockUserId);

		// WHEN
		ResultActions expectedResult = mockMvc.perform(delete("/categories/{id}", mockCategoryId));

		// THEN
		expectedResult
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.status").value(HttpStatus.FORBIDDEN.value()))
			.andExpect(jsonPath("$.errors[0].message").value(ACCESS_DENIED_REASON));
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
