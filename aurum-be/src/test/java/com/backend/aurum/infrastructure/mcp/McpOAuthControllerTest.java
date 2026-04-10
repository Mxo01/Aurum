package com.backend.aurum.infrastructure.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class McpOAuthControllerTest {

	@Mock
	private HttpServletRequest request;

	@Mock
	private HttpServletResponse response;

	@InjectMocks
	private McpOAuthController testSubject;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(testSubject, "auth0ClientId", "test-client-id");
		ReflectionTestUtils.setField(testSubject, "resourceUrl", "http://localhost:8080");
		ReflectionTestUtils.setField(testSubject, "issuerUri", "https://auth.test.com/");
		ReflectionTestUtils.setField(testSubject, "audience", "https://api.test.com");
	}

	@Test
	void authorize_replacesRedirectUriWithBackendCallbackUrl() throws Exception {
		// GIVEN
		when(request.getParameter("state")).thenReturn("test-state");
		when(request.getParameter("redirect_uri")).thenReturn("http://localhost:55768/callback");
		when(request.getParameterMap()).thenReturn(
			Map.of(
				"response_type",
				new String[] { "code" },
				"client_id",
				new String[] { "test-client-id" },
				"redirect_uri",
				new String[] { "http://localhost:55768/callback" },
				"state",
				new String[] { "test-state" }
			)
		);

		// WHEN
		testSubject.authorize(request, response);

		// THEN
		verify(response).sendRedirect(
			contains("redirect_uri=http%3A%2F%2Flocalhost%3A8080%2Foauth%2Fcallback")
		);
	}

	@Test
	void authorize_injectsAudienceParameter() throws Exception {
		// GIVEN
		when(request.getParameter("state")).thenReturn("test-state");
		when(request.getParameter("redirect_uri")).thenReturn("http://localhost:55768/callback");
		when(request.getParameterMap()).thenReturn(Map.of("state", new String[] { "test-state" }));

		// WHEN
		testSubject.authorize(request, response);

		// THEN
		verify(response).sendRedirect(contains("audience=https%3A%2F%2Fapi.test.com"));
	}

	@Test
	void callback_forwardsCodeToOriginalRedirectUri() throws Exception {
		// GIVEN — simulate a prior authorize call that stored the pending callback
		when(request.getParameter("state")).thenReturn("stored-state");
		when(request.getParameter("redirect_uri")).thenReturn("http://localhost:55768/callback");
		when(request.getParameterMap()).thenReturn(
			Map.of(
				"state",
				new String[] { "stored-state" },
				"redirect_uri",
				new String[] { "http://localhost:55768/callback" }
			)
		);
		testSubject.authorize(request, response);
		clearInvocations(response);

		// WHEN
		testSubject.callback("auth-code-123", null, null, "stored-state", response);

		// THEN
		verify(response).sendRedirect(contains("http://localhost:55768/callback"));
		verify(response).sendRedirect(contains("code=auth-code-123"));
		verify(response).sendRedirect(contains("state=stored-state"));
	}

	@Test
	void callback_returnsErrorForUnknownState() throws Exception {
		// WHEN
		testSubject.callback(null, "access_denied", null, "unknown-state", response);

		// THEN
		verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown or expired state");
	}

	@Test
	void register_returnsPreRegisteredClientId() {
		// GIVEN
		Map<String, Object> stubbedBody = Map.of(
			"client_name",
			"Claude Code",
			"redirect_uris",
			List.of("http://localhost:55768/callback"),
			"grant_types",
			List.of("authorization_code")
		);

		// WHEN
		Map<String, Object> expectedResult = testSubject.register(stubbedBody);

		// THEN
		assertThat(expectedResult.get("client_id")).isEqualTo("test-client-id");
	}

	@Test
	void register_echoesRedirectUrisFromRequest() {
		// GIVEN
		List<String> stubbedRedirectUris = List.of("http://localhost:55768/callback");
		Map<String, Object> stubbedBody = Map.of("redirect_uris", stubbedRedirectUris);

		// WHEN
		Map<String, Object> expectedResult = testSubject.register(stubbedBody);

		// THEN
		assertThat(expectedResult.get("redirect_uris")).isEqualTo(stubbedRedirectUris);
	}

	@Test
	void register_returnsEmptyRedirectUrisWhenNotProvided() {
		// WHEN
		Map<String, Object> expectedResult = testSubject.register(Map.of());

		// THEN
		assertThat(expectedResult.get("redirect_uris")).isEqualTo(List.of());
	}
}
