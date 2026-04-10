package com.backend.aurum.infrastructure.mcp;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class McpAuthorizationServerMetadataController {

	@Value("${mcp.resource-url}")
	private String resourceUrl;

	@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
	private String issuerUri;

	/**
	 * OAuth 2.0 Authorization Server Metadata (RFC 8414).
	 *
	 * <p>Exposes our backend as the "issuer" for MCP client discovery. The actual
	 * authentication happens at Auth0 — we proxy the authorize endpoint (to inject
	 * the audience claim) and return a pre-registered client_id for DCR.
	 */
	@GetMapping("/.well-known/oauth-authorization-server")
	public Map<String, Object> authorizationServerMetadata() {
		return Map.of(
			"issuer",
			resourceUrl,
			"authorization_endpoint",
			resourceUrl + "/oauth/authorize",
			"token_endpoint",
			issuerUri + "oauth/token",
			"registration_endpoint",
			resourceUrl + "/oauth/register",
			"response_types_supported",
			List.of("code"),
			"grant_types_supported",
			List.of("authorization_code", "refresh_token"),
			"code_challenge_methods_supported",
			List.of("S256"),
			"token_endpoint_auth_methods_supported",
			List.of("none")
		);
	}
}
