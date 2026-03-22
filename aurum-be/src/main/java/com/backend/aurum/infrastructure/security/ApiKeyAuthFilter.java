package com.backend.aurum.infrastructure.security;

import com.backend.aurum.domain.mcp.service.ApiKeyService;
import com.backend.aurum.domain.user.model.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

	private final ApiKeyService apiKeyService;
	private final McpSessionRegistry mcpSessionRegistry;

	@Override
	protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
		String uri = request.getRequestURI();
		return !uri.equals("/mcp/message") && !uri.equals("/sse");
	}

	@Override
	protected void doFilterInternal(
		@NonNull HttpServletRequest request,
		@NonNull HttpServletResponse response,
		@NonNull FilterChain chain
	) throws ServletException, IOException {
		String uri = request.getRequestURI();

		if (uri.equals("/sse")) {
			handleSse(request, response, chain);
		} else {
			handleMcpMessage(request, response, chain);
		}
	}

	private void handleSse(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain chain
	) throws ServletException, IOException {
		String plainKey = request.getParameter("key");

		if (plainKey == null) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing API key");
			return;
		}

		Optional<User> userOpt = apiKeyService.resolveUser(plainKey);
		if (userOpt.isEmpty()) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid API key");
			return;
		}

		User user = userOpt.get();
		setAuthentication(user);

		SessionCapturingResponseWrapper wrapper = new SessionCapturingResponseWrapper(
			response,
			user,
			mcpSessionRegistry
		);
		chain.doFilter(new RedactedKeyRequest(request), wrapper);
	}

	private void handleMcpMessage(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain chain
	) throws ServletException, IOException {
		// Prefer sessionId-based auth (key was already validated at SSE connect time)
		String sessionId = request.getParameter("sessionId");
		if (sessionId != null) {
			Optional<User> userOpt = mcpSessionRegistry.getUser(sessionId);
			if (userOpt.isPresent()) {
				setAuthentication(userOpt.get());
				chain.doFilter(request, response);
				return;
			}
		}

		// Fallback: key-based auth
		String plainKey = request.getParameter("key");
		if (plainKey == null) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing API key");
			return;
		}

		Optional<User> userOpt = apiKeyService.resolveUser(plainKey);
		if (userOpt.isEmpty()) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid API key");
			return;
		}

		setAuthentication(userOpt.get());
		chain.doFilter(new RedactedKeyRequest(request), response);
	}

	private void setAuthentication(User user) {
		UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
			user,
			null,
			List.of()
		);
		SecurityContextHolder.getContext().setAuthentication(auth);
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

	/**
	 * Intercepts the SSE response stream to extract the MCP sessionId from the
	 * "event:endpoint" SSE event, then registers it in the McpSessionRegistry.
	 */
	private static final class SessionCapturingResponseWrapper extends HttpServletResponseWrapper {

		private final User user;
		private final McpSessionRegistry registry;
		private SessionCapturingOutputStream capturingStream;

		SessionCapturingResponseWrapper(
			HttpServletResponse response,
			User user,
			McpSessionRegistry registry
		) {
			super(response);
			this.user = user;
			this.registry = registry;
		}

		@Override
		public ServletOutputStream getOutputStream() throws IOException {
			if (capturingStream == null) {
				capturingStream = new SessionCapturingOutputStream(super.getOutputStream(), user, registry);
			}
			return capturingStream;
		}
	}

	private static final class SessionCapturingOutputStream extends ServletOutputStream {

		private static final Pattern SESSION_ID_PATTERN = Pattern.compile(
			"data:/mcp/message\\?sessionId=([\\w-]+)"
		);

		private final ServletOutputStream delegate;
		private final User user;
		private final McpSessionRegistry registry;
		private boolean sessionRegistered = false;

		SessionCapturingOutputStream(
			ServletOutputStream delegate,
			User user,
			McpSessionRegistry registry
		) {
			this.delegate = delegate;
			this.user = user;
			this.registry = registry;
		}

		@Override
		public void write(int b) throws IOException {
			delegate.write(b);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			if (!sessionRegistered) {
				String chunk = new String(b, off, len, StandardCharsets.UTF_8);
				Matcher matcher = SESSION_ID_PATTERN.matcher(chunk);
				if (matcher.find()) {
					registry.register(matcher.group(1), user);
					sessionRegistered = true;
				}
			}
			delegate.write(b, off, len);
		}

		@Override
		public void flush() throws IOException {
			delegate.flush();
		}

		@Override
		public void close() throws IOException {
			delegate.close();
		}

		@Override
		public boolean isReady() {
			return delegate.isReady();
		}

		@Override
		public void setWriteListener(WriteListener writeListener) {
			delegate.setWriteListener(writeListener);
		}
	}
}
