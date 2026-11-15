package com.backend.aurum.infrastructure.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.mcp.error.ToolUserError;
import com.backend.aurum.domain.shared.exception.AccessDeniedException;
import com.backend.aurum.domain.shared.exception.NotFoundException;
import com.backend.aurum.domain.shared.exception.ValidationException;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

@ExtendWith(MockitoExtension.class)
class SecuredToolCallbackTest {

	private static final String TOOL_NAME = "getAssets";
	private static final String TOOL_DESCRIPTION = "Get all assets for the current user";
	private static final String INPUT_SCHEMA = "{}";
	private static final String TOOL_INPUT = "{}";
	private static final String TOOL_RESULT = "ok";
	private static final String CONTEXT_KEY = "sessionId";
	private static final String CONTEXT_VALUE = "abc";

	private static final String NOT_AVAILABLE = "This item is not available";
	private static final String FAILED = "The request could not be completed";

	private static final String USER_SENTENCE = "The month must be between 1 and 12";
	private static final String VALIDATION_SENTENCE = "The asset name is required";
	private static final String NOT_FOUND_SENTENCE = "Asset not found";
	private static final String OWNERSHIP_SENTENCE = "This asset does not belong to you";
	private static final String SCOPE_SENTENCE = "Missing MCP scope for tool getAssets: mcp:read";
	private static final String INTERNAL_SENTENCE =
		"could not execute statement; SQL [insert into snapshots (amount) values (?)]";

	@Mock
	private ToolCallback delegate;

	@Mock
	private McpToolAuthorization authorization;

	private ToolDefinition stubbedDefinition;
	private SecuredToolCallback testSubject;

	@BeforeEach
	void setUp() {
		stubbedDefinition = ToolDefinition.builder()
			.name(TOOL_NAME)
			.description(TOOL_DESCRIPTION)
			.inputSchema(INPUT_SCHEMA)
			.build();
		when(delegate.getToolDefinition()).thenReturn(stubbedDefinition);
		testSubject = new SecuredToolCallback(delegate, authorization);
	}

	@Test
	void delegatesTheDefinitionAndTheMetadata() {
		// GIVEN
		ToolMetadata stubbedMetadata = ToolMetadata.builder().returnDirect(true).build();
		when(delegate.getToolMetadata()).thenReturn(stubbedMetadata);

		// THEN
		assertThat(testSubject.getToolDefinition()).isEqualTo(stubbedDefinition);
		assertThat(testSubject.getToolMetadata()).isEqualTo(stubbedMetadata);
	}

	@Test
	void returnsTheToolResultOnceTheScopeCheckPasses() {
		// GIVEN
		when(delegate.call(TOOL_INPUT)).thenReturn(TOOL_RESULT);

		// WHEN
		String expectedResult = testSubject.call(TOOL_INPUT);

		// THEN
		assertThat(expectedResult).isEqualTo(TOOL_RESULT);
		verify(authorization).check(TOOL_NAME);
	}

	@Test
	void passesTheToolContextToTheWrappedTool() {
		// GIVEN
		ToolContext stubbedContext = new ToolContext(Map.of(CONTEXT_KEY, CONTEXT_VALUE));
		when(delegate.call(TOOL_INPUT, stubbedContext)).thenReturn(TOOL_RESULT);

		// WHEN
		String expectedResult = testSubject.call(TOOL_INPUT, stubbedContext);

		// THEN
		assertThat(expectedResult).isEqualTo(TOOL_RESULT);
		verify(authorization).check(TOOL_NAME);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("normalisedFailures")
	void answersOnlyACuratedSentenceForEveryFailure(
		String scenario,
		CuratedFailure failure,
		String expectedMessage
	) {
		// GIVEN
		RuntimeException stubbedFailure = failure.thrown();
		when(delegate.call(TOOL_INPUT)).thenThrow(stubbedFailure);

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.call(TOOL_INPUT))
			.isNotSameAs(stubbedFailure)
			.isInstanceOf(RuntimeException.class)
			.hasMessage(expectedMessage)
			.hasNoCause();
	}

	private static Stream<Arguments> normalisedFailures() {
		return Stream.of(
			Arguments.of(
				"a curated user error keeps its sentence",
				(CuratedFailure) () -> new ToolUserError(USER_SENTENCE),
				USER_SENTENCE
			),
			Arguments.of(
				"a validation error keeps its sentence",
				(CuratedFailure) () -> new ValidationException(VALIDATION_SENTENCE),
				VALIDATION_SENTENCE
			),
			Arguments.of(
				"a missing item answers the shared sentence",
				(CuratedFailure) () -> new NotFoundException(NOT_FOUND_SENTENCE),
				NOT_AVAILABLE
			),
			Arguments.of(
				"an ownership denial answers the same shared sentence",
				(CuratedFailure) () -> new AccessDeniedException(OWNERSHIP_SENTENCE),
				NOT_AVAILABLE
			),
			Arguments.of(
				"a persistence failure answers the generic sentence",
				(CuratedFailure) () -> new IllegalStateException(INTERNAL_SENTENCE),
				FAILED
			),
			Arguments.of(
				"any other runtime failure answers the generic sentence",
				(CuratedFailure) () -> new UnsupportedOperationException(INTERNAL_SENTENCE),
				FAILED
			)
		);
	}

	@Test
	void keepsTheAuthorisationSentenceWhenTheScopeIsMissing() {
		// GIVEN
		org.springframework.security.access.AccessDeniedException stubbedDenial =
			new org.springframework.security.access.AccessDeniedException(SCOPE_SENTENCE);
		doThrow(stubbedDenial).when(authorization).check(TOOL_NAME);

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.call(TOOL_INPUT))
			.isNotSameAs(stubbedDenial)
			.isNotInstanceOf(org.springframework.security.access.AccessDeniedException.class)
			.hasMessage(SCOPE_SENTENCE)
			.hasNoCause();
		verify(delegate, never()).call(TOOL_INPUT);
	}

	@FunctionalInterface
	private interface CuratedFailure {
		RuntimeException thrown();
	}
}
