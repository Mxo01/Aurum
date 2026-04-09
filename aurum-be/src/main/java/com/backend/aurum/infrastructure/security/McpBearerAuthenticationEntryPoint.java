package com.backend.aurum.infrastructure.security;

import jakarta.servlet.ServletException;
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
	) throws IOException, ServletException {
		String metadataUrl = resourceUrl + "/.well-known/oauth-protected-resource";
		response.setHeader(
			"WWW-Authenticate",
			"Bearer realm=\"aurum-mcp\", resource_metadata=\"" + metadataUrl + "\""
		);
		response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
	}
}
