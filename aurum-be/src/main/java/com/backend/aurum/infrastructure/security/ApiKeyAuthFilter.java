package com.backend.aurum.infrastructure.security;

import com.backend.aurum.domain.mcp.service.ApiKeyService;
import com.backend.aurum.domain.user.model.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

	private final ApiKeyService apiKeyService;

	@Override
	protected void doFilterInternal(HttpServletRequest request,
			HttpServletResponse response,
			FilterChain chain) throws ServletException, IOException {

		String plainKey = extractKey(request);

		if (plainKey == null) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing API key");
			return;
		}

		Optional<User> userOpt = apiKeyService.resolveUser(plainKey);
		if (userOpt.isEmpty()) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid API key");
			return;
		}

		UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
				userOpt.get(), null, List.of());
		SecurityContextHolder.getContext().setAuthentication(auth);

		chain.doFilter(new RedactedKeyRequest(request), response);
	}

	private String extractKey(HttpServletRequest request) {
		String header = request.getHeader("Authorization");
		if (header != null && header.startsWith("Bearer ")) {
			return header.substring("Bearer ".length()).trim();
		}
		return request.getParameter("key");
	}

	/** Wraps the request so any downstream logging sees key=REDACTED in the query string. */
	private static final class RedactedKeyRequest extends HttpServletRequestWrapper {
		RedactedKeyRequest(HttpServletRequest request) {
			super(request);
		}

		@Override
		public String getQueryString() {
			String qs = super.getQueryString();
			if (qs == null) return null;
			return qs.replaceAll("(?i)((?:^|&)key=)[^&]+", "$1REDACTED");
		}
	}
}
