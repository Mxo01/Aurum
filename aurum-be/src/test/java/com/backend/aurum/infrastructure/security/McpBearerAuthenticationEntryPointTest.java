package com.backend.aurum.infrastructure.security;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
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
	void setUp() throws Exception {
		ReflectionTestUtils.setField(testSubject, "resourceUrl", "https://api.test.com");
		when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
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
	void commence_sets401StatusWithJsonContentType() throws Exception {
		// WHEN
		testSubject.commence(request, response, new BadCredentialsException("test"));

		// THEN
		verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		verify(response).setContentType("application/json");
	}
}
