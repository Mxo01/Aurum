package com.backend.aurum.infrastructure.mcp;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rejects browser requests from origins the MCP server does not trust.
 *
 * <p>MCP clients are not browsers and send no Origin header. Accepting an arbitrary
 * Origin is what makes DNS-rebinding and cross-site requests possible, so when an
 * Origin is present it must match the configured allowlist exactly. An empty
 * allowlist therefore blocks every browser origin and leaves native clients alone.
 */
public final class McpOriginValidationFilter extends OncePerRequestFilter {

	static final String MCP_PATH = "/mcp";

	private final Set<String> allowedOrigins;

	public McpOriginValidationFilter(String allowedOrigins) {
		this.allowedOrigins = parse(allowedOrigins);
	}

	private static Set<String> parse(String raw) {
		if (raw == null || raw.isBlank()) {
			return Set.of();
		}

		Set<String> origins = new LinkedHashSet<>();
		Arrays.stream(raw.split(","))
			.map(String::trim)
			.filter(origin -> !origin.isEmpty())
			.map(origin -> origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin)
			.forEach(origins::add);
		return Set.copyOf(origins);
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !MCP_PATH.equals(request.getRequestURI());
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		String origin = request.getHeader(HttpHeaders.ORIGIN);
		if (origin == null || allowedOrigins.contains(origin)) {
			filterChain.doFilter(request, response);
			return;
		}

		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		response
			.getWriter()
			.write("{\"error\":\"invalid_origin\",\"error_description\":\"Origin not allowed\"}");
	}
}
