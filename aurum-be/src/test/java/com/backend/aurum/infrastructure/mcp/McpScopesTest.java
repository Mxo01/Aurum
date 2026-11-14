package com.backend.aurum.infrastructure.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class McpScopesTest {

	private static final String READ_SCOPE = "mcp:read";
	private static final String WRITE_SCOPE = "mcp:write";
	private static final String ASSET_READ_SCOPE = "mcp:assets:read";

	private static final String TOGETHER_MESSAGE =
		McpScopes.READ_PROPERTY + " and " + McpScopes.WRITE_PROPERTY + " must be configured together";

	@ParameterizedTest(name = "{0}")
	@MethodSource("configuredScopePairs")
	void exposesTheConfiguredScopes(
		String scenario,
		String rawRead,
		String rawWrite,
		List<String> expectedRead,
		List<String> expectedWrite,
		List<String> expectedAll
	) {
		// WHEN
		McpScopes testSubject = new McpScopes(rawRead, rawWrite);

		// THEN
		assertThat(testSubject.enforced()).isTrue();
		assertThat(testSubject.read()).containsExactlyInAnyOrderElementsOf(expectedRead);
		assertThat(testSubject.write()).containsExactlyInAnyOrderElementsOf(expectedWrite);
		assertThat(testSubject.all()).isEqualTo(expectedAll);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("unconfiguredScopePairs")
	void leavesBothCategoriesEmptyWhenNothingIsConfigured(
		String scenario,
		String rawRead,
		String rawWrite
	) {
		// WHEN
		McpScopes testSubject = new McpScopes(rawRead, rawWrite);

		// THEN
		assertThat(testSubject.enforced()).isFalse();
		assertThat(testSubject.read()).isEmpty();
		assertThat(testSubject.write()).isEmpty();
		assertThat(testSubject.all()).isEmpty();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidConfigurations")
	void rejectsAnInvalidConfiguration(
		String scenario,
		String rawRead,
		String rawWrite,
		String expectedMessage
	) {
		// THEN
		assertThatThrownBy(() -> new McpScopes(rawRead, rawWrite))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage(expectedMessage);
	}

	private static Stream<Arguments> configuredScopePairs() {
		return Stream.of(
			Arguments.of(
				"a single read and write scope",
				READ_SCOPE,
				WRITE_SCOPE,
				List.of(READ_SCOPE),
				List.of(WRITE_SCOPE),
				List.of(READ_SCOPE, WRITE_SCOPE)
			),
			Arguments.of(
				"surrounding whitespace is trimmed",
				"  " + READ_SCOPE + "  ",
				" " + WRITE_SCOPE + " ",
				List.of(READ_SCOPE),
				List.of(WRITE_SCOPE),
				List.of(READ_SCOPE, WRITE_SCOPE)
			),
			Arguments.of(
				"repeated scopes collapse into one",
				READ_SCOPE + "," + READ_SCOPE + "," + READ_SCOPE,
				WRITE_SCOPE + "," + WRITE_SCOPE,
				List.of(READ_SCOPE),
				List.of(WRITE_SCOPE),
				List.of(READ_SCOPE, WRITE_SCOPE)
			),
			Arguments.of(
				"blank entries between commas are skipped",
				"," + READ_SCOPE + ",",
				WRITE_SCOPE,
				List.of(READ_SCOPE),
				List.of(WRITE_SCOPE),
				List.of(READ_SCOPE, WRITE_SCOPE)
			),
			Arguments.of(
				"a scope shared by both categories is advertised once",
				READ_SCOPE,
				READ_SCOPE,
				List.of(READ_SCOPE),
				List.of(READ_SCOPE),
				List.of(READ_SCOPE)
			),
			Arguments.of(
				"the advertised list is sorted across both categories",
				WRITE_SCOPE,
				ASSET_READ_SCOPE + "," + READ_SCOPE,
				List.of(WRITE_SCOPE),
				List.of(ASSET_READ_SCOPE, READ_SCOPE),
				List.of(ASSET_READ_SCOPE, READ_SCOPE, WRITE_SCOPE)
			)
		);
	}

	private static Stream<Arguments> unconfiguredScopePairs() {
		return Stream.of(
			Arguments.of("empty values", "", ""),
			Arguments.of("null values", null, null),
			Arguments.of("blank values", "   ", "\t")
		);
	}

	private static Stream<Arguments> invalidConfigurations() {
		return Stream.of(
			Arguments.of("read configured and write missing", READ_SCOPE, "", TOGETHER_MESSAGE),
			Arguments.of("write configured and read missing", "", WRITE_SCOPE, TOGETHER_MESSAGE),
			Arguments.of("read missing entirely", null, WRITE_SCOPE, TOGETHER_MESSAGE),
			Arguments.of("read configured and write blank", READ_SCOPE, "   ", TOGETHER_MESSAGE),
			Arguments.of(
				"read holds a space in the token",
				"mcp read",
				WRITE_SCOPE,
				invalidTokenMessage(McpScopes.READ_PROPERTY, "mcp read")
			),
			Arguments.of(
				"read holds a quote in the token",
				"mcp\"read",
				WRITE_SCOPE,
				invalidTokenMessage(McpScopes.READ_PROPERTY, "mcp\"read")
			),
			Arguments.of(
				"read holds a control character in the token",
				"mcp\tread",
				WRITE_SCOPE,
				invalidTokenMessage(McpScopes.READ_PROPERTY, "mcp\tread")
			),
			Arguments.of(
				"write holds a backslash in the token",
				READ_SCOPE,
				"mcp\\write",
				invalidTokenMessage(McpScopes.WRITE_PROPERTY, "mcp\\write")
			),
			Arguments.of(
				"read contains only separators",
				",,,",
				WRITE_SCOPE,
				emptyListMessage(McpScopes.READ_PROPERTY)
			),
			Arguments.of(
				"read contains only whitespace between separators",
				" , ",
				WRITE_SCOPE,
				emptyListMessage(McpScopes.READ_PROPERTY)
			),
			Arguments.of(
				"write contains only separators",
				READ_SCOPE,
				",,,",
				emptyListMessage(McpScopes.WRITE_PROPERTY)
			)
		);
	}

	private static String invalidTokenMessage(String property, String token) {
		return property + " contains an invalid OAuth scope token: " + token;
	}

	private static String emptyListMessage(String property) {
		return property + " must contain at least one scope";
	}
}
