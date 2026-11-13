package com.backend.aurum.infrastructure.mcp;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Scopes advertised by the MCP resource and enforced on tool calls.
 *
 * <p>Scopes are optional: when neither list is configured the server only enforces
 * audience binding. When they are configured they must be configured together, so a
 * half-configured deployment cannot leave the write surface open by accident.
 */
@Component
public class McpScopes {

	/** RFC 6749 scope-token: printable ASCII except space, quote and backslash. */
	private static final Pattern SCOPE_TOKEN = Pattern.compile("[\\x21\\x23-\\x5B\\x5D-\\x7E]+");

	private final Set<String> read;
	private final Set<String> write;

	public McpScopes(
		@Value("${mcp.scopes.read:}") String read,
		@Value("${mcp.scopes.write:}") String write
	) {
		this.read = parse("mcp.scopes.read", read);
		this.write = parse("mcp.scopes.write", write);

		if (this.read.isEmpty() != this.write.isEmpty()) {
			throw new IllegalStateException(
				"mcp.scopes.read and mcp.scopes.write must be configured together"
			);
		}
	}

	private static Set<String> parse(String property, String raw) {
		if (raw == null || raw.isBlank()) {
			return Set.of();
		}

		Set<String> scopes = new LinkedHashSet<>();
		for (String candidate : raw.split(",")) {
			String scope = candidate.trim();
			if (scope.isEmpty()) {
				continue;
			}
			if (!SCOPE_TOKEN.matcher(scope).matches()) {
				throw new IllegalStateException(
					property + " contains an invalid OAuth scope token: " + scope
				);
			}
			scopes.add(scope);
		}

		if (scopes.isEmpty()) {
			throw new IllegalStateException(property + " must contain at least one scope");
		}

		return Set.copyOf(scopes);
	}

	public Set<String> read() {
		return read;
	}

	public Set<String> write() {
		return write;
	}

	public boolean enforced() {
		return !read.isEmpty();
	}

	public List<String> all() {
		return Stream.concat(read.stream(), write.stream()).distinct().sorted().toList();
	}
}
