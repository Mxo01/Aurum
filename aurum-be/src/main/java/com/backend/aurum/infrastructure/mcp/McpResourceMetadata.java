package com.backend.aurum.infrastructure.mcp;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Canonical MCP resource identifiers and their metadata URL.
 *
 * <p>RFC 9728 places the protected resource metadata document between the host and
 * the resource path: the resource {@code https://host/mcp} is described at
 * {@code https://host/.well-known/oauth-protected-resource/mcp}. The URL is derived
 * from the configured resource instead of being hardcoded, so a different MCP path
 * cannot silently advertise the wrong document.
 */
public final class McpResourceMetadata {

	private McpResourceMetadata() {}

	public static URI resourceUri(String resourceUrl) {
		if (resourceUrl == null || resourceUrl.isBlank()) {
			throw new IllegalArgumentException("mcp.resource-url must be configured");
		}

		URI uri;
		try {
			uri = new URI(resourceUrl.trim());
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("mcp.resource-url is not a valid URI", e);
		}

		if (
			!uri.isAbsolute() ||
			uri.getHost() == null ||
			uri.getUserInfo() != null ||
			uri.getQuery() != null ||
			uri.getFragment() != null
		) {
			throw new IllegalArgumentException(
				"mcp.resource-url must be an absolute http(s) URI without userinfo, query or fragment"
			);
		}

		String scheme = uri.getScheme().toLowerCase(java.util.Locale.ROOT);
		if (!scheme.equals("http") && !scheme.equals("https")) {
			throw new IllegalArgumentException("mcp.resource-url must use http or https");
		}

		return uri;
	}

	public static String canonicalResource(String resourceUrl) {
		URI uri = resourceUri(resourceUrl);
		String path = normalizedPath(uri);
		return (
			uri.getScheme().toLowerCase(java.util.Locale.ROOT) + "://" + uri.getRawAuthority() + path
		);
	}

	private static String normalizedPath(URI uri) {
		String path = uri.getPath() == null ? "" : uri.getPath();
		if (path.equals("/")) {
			return "";
		}
		if (path.endsWith("/")) {
			return path.substring(0, path.length() - 1);
		}
		return path;
	}

	public static String protectedResourceMetadataUrl(String resourceUrl) {
		URI uri = resourceUri(resourceUrl);
		String path = normalizedPath(uri);
		return (
			uri.getScheme().toLowerCase(java.util.Locale.ROOT) +
			"://" +
			uri.getRawAuthority() +
			"/.well-known/oauth-protected-resource" +
			path
		);
	}
}
