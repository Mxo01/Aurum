package com.backend.aurum.infrastructure.security;

import static org.mockito.Mockito.verify;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class McpBearerAuthenticationEntryPointTest {

	@Mock
	private HttpServletRequest request;

	@Mock
	private HttpServletResponse response;

	@InjectMocks
	private McpBearerAuthenticationEntryPoint testSubject;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(testSubject, "resourceUrl", "https://api.test.com");
	}

	@Test
	void commence_setsWwwAuthenticateHeaderWithResourceMetadataUrl() throws Exception {
		// WHEN
		testSubject.commence(request, response, new BadCredentialsException("test"));

		// THEN
		verify(response).setHeader(
			"WWW-Authenticate",
			"Bearer realm=\"aurum-mcp\", resource_metadata=\"https://api.test.com/.well-known/oauth-protected-resource\""
		);
	}

	@Test
	void commence_sends401Unauthorized() throws Exception {
		// WHEN
		testSubject.commence(request, response, new BadCredentialsException("test"));

		// THEN
		verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
	}
}
