package com.backend.aurum.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class McpBearerAuthenticationEntryPoint implements AuthenticationEntryPoint {

	@Value("${mcp.resource-url}")
	private String resourceUrl;

	@Override
	public void commence(
		HttpServletRequest request,
		HttpServletResponse response,
		AuthenticationException authException
	) throws IOException {
		String metadataUrl = resourceUrl + "/.well-known/oauth-protected-resource";
		response.setHeader(
			"WWW-Authenticate",
			"Bearer realm=\"aurum-mcp\", resource_metadata=\"" + metadataUrl + "\""
		);
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response
			.getWriter()
			.write("{\"error\":\"invalid_token\",\"error_description\":\"Bearer token required\"}");
	}
}
