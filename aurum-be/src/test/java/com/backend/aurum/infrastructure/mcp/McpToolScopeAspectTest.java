package com.backend.aurum.infrastructure.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class McpToolScopeAspectTest {

	public static class ToolBean {

		@McpTool(description = "read")
		public String getAssets() {
			return "ok";
		}

		@McpTool(description = "write")
		public String addAsset() {
			return "ok";
		}
	}

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void allowsReadToolsWithTheReadScope() {
		ToolBean testSubject = proxy(new McpScopes("mcp:read", "mcp:write"));
		authenticateWith("SCOPE_mcp:read");

		assertThat(testSubject.getAssets()).isEqualTo("ok");
	}

	@Test
	void rejectsReadToolsWithOnlyTheWriteScope() {
		ToolBean testSubject = proxy(new McpScopes("mcp:read", "mcp:write"));
		authenticateWith("SCOPE_mcp:write");

		assertThatThrownBy(testSubject::getAssets).isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void allowsWriteToolsWithTheWriteScope() {
		ToolBean testSubject = proxy(new McpScopes("mcp:read", "mcp:write"));
		authenticateWith("SCOPE_mcp:write");

		assertThat(testSubject.addAsset()).isEqualTo("ok");
	}

	@Test
	void rejectsWriteToolsWithOnlyTheReadScope() {
		ToolBean testSubject = proxy(new McpScopes("mcp:read", "mcp:write"));
		authenticateWith("SCOPE_mcp:read");

		assertThatThrownBy(testSubject::addAsset).isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void allowsEverythingWhenScopesAreNotConfigured() {
		ToolBean testSubject = proxy(new McpScopes("", ""));

		assertThat(testSubject.getAssets()).isEqualTo("ok");
		assertThat(testSubject.addAsset()).isEqualTo("ok");
	}

	private ToolBean proxy(McpScopes scopes) {
		AspectJProxyFactory factory = new AspectJProxyFactory(new ToolBean());
		factory.addAspect(new McpToolScopeAspect(new McpToolAuthorization(scopes)));
		return factory.getProxy();
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
