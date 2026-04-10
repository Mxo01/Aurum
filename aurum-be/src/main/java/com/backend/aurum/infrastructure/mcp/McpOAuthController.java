package com.backend.aurum.infrastructure.mcp;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/oauth")
public class McpOAuthController {

	@Value("${mcp.auth0-client-id}")
	private String auth0ClientId;

	@Value("${mcp.resource-url}")
	private String resourceUrl;

	@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
	private String issuerUri;

	@Value("${spring.security.oauth2.resourceserver.jwt.audiences}")
	private String audience;

	private record PendingCallback(String redirectUri, Instant createdAt) {}

	/** state → original redirect_uri, TTL 10 minutes */
	private final ConcurrentHashMap<String, PendingCallback> pendingCallbacks =
		new ConcurrentHashMap<>();

	/**
	 * Proxy for the Auth0 authorization endpoint.
	 *
	 * <p>Saves the MCP client's random-port redirect_uri (keyed by state), replaces it
	 * with our fixed /oauth/callback URL, and injects the audience required by Auth0 to
	 * issue a signed JWT access token.
	 */
	@RequestMapping("/authorize")
	public void authorize(HttpServletRequest request, HttpServletResponse response)
		throws IOException {
		String state = request.getParameter("state");
		String originalRedirectUri = request.getParameter("redirect_uri");

		if (state != null && originalRedirectUri != null) {
			pendingCallbacks.put(state, new PendingCallback(originalRedirectUri, Instant.now()));
		}

		Map<String, String[]> params = new LinkedHashMap<>(request.getParameterMap());
		params.put("redirect_uri", new String[] { resourceUrl + "/oauth/callback" });
		params.put("audience", new String[] { audience });

		String query = params
			.entrySet()
			.stream()
			.flatMap(e ->
				java.util.Arrays.stream(e.getValue()).map(
					v ->
						URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) +
						"=" +
						URLEncoder.encode(v, StandardCharsets.UTF_8)
				)
			)
			.collect(java.util.stream.Collectors.joining("&"));

		response.sendRedirect(issuerUri + "authorize?" + query);
	}

	/**
	 * Fixed callback URL registered in Auth0.
	 *
	 * <p>Receives the authorization code from Auth0 and forwards it to the MCP client's
	 * original random-port redirect_uri, looked up by the state parameter.
	 */
	@GetMapping("/callback")
	public void callback(
		@RequestParam(required = false) String code,
		@RequestParam(required = false) String error,
		@RequestParam(required = false) String errorDescription,
		@RequestParam String state,
		HttpServletResponse response
	) throws IOException {
		PendingCallback pending = pendingCallbacks.remove(state);
		if (pending == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown or expired state");
			return;
		}

		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(
			pending.redirectUri()
		).queryParam("state", state);

		if (code != null) builder.queryParam("code", code);
		if (error != null) {
			builder.queryParam("error", error);
			if (errorDescription != null) builder.queryParam("error_description", errorDescription);
		}

		response.sendRedirect(builder.build().toUriString());
	}

	/**
	 * Dynamic Client Registration proxy (RFC 7591).
	 *
	 * <p>Auth0 does not support anonymous DCR. Returns the pre-registered public Auth0
	 * client_id for the Aurum MCP Native application.
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

	@Scheduled(fixedRateString = "PT5M")
	public void evictExpiredPendingCallbacks() {
		Instant cutoff = Instant.now().minusSeconds(600);
		pendingCallbacks.entrySet().removeIf(e -> e.getValue().createdAt().isBefore(cutoff));
	}
}
