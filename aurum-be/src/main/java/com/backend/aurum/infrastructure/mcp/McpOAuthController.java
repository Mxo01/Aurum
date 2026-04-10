package com.backend.aurum.infrastructure.mcp;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/oauth")
public class McpOAuthController {

	@Value("${mcp.auth0-client-id}")
	private String auth0ClientId;

	@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
	private String issuerUri;

	@Value("${spring.security.oauth2.resourceserver.jwt.audiences}")
	private String audience;

	/**
	 * Proxy for the Auth0 authorization endpoint.
	 *
	 * <p>Injects the {@code audience} parameter required by Auth0 to issue a signed
	 * JWT access token instead of an opaque token. Without it, Auth0 would issue an
	 * opaque token that our JwtDecoder cannot validate.
	 */
	@RequestMapping("/authorize")
	public void authorize(HttpServletRequest request, HttpServletResponse response)
		throws IOException {
		String queryString = request.getQueryString();
		String auth0AuthorizeUrl = issuerUri + "authorize?" + queryString + "&audience=" + audience;
		response.sendRedirect(auth0AuthorizeUrl);
	}

	/**
	 * Dynamic Client Registration proxy (RFC 7591).
	 *
	 * <p>Auth0 does not support anonymous DCR. This endpoint accepts any registration
	 * request from an MCP client and returns the pre-registered public Auth0 client_id.
	 * The actual OAuth flow (authorize + token exchange) still goes directly to Auth0.
	 */
	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	@SuppressWarnings("unchecked")
	public Map<String, Object> register(@RequestBody Map<String, Object> body) {
		List<String> redirectUris = body.containsKey("redirect_uris")
			? (List<String>) body.get("redirect_uris")
			: List.of();

		return Map.of(
			"client_id",
			auth0ClientId,
			"client_id_issued_at",
			Instant.now().getEpochSecond(),
			"token_endpoint_auth_method",
			"none",
			"grant_types",
			List.of("authorization_code", "refresh_token"),
			"response_types",
			List.of("code"),
			"redirect_uris",
			redirectUris
		);
	}
}
