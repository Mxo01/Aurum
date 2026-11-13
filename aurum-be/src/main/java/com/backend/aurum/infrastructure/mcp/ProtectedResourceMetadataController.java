package com.backend.aurum.infrastructure.mcp;

import io.swagger.v3.oas.annotations.Hidden;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
public class ProtectedResourceMetadataController {

	private final McpSettings settings;
	private final McpScopes scopes;

	public ProtectedResourceMetadataController(McpSettings settings, McpScopes scopes) {
		this.settings = settings;
		this.scopes = scopes;
	}

	/**
	 * OAuth 2.0 Protected Resource Metadata (RFC 9728).
	 *
	 * <p>Served both at the root and at the MCP endpoint's well-known path. MCP
	 * clients normally use the URL from the {@code WWW-Authenticate} challenge, but
	 * the fallback path is part of the discovery contract.
	 */
	@GetMapping(
		{ "/.well-known/oauth-protected-resource", "/.well-known/oauth-protected-resource/mcp" }
	)
	public Map<String, Object> protectedResourceMetadata() {
		Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.put("resource", settings.resourceUrl());
		metadata.put("authorization_servers", List.of(settings.issuer()));
		if (!scopes.all().isEmpty()) {
			metadata.put("scopes_supported", scopes.all());
		}
		metadata.put("bearer_methods_supported", List.of("header"));
		return metadata;
	}
}
