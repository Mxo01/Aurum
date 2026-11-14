package com.backend.aurum.infrastructure.mcp;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class McpToolAuthorizationTest {

	private static final String READ_SCOPE = "mcp:read";
	private static final String WRITE_SCOPE = "mcp:write";
	private static final String ASSET_READ_SCOPE = "mcp:assets:read";

	private static final String READ_AUTHORITY = "SCOPE_" + READ_SCOPE;
	private static final String WRITE_AUTHORITY = "SCOPE_" + WRITE_SCOPE;
	private static final String ASSET_READ_AUTHORITY = "SCOPE_" + ASSET_READ_SCOPE;
	private static final String ROLE_AUTHORITY = "ROLE_USER";

	private static final String READ_TOOL = "getAssets";
	private static final String WRITE_TOOL = "addAsset";
	private static final String UNKNOWN_TOOL = "rebalancePortfolio";
	private static final String NULL_TOOL = null;

	private static final String CLIENT_PRINCIPAL = "mcp-client";

	@Mock
	private McpScopes mcpScopes;

	@InjectMocks
	private McpToolAuthorization testSubject;

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("allowedToolCalls")
	void allowsTheToolCall(
		String scenario,
		List<String> readScopes,
		List<String> writeScopes,
		String toolName,
		List<String> authorities
	) {
		// GIVEN
		givenScopes(readScopes, writeScopes);
		givenAuthentication(authorities);

		// THEN
		assertThatCode(() -> testSubject.check(toolName)).doesNotThrowAnyException();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("deniedToolCalls")
	void deniesTheToolCall(
		String scenario,
		List<String> readScopes,
		List<String> writeScopes,
		String toolName,
		List<String> authorities,
		String expectedMessage
	) {
		// GIVEN
		givenScopes(readScopes, writeScopes);
		givenAuthentication(authorities);

		// THEN
		assertThatThrownBy(() -> testSubject.check(toolName))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessage(expectedMessage);
	}

	@Test
	void deniesAReadToolWhenTheAuthenticationIsNotAuthenticated() {
		// GIVEN
		givenScopes(List.of(READ_SCOPE), List.of(WRITE_SCOPE));
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken(CLIENT_PRINCIPAL, null)
		);

		// THEN
		assertThatThrownBy(() -> testSubject.check(READ_TOOL))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessage(missingScopeMessage(READ_TOOL, READ_SCOPE));
	}

	private static Stream<Arguments> allowedToolCalls() {
		return Stream.of(
			Arguments.of(
				"a read tool stays available when no scope is configured",
				List.of(),
				List.of(),
				READ_TOOL,
				null
			),
			Arguments.of(
				"a read tool is allowed with the configured read scope",
				List.of(READ_SCOPE),
				List.of(WRITE_SCOPE),
				READ_TOOL,
				List.of(READ_AUTHORITY)
			),
			Arguments.of(
				"the read scope is found among unrelated authorities",
				List.of(READ_SCOPE),
				List.of(WRITE_SCOPE),
				READ_TOOL,
				List.of(ROLE_AUTHORITY, READ_AUTHORITY)
			),
			Arguments.of(
				"a read tool accepts any one of the configured read scopes",
				List.of(READ_SCOPE, ASSET_READ_SCOPE),
				List.of(WRITE_SCOPE),
				READ_TOOL,
				List.of(ASSET_READ_AUTHORITY)
			),
			Arguments.of(
				"a write tool is allowed with the configured write scope",
				List.of(READ_SCOPE),
				List.of(WRITE_SCOPE),
				WRITE_TOOL,
				List.of(WRITE_AUTHORITY)
			),
			Arguments.of(
				"an unknown tool is treated as a write and allowed with the write scope",
				List.of(READ_SCOPE),
				List.of(WRITE_SCOPE),
				UNKNOWN_TOOL,
				List.of(WRITE_AUTHORITY)
			)
		);
	}

	private static Stream<Arguments> deniedToolCalls() {
		return Stream.of(
			Arguments.of(
				"a write tool is denied when no scope is configured",
				List.of(),
				List.of(),
				WRITE_TOOL,
				null,
				notConfiguredMessage(WRITE_TOOL)
			),
			Arguments.of(
				"a write tool is denied when no scope is configured even with a write authority",
				List.of(),
				List.of(),
				WRITE_TOOL,
				List.of(WRITE_AUTHORITY),
				notConfiguredMessage(WRITE_TOOL)
			),
			Arguments.of(
				"an unknown tool is denied when no scope is configured",
				List.of(),
				List.of(),
				UNKNOWN_TOOL,
				List.of(READ_AUTHORITY),
				notConfiguredMessage(UNKNOWN_TOOL)
			),
			Arguments.of(
				"a read tool is denied with only the write scope",
				List.of(READ_SCOPE),
				List.of(WRITE_SCOPE),
				READ_TOOL,
				List.of(WRITE_AUTHORITY),
				missingScopeMessage(READ_TOOL, READ_SCOPE)
			),
			Arguments.of(
				"a read tool is denied without any authority",
				List.of(READ_SCOPE),
				List.of(WRITE_SCOPE),
				READ_TOOL,
				List.of(),
				missingScopeMessage(READ_TOOL, READ_SCOPE)
			),
			Arguments.of(
				"a read tool is denied without an authentication",
				List.of(READ_SCOPE),
				List.of(WRITE_SCOPE),
				READ_TOOL,
				null,
				missingScopeMessage(READ_TOOL, READ_SCOPE)
			),
			Arguments.of(
				"a read tool is denied when none of the configured read scopes match",
				List.of(READ_SCOPE, ASSET_READ_SCOPE),
				List.of(WRITE_SCOPE),
				READ_TOOL,
				List.of(ROLE_AUTHORITY),
				missingScopeMessage(READ_TOOL, READ_SCOPE, ASSET_READ_SCOPE)
			),
			Arguments.of(
				"a write tool is denied with only the read scope",
				List.of(READ_SCOPE),
				List.of(WRITE_SCOPE),
				WRITE_TOOL,
				List.of(READ_AUTHORITY),
				missingScopeMessage(WRITE_TOOL, WRITE_SCOPE)
			),
			Arguments.of(
				"a write tool is denied without an authentication",
				List.of(READ_SCOPE),
				List.of(WRITE_SCOPE),
				WRITE_TOOL,
				null,
				missingScopeMessage(WRITE_TOOL, WRITE_SCOPE)
			),
			Arguments.of(
				"an unknown tool is denied with only the read scope",
				List.of(READ_SCOPE),
				List.of(WRITE_SCOPE),
				UNKNOWN_TOOL,
				List.of(READ_AUTHORITY),
				missingScopeMessage(UNKNOWN_TOOL, WRITE_SCOPE)
			),
			Arguments.of(
				"a null tool name is treated as a write and denied",
				List.of(READ_SCOPE),
				List.of(WRITE_SCOPE),
				NULL_TOOL,
				List.of(READ_AUTHORITY),
				missingScopeMessage(NULL_TOOL, WRITE_SCOPE)
			)
		);
	}

	private void givenScopes(List<String> readScopes, List<String> writeScopes) {
		lenient().when(mcpScopes.read()).thenReturn(orderedScopes(readScopes));
		lenient().when(mcpScopes.write()).thenReturn(orderedScopes(writeScopes));
	}

	private void givenAuthentication(List<String> authorities) {
		if (authorities == null) {
			return;
		}

		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken(
				CLIENT_PRINCIPAL,
				null,
				authorities.stream().map(SimpleGrantedAuthority::new).toList()
			)
		);
	}

	private static Set<String> orderedScopes(List<String> scopes) {
		return new LinkedHashSet<>(scopes);
	}

	private static String notConfiguredMessage(String toolName) {
		return McpScopes.WRITE_PROPERTY + " is not configured, so tool " + toolName + " is denied";
	}

	private static String missingScopeMessage(String toolName, String... requiredScopes) {
		return "Missing MCP scope for tool " + toolName + ": " + String.join(", ", requiredScopes);
	}
}
