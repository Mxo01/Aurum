package com.backend.aurum.infrastructure.mcp;

import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Enforces the read/write split on MCP tools when scopes are configured.
 *
 * <p>Tools whose name starts with {@code get} are read-only; everything else is a
 * write. The default is deliberately fail-closed for future tools: a new tool is
 * treated as a write unless it is named as a read.
 */
@Component
public class McpToolAuthorization {

	private final McpScopes scopes;

	public McpToolAuthorization(McpScopes scopes) {
		this.scopes = scopes;
	}

	public void check(String toolName) {
		if (!scopes.enforced()) {
			return;
		}

		Set<String> required = isReadTool(toolName) ? scopes.read() : scopes.write();
		if (!hasAnyScope(required)) {
			throw new AccessDeniedException(
				"Missing MCP scope for tool " + toolName + ": " + String.join(", ", required)
			);
		}
	}

	private boolean hasAnyScope(Set<String> required) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return false;
		}

		return authentication
			.getAuthorities()
			.stream()
			.map(GrantedAuthority::getAuthority)
			.anyMatch(authority ->
				required.stream().anyMatch(scope -> authority.equals("SCOPE_" + scope))
			);
	}

	static boolean isReadTool(String toolName) {
		return toolName != null && toolName.startsWith("get");
	}
}
