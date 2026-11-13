package com.backend.aurum.infrastructure.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class McpResourceMetadataTest {

	@Test
	void canonicalResource_stripsTrailingSlash() {
		assertThat(McpResourceMetadata.canonicalResource("https://api.test.com/mcp/")).isEqualTo(
			"https://api.test.com/mcp"
		);
	}

	@Test
	void protectedResourceMetadataUrl_isInsertedBeforeTheResourcePath() {
		assertThat(
			McpResourceMetadata.protectedResourceMetadataUrl("https://api.test.com/mcp")
		).isEqualTo("https://api.test.com/.well-known/oauth-protected-resource/mcp");
	}

	@Test
	void protectedResourceMetadataUrl_usesRootWhenResourceHasNoPath() {
		assertThat(McpResourceMetadata.protectedResourceMetadataUrl("https://api.test.com")).isEqualTo(
			"https://api.test.com/.well-known/oauth-protected-resource"
		);
	}

	@Test
	void protectedResourceMetadataUrl_keepsThePort() {
		assertThat(
			McpResourceMetadata.protectedResourceMetadataUrl("http://localhost:8080/mcp")
		).isEqualTo("http://localhost:8080/.well-known/oauth-protected-resource/mcp");
	}

	@Test
	void resourceUri_rejectsRelativeUri() {
		assertThatThrownBy(() -> McpResourceMetadata.resourceUri("/mcp")).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	@Test
	void resourceUri_rejectsQueryAndFragment() {
		assertThatThrownBy(() ->
			McpResourceMetadata.resourceUri("https://api.test.com/mcp?x=1")
		).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() ->
			McpResourceMetadata.resourceUri("https://api.test.com/mcp#fragment")
		).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void resourceUri_rejectsUserInfo() {
		assertThatThrownBy(() ->
			McpResourceMetadata.resourceUri("https://user:pass@api.test.com/mcp")
		).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void resourceUri_rejectsBlank() {
		assertThatThrownBy(() -> McpResourceMetadata.resourceUri(" ")).isInstanceOf(
			IllegalArgumentException.class
		);
	}
}
