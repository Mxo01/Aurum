package com.backend.aurum.infrastructure.mcp;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Scopes advertised by the MCP resource and enforced on tool calls.
 *
 * <p>Read and write scopes are configured together, so a half-configured deployment
 * cannot leave a category unsupervised by accident. When both lists are empty the server
 * still starts — local development and CI must not be bricked — but the guard denies every
 * write: a missing configuration closes the write surface instead of opening it. The
 * advertised scope list stays empty in that case, so discovery never promises a scope that
 * would not be enforced.
 */
@Slf4j
@Component
public class McpScopes {

	/** Shared by the startup warning and the tool denial message. */
	static final String READ_PROPERTY = "mcp.scopes.read";
	static final String WRITE_PROPERTY = "mcp.scopes.write";

	/** RFC 6749 scope-token: printable ASCII except space, quote and backslash. */
	private static final Pattern SCOPE_TOKEN = Pattern.compile("[\\x21\\x23-\\x5B\\x5D-\\x7E]+");

	private final Set<String> read;
	private final Set<String> write;

	public McpScopes(
		@Value("${mcp.scopes.read:}") String read,
		@Value("${mcp.scopes.write:}") String write
	) {
		this.read = parse(READ_PROPERTY, read);
		this.write = parse(WRITE_PROPERTY, write);

		if (this.read.isEmpty() != this.write.isEmpty()) {
			throw new IllegalStateException(
				READ_PROPERTY + " and " + WRITE_PROPERTY + " must be configured together"
			);
		}

		warnIfUnconfigured();
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

	private void warnIfUnconfigured() {
		if (enforced()) {
			return;
		}

		log.warn(
			"{} and {} are not set: MCP write tools are denied until both are configured, read tools stay available",
			READ_PROPERTY,
			WRITE_PROPERTY
		);
	}

	public Set<String> read() {
		return read;
	}

	public Set<String> write() {
		return write;
	}

	/**
	 * Whether both scope lists are configured.
	 *
	 * <p>This is never a licence to skip the tool check: writes are denied when it is
	 * false, so callers must not use it to bypass authorization.
	 */
	public boolean enforced() {
		return !all().isEmpty();
	}

	public List<String> all() {
		return Stream.concat(read.stream(), write.stream()).distinct().sorted().toList();
	}
}
