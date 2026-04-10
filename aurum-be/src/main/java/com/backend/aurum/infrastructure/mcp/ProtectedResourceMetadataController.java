package com.backend.aurum.infrastructure.mcp;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProtectedResourceMetadataController {

	@Value("${mcp.resource-url}")
	private String resourceUrl;

	@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
	private String issuerUri;

	@GetMapping("/.well-known/oauth-protected-resource")
	public Map<String, Object> protectedResourceMetadata() {
		return Map.of(
			"resource",
			resourceUrl,
			"authorization_servers",
			List.of(resourceUrl),
			"scopes_supported",
			List.of("openid", "profile"),
			"bearer_methods_supported",
			List.of("header")
		);
	}
}
