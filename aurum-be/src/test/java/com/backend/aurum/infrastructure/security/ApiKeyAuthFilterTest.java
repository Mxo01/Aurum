package com.backend.aurum.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.mcp.service.ApiKeyService;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.model.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ApiKeyAuthFilterTest {

	@Mock
	private ApiKeyService apiKeyService;

	@Mock
	private McpSessionRegistry mcpSessionRegistry;

	@InjectMocks
	private ApiKeyAuthFilter testSubject;

	@Test
	void shouldNotFilter_returnsTrueForNonMcpUri() {
		// GIVEN
		MockHttpServletRequest mockRequest = new MockHttpServletRequest();
		mockRequest.setRequestURI("/assets");

		// WHEN
		boolean expectedResult = testSubject.shouldNotFilter(mockRequest);

		// THEN
		assertThat(expectedResult).isTrue();
	}

	@Test
	void shouldNotFilter_returnsFalseForSseUri() {
		// GIVEN
		MockHttpServletRequest mockRequest = new MockHttpServletRequest();
		mockRequest.setRequestURI("/sse");

		// WHEN
		boolean expectedResult = testSubject.shouldNotFilter(mockRequest);

		// THEN
		assertThat(expectedResult).isFalse();
	}

	@Test
	void shouldNotFilter_returnsFalseForMcpMessageUri() {
		// GIVEN
		MockHttpServletRequest mockRequest = new MockHttpServletRequest();
		mockRequest.setRequestURI("/mcp/message");

		// WHEN
		boolean expectedResult = testSubject.shouldNotFilter(mockRequest);

		// THEN
		assertThat(expectedResult).isFalse();
	}

	@Test
	void doFilterInternal_sse_returns401WhenKeyMissing() throws Exception {
		// GIVEN
		MockHttpServletRequest mockRequest = new MockHttpServletRequest();
		mockRequest.setRequestURI("/sse");
		MockHttpServletResponse mockResponse = new MockHttpServletResponse();
		FilterChain mockChain = mock(FilterChain.class);

		// WHEN
		testSubject.doFilterInternal(mockRequest, mockResponse, mockChain);

		// THEN
		assertThat(mockResponse.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
	}

	@Test
	void doFilterInternal_sse_returns401WhenKeyInvalid() throws Exception {
		// GIVEN
		MockHttpServletRequest mockRequest = new MockHttpServletRequest();
		mockRequest.setRequestURI("/sse");
		mockRequest.setParameter("key", "invalid_key");
		MockHttpServletResponse mockResponse = new MockHttpServletResponse();
		FilterChain mockChain = mock(FilterChain.class);
		when(apiKeyService.resolveUser("invalid_key")).thenReturn(Optional.empty());

		// WHEN
		testSubject.doFilterInternal(mockRequest, mockResponse, mockChain);

		// THEN
		assertThat(mockResponse.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
	}

	@Test
	void doFilterInternal_sse_authenticatesAndProceedsWhenKeyValid() throws Exception {
		// GIVEN
		MockHttpServletRequest mockRequest = new MockHttpServletRequest();
		mockRequest.setRequestURI("/sse");
		mockRequest.setParameter("key", "aurum_validkey");
		MockHttpServletResponse mockResponse = new MockHttpServletResponse();
		FilterChain mockChain = mock(FilterChain.class);
		User mockUser = Instancio.create(User.class);
		when(apiKeyService.resolveUser("aurum_validkey")).thenReturn(Optional.of(mockUser));

		// WHEN
		testSubject.doFilterInternal(mockRequest, mockResponse, mockChain);

		// THEN
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
		UserPrincipal expectedPrincipal = (UserPrincipal) SecurityContextHolder.getContext()
			.getAuthentication()
			.getPrincipal();
		assertThat(expectedPrincipal.user()).isEqualTo(mockUser);
		verify(mockChain).doFilter(
			org.mockito.ArgumentMatchers.notNull(),
			org.mockito.ArgumentMatchers.notNull()
		);
		SecurityContextHolder.clearContext();
	}

	@Test
	void doFilterInternal_mcpMessage_returns401WhenKeyMissingAndNoSession() throws Exception {
		// GIVEN
		MockHttpServletRequest mockRequest = new MockHttpServletRequest();
		mockRequest.setRequestURI("/mcp/message");
		MockHttpServletResponse mockResponse = new MockHttpServletResponse();
		FilterChain mockChain = mock(FilterChain.class);

		// WHEN
		testSubject.doFilterInternal(mockRequest, mockResponse, mockChain);

		// THEN
		assertThat(mockResponse.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
	}

	@Test
	void doFilterInternal_mcpMessage_authenticatesViaSessionWhenSessionExists() throws Exception {
		// GIVEN
		MockHttpServletRequest mockRequest = new MockHttpServletRequest();
		mockRequest.setRequestURI("/mcp/message");
		mockRequest.setParameter("sessionId", "session-123");
		MockHttpServletResponse mockResponse = new MockHttpServletResponse();
		FilterChain mockChain = mock(FilterChain.class);
		User mockUser = Instancio.create(User.class);
		when(mcpSessionRegistry.getUser("session-123")).thenReturn(Optional.of(mockUser));

		// WHEN
		testSubject.doFilterInternal(mockRequest, mockResponse, mockChain);

		// THEN
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
		UserPrincipal expectedPrincipal = (UserPrincipal) SecurityContextHolder.getContext()
			.getAuthentication()
			.getPrincipal();
		assertThat(expectedPrincipal.user()).isEqualTo(mockUser);
		verify(mockChain).doFilter(mockRequest, mockResponse);
		SecurityContextHolder.clearContext();
	}

	@Test
	void doFilterInternal_mcpMessage_fallsBackToKeyAuthWhenSessionNotFound() throws Exception {
		// GIVEN
		MockHttpServletRequest mockRequest = new MockHttpServletRequest();
		mockRequest.setRequestURI("/mcp/message");
		mockRequest.setParameter("sessionId", "expired-session");
		mockRequest.setParameter("key", "aurum_validkey");
		MockHttpServletResponse mockResponse = new MockHttpServletResponse();
		FilterChain mockChain = mock(FilterChain.class);
		User mockUser = Instancio.create(User.class);
		when(mcpSessionRegistry.getUser("expired-session")).thenReturn(Optional.empty());
		when(apiKeyService.resolveUser("aurum_validkey")).thenReturn(Optional.of(mockUser));

		// WHEN
		testSubject.doFilterInternal(mockRequest, mockResponse, mockChain);

		// THEN
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
		verify(mockChain).doFilter(
			org.mockito.ArgumentMatchers.notNull(),
			org.mockito.ArgumentMatchers.notNull()
		);
		SecurityContextHolder.clearContext();
	}

	@Test
	void doFilterInternal_mcpMessage_returns401WhenKeyInvalid() throws Exception {
		// GIVEN
		MockHttpServletRequest mockRequest = new MockHttpServletRequest();
		mockRequest.setRequestURI("/mcp/message");
		mockRequest.setParameter("key", "invalid_key");
		MockHttpServletResponse mockResponse = new MockHttpServletResponse();
		FilterChain mockChain = mock(FilterChain.class);
		when(apiKeyService.resolveUser("invalid_key")).thenReturn(Optional.empty());

		// WHEN
		testSubject.doFilterInternal(mockRequest, mockResponse, mockChain);

		// THEN
		assertThat(mockResponse.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
	}
}
