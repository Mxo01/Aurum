package com.backend.aurum.infrastructure.security;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.aurum.infrastructure.mcp.McpScopes;
import com.backend.aurum.infrastructure.mcp.McpSettings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

@ExtendWith(MockitoExtension.class)
class McpBearerAuthenticationEntryPointTest {

	@Mock
	private HttpServletRequest request;

	@Mock
	private HttpServletResponse response;

	@Mock
	private McpSettings settings;

	@Mock
	private McpScopes scopes;

	@InjectMocks
	private McpBearerAuthenticationEntryPoint testSubject;

	@BeforeEach
	void setUp() throws Exception {
		when(settings.metadataUrl()).thenReturn(
			"https://api.test.com/.well-known/oauth-protected-resource/mcp"
		);
		when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
	}

	@Test
	void commence_setsWwwAuthenticateHeaderWithResourceMetadataUrl() throws Exception {
		// GIVEN
		when(scopes.all()).thenReturn(List.of());

		// WHEN
		testSubject.commence(request, response, new BadCredentialsException("test"));

		// THEN
		verify(response).setHeader(
			"WWW-Authenticate",
			"Bearer realm=\"aurum-mcp\", resource_metadata=\"https://api.test.com/.well-known/oauth-protected-resource/mcp\""
		);
	}

	@Test
	void commence_advertisesScopesWhenConfigured() throws Exception {
		// GIVEN
		when(scopes.all()).thenReturn(List.of("mcp:read", "mcp:write"));

		// WHEN
		testSubject.commence(request, response, new BadCredentialsException("test"));

		// THEN
		verify(response).setHeader(
			"WWW-Authenticate",
			"Bearer realm=\"aurum-mcp\", resource_metadata=\"https://api.test.com/.well-known/oauth-protected-resource/mcp\", scope=\"mcp:read mcp:write\""
		);
	}

	@Test
	void commence_sets401StatusWithJsonContentType() throws Exception {
		// GIVEN
		when(scopes.all()).thenReturn(List.of());

		// WHEN
		testSubject.commence(request, response, new BadCredentialsException("test"));

		// THEN
		verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		verify(response).setContentType("application/json");
	}
}
