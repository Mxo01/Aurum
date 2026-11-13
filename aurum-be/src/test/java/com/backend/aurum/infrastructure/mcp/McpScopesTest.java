package com.backend.aurum.infrastructure.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class McpScopesTest {

	@Test
	void noScopesMeansNoEnforcement() {
		McpScopes testSubject = new McpScopes("", "");

		assertThat(testSubject.enforced()).isFalse();
		assertThat(testSubject.all()).isEmpty();
	}

	@Test
	void parsesAndSortsReadAndWriteScopes() {
		McpScopes testSubject = new McpScopes("mcp:read, mcp:read", "mcp:write");

		assertThat(testSubject.enforced()).isTrue();
		assertThat(testSubject.read()).containsExactly("mcp:read");
		assertThat(testSubject.write()).containsExactly("mcp:write");
		assertThat(testSubject.all()).isEqualTo(List.of("mcp:read", "mcp:write"));
	}

	@Test
	void rejectsHalfConfiguredScopes() {
		assertThatThrownBy(() -> new McpScopes("mcp:read", "")).isInstanceOf(
			IllegalStateException.class
		);
		assertThatThrownBy(() -> new McpScopes("", "mcp:write")).isInstanceOf(
			IllegalStateException.class
		);
	}

	@Test
	void rejectsInvalidScopeTokens() {
		assertThatThrownBy(() -> new McpScopes("mcp read", "mcp:write")).isInstanceOf(
			IllegalStateException.class
		);
	}

	@Test
	void rejectsACommaOnlyValue() {
		assertThatThrownBy(() -> new McpScopes(",,,", "mcp:write")).isInstanceOf(
			IllegalStateException.class
		);
	}
}
