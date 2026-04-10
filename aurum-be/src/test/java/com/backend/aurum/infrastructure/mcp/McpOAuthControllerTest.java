package com.backend.aurum.infrastructure.mcp;

import static org.assertj.core.api.Assertions.assertThat;
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
		ReflectionTestUtils.setField(testSubject, "issuerUri", "https://auth.test.com/");
		ReflectionTestUtils.setField(testSubject, "audience", "https://api.test.com");
	}

	@Test
	void authorize_redirectsToAuth0AuthorizeWithAudienceInjected() throws Exception {
		// GIVEN
		when(request.getQueryString()).thenReturn(
			"client_id=test-client-id&response_type=code&redirect_uri=http://localhost:1234/callback"
		);

		// WHEN
		testSubject.authorize(request, response);

		// THEN
		verify(response).sendRedirect(
			"https://auth.test.com/authorize?client_id=test-client-id&response_type=code&redirect_uri=http://localhost:1234/callback&audience=https://api.test.com"
		);
	}

	@Test
	void register_returnsPreRegisteredClientId() {
		// GIVEN
		Map<String, Object> stubbedBody = Map.of(
			"client_name",
			"Claude Code",
			"redirect_uris",
			List.of("http://127.0.0.1:54321/oauth/callback"),
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
		List<String> stubbedRedirectUris = List.of("http://127.0.0.1:54321/oauth/callback");
		Map<String, Object> stubbedBody = Map.of("redirect_uris", stubbedRedirectUris);

		// WHEN
		Map<String, Object> expectedResult = testSubject.register(stubbedBody);

		// THEN
		assertThat(expectedResult.get("redirect_uris")).isEqualTo(stubbedRedirectUris);
	}

	@Test
	void register_returnsEmptyRedirectUrisWhenNotProvided() {
		// GIVEN
		Map<String, Object> stubbedBody = Map.of("client_name", "Some Client");

		// WHEN
		Map<String, Object> expectedResult = testSubject.register(stubbedBody);

		// THEN
		assertThat(expectedResult.get("redirect_uris")).isEqualTo(List.of());
	}

	@Test
	void register_setsTokenEndpointAuthMethodToNone() {
		// GIVEN
		Map<String, Object> stubbedBody = Map.of();

		// WHEN
		Map<String, Object> expectedResult = testSubject.register(stubbedBody);

		// THEN
		assertThat(expectedResult.get("token_endpoint_auth_method")).isEqualTo("none");
	}
}
