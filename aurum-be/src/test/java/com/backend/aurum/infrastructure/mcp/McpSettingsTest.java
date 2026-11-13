package com.backend.aurum.infrastructure.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class McpSettingsTest {

	@Test
	void audienceDefaultsToTheResourceUri() {
		McpSettings testSubject = new McpSettings(
			"https://api.test.com/mcp",
			"",
			"https://auth.test.com/"
		);

		assertThat(testSubject.resourceUrl()).isEqualTo("https://api.test.com/mcp");
		assertThat(testSubject.audience()).isEqualTo("https://api.test.com/mcp");
		assertThat(testSubject.metadataUrl()).isEqualTo(
			"https://api.test.com/.well-known/oauth-protected-resource/mcp"
		);
	}

	@Test
	void explicitAudienceIsKept() {
		McpSettings testSubject = new McpSettings(
			"https://api.test.com/mcp",
			"https://mcp.test.com",
			"https://auth.test.com/"
		);

		assertThat(testSubject.audience()).isEqualTo("https://mcp.test.com");
	}

	@Test
	void resourceIsCanonicalizedButTheAudienceIsKeptExactly() {
		McpSettings testSubject = new McpSettings(
			"https://api.test.com/mcp/",
			"https://api.test.com/mcp/",
			"https://auth.test.com/"
		);

		assertThat(testSubject.resourceUrl()).isEqualTo("https://api.test.com/mcp");
		assertThat(testSubject.audience()).isEqualTo("https://api.test.com/mcp/");
	}

	@Test
	void rejectsAnInvalidResource() {
		assertThatThrownBy(() ->
			new McpSettings("not-a-uri", "", "https://auth.test.com/")
		).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsAnInvalidIssuer() {
		assertThatThrownBy(() ->
			new McpSettings("https://api.test.com/mcp", "", "not-a-uri")
		).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsABlankIssuer() {
		assertThatThrownBy(() -> new McpSettings("https://api.test.com/mcp", "", " ")).isInstanceOf(
			IllegalArgumentException.class
		);
	}
}
