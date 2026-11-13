package com.backend.aurum.infrastructure.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class McpToolAuthorizationTest {

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void doesNothingWhenScopesAreNotConfigured() {
		McpToolAuthorization testSubject = new McpToolAuthorization(new McpScopes("", ""));

		assertThatCode(() -> testSubject.check("addAsset")).doesNotThrowAnyException();
	}

	@Test
	void allowsReadToolsWithTheReadScope() {
		McpToolAuthorization testSubject = new McpToolAuthorization(
			new McpScopes("mcp:read", "mcp:write")
		);
		authenticateWith("SCOPE_mcp:read");

		assertThatCode(() -> testSubject.check("getAssets")).doesNotThrowAnyException();
	}

	@Test
	void rejectsReadToolsWithoutTheReadScope() {
		McpToolAuthorization testSubject = new McpToolAuthorization(
			new McpScopes("mcp:read", "mcp:write")
		);
		authenticateWith("SCOPE_mcp:write");

		assertThatThrownBy(() -> testSubject.check("getAssets")).isInstanceOf(
			AccessDeniedException.class
		);
	}

	@Test
	void allowsWriteToolsWithTheWriteScope() {
		McpToolAuthorization testSubject = new McpToolAuthorization(
			new McpScopes("mcp:read", "mcp:write")
		);
		authenticateWith("SCOPE_mcp:write");

		assertThatCode(() -> testSubject.check("addAsset")).doesNotThrowAnyException();
	}

	@Test
	void rejectsWriteToolsWithOnlyTheReadScope() {
		McpToolAuthorization testSubject = new McpToolAuthorization(
			new McpScopes("mcp:read", "mcp:write")
		);
		authenticateWith("SCOPE_mcp:read");

		assertThatThrownBy(() -> testSubject.check("addAsset")).isInstanceOf(
			AccessDeniedException.class
		);
	}

	@Test
	void treatsUnknownToolNamesAsWrites() {
		McpToolAuthorization testSubject = new McpToolAuthorization(
			new McpScopes("mcp:read", "mcp:write")
		);
		authenticateWith("SCOPE_mcp:read");

		assertThatThrownBy(() -> testSubject.check("deleteAsset")).isInstanceOf(
			AccessDeniedException.class
		);
	}

	@Test
	void classifiesOnlyGetPrefixAsRead() {
		assertThat(McpToolAuthorization.isReadTool("getAssets")).isTrue();
		assertThat(McpToolAuthorization.isReadTool("addAsset")).isFalse();
		assertThat(McpToolAuthorization.isReadTool("bulkAddAssets")).isFalse();
		assertThat(McpToolAuthorization.isReadTool(null)).isFalse();
	}

	private void authenticateWith(String authority) {
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken(
				"user",
				null,
				List.of(new SimpleGrantedAuthority(authority))
			)
		);
	}
}
