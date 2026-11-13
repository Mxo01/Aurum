package com.backend.aurum.infrastructure.mcp;

import java.net.URI;
import java.net.URISyntaxException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * The MCP resource server's own identity: the canonical resource URI, the audience
 * the access token must be issued for, and the authorization server issuer.
 *
 * <p>The audience is the Auth0 API identifier. It defaults to the resource URI, which
 * is the value Auth0 recommends and the only value that makes RFC 8707 audience
 * binding meaningful. It is kept configurable for tenants that cannot use the URL as
 * the API identifier.
 */
@Component
public class McpSettings {

	private final String resourceUrl;
	private final String audience;
	private final String issuer;
	private final String metadataUrl;

	public McpSettings(
		@Value("${mcp.resource-url}") String resourceUrl,
		@Value("${mcp.audience:}") String audience,
		@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer
	) {
		this.resourceUrl = McpResourceMetadata.canonicalResource(resourceUrl);
		// The audience must match the token's `aud` claim byte for byte, so it is
		// validated but never normalized. Only the resource URI is canonicalized.
		this.audience = StringUtils.hasText(audience)
			? McpResourceMetadata.resourceUri(audience).toString()
			: this.resourceUrl;
		this.issuer = canonicalIssuer(issuer);
		this.metadataUrl = McpResourceMetadata.protectedResourceMetadataUrl(this.resourceUrl);
	}

	private static String canonicalIssuer(String issuer) {
		if (!StringUtils.hasText(issuer)) {
			throw new IllegalArgumentException("Authorization server issuer must be configured");
		}

		URI uri;
		try {
			uri = new URI(issuer.trim());
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Authorization server issuer is not a valid URI", e);
		}

		if (
			!uri.isAbsolute() ||
			uri.getHost() == null ||
			uri.getQuery() != null ||
			uri.getFragment() != null
		) {
			throw new IllegalArgumentException(
				"Authorization server issuer must be an absolute http(s) URI"
			);
		}

		return uri.toString();
	}

	public String resourceUrl() {
		return resourceUrl;
	}

	public String audience() {
		return audience;
	}

	public String issuer() {
		return issuer;
	}

	public String metadataUrl() {
		return metadataUrl;
	}
}
