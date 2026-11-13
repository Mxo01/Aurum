package com.backend.aurum.infrastructure.security;

import com.backend.aurum.infrastructure.mcp.McpScopes;
import com.backend.aurum.infrastructure.mcp.McpSettings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class McpBearerAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final McpSettings settings;
	private final McpScopes scopes;

	public McpBearerAuthenticationEntryPoint(McpSettings settings, McpScopes scopes) {
		this.settings = settings;
		this.scopes = scopes;
	}

	@Override
	public void commence(
		HttpServletRequest request,
		HttpServletResponse response,
		AuthenticationException authException
	) throws IOException {
		StringBuilder challenge = new StringBuilder("Bearer realm=\"aurum-mcp\", resource_metadata=\"")
			.append(settings.metadataUrl())
			.append("\"");
		if (!scopes.all().isEmpty()) {
			challenge.append(", scope=\"").append(String.join(" ", scopes.all())).append("\"");
		}

		response.setHeader("WWW-Authenticate", challenge.toString());
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response
			.getWriter()
			.write("{\"error\":\"invalid_token\",\"error_description\":\"Bearer token required\"}");
	}
}
