package com.backend.aurum.infrastructure.mcp;

import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Enforces the read/write split on MCP tools.
 *
 * <p>Tools whose name starts with {@code get} are read-only; everything else is a write,
 * including tools that do not exist yet. A configured category requires one of its scopes
 * on the token. An unconfigured write category denies every write, so losing the
 * configuration can never open the write surface; an unconfigured read category leaves
 * reads available, because they cannot mutate anything.
 */
@Component
public class McpToolAuthorization {

	private final McpScopes scopes;

	public McpToolAuthorization(McpScopes scopes) {
		this.scopes = scopes;
	}

	public void check(String toolName) {
		boolean readTool = isReadTool(toolName);
		Set<String> required = readTool ? scopes.read() : scopes.write();

		if (required.isEmpty()) {
			if (readTool) {
				return;
			}
			throw new AccessDeniedException(
				McpScopes.WRITE_PROPERTY + " is not configured, so tool " + toolName + " is denied"
			);
		}

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
