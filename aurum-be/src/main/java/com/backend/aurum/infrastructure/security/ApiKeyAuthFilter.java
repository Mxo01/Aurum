package com.backend.aurum.infrastructure.security;

import com.backend.aurum.domain.mcp.service.ApiKeyService;
import com.backend.aurum.domain.user.model.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

	private final ApiKeyService apiKeyService;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String uri = request.getRequestURI();
		return !uri.startsWith("/mcp/") && !uri.equals("/sse");
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain chain
	) throws ServletException, IOException {
		String header = request.getHeader("Authorization");

		if (header == null || !header.startsWith("Bearer ")) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing API key");
			return;
		}

		String plainKey = header.substring("Bearer ".length()).trim();

		Optional<User> userOpt = apiKeyService.resolveUser(plainKey);
		if (userOpt.isEmpty()) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid API key");
			return;
		}

		UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
			userOpt.get(),
			null,
			List.of()
		);
		SecurityContextHolder.getContext().setAuthentication(auth);

		chain.doFilter(request, response);
	}
}
