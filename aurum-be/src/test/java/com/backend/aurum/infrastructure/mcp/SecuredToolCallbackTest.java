package com.backend.aurum.infrastructure.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.security.access.AccessDeniedException;

class SecuredToolCallbackTest {

	@Test
	void checksTheScopeBeforeCallingTheDelegate() {
		// GIVEN
		ToolCallback delegate = mock(ToolCallback.class);
		McpToolAuthorization authorization = mock(McpToolAuthorization.class);
		when(delegate.getToolDefinition()).thenReturn(
			ToolDefinition.builder().name("getAssets").description("d").inputSchema("{}").build()
		);
		when(delegate.call("{}")).thenReturn("ok");

		// WHEN
		String result = new SecuredToolCallback(delegate, authorization).call("{}");

		// THEN
		assertThat(result).isEqualTo("ok");
		verify(authorization).check("getAssets");
	}

	@Test
	void doesNotCallTheDelegateWhenTheScopeIsMissing() {
		// GIVEN
		ToolCallback delegate = mock(ToolCallback.class);
		McpToolAuthorization authorization = mock(McpToolAuthorization.class);
		when(delegate.getToolDefinition()).thenReturn(
			ToolDefinition.builder().name("addAsset").description("d").inputSchema("{}").build()
		);
		org.mockito.Mockito.doThrow(new AccessDeniedException("denied"))
			.when(authorization)
			.check("addAsset");

		// WHEN / THEN
		assertThatThrownBy(() ->
			new SecuredToolCallback(delegate, authorization).call("{}")
		).isInstanceOf(AccessDeniedException.class);
		verify(delegate, never()).call("{}");
	}
}
