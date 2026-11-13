package com.backend.aurum.infrastructure.mcp;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

/**
 * Checks the caller's scope before delegating to the real MCP tool callback.
 *
 * <p>The check runs at invocation time and reads the security context of the HTTP
 * request, so it works with the stateless MCP transport where every request is
 * authenticated independently.
 */
public final class SecuredToolCallback implements ToolCallback {

	private final ToolCallback delegate;
	private final McpToolAuthorization authorization;

	public SecuredToolCallback(ToolCallback delegate, McpToolAuthorization authorization) {
		this.delegate = delegate;
		this.authorization = authorization;
	}

	@Override
	public ToolDefinition getToolDefinition() {
		return delegate.getToolDefinition();
	}

	@Override
	public ToolMetadata getToolMetadata() {
		return delegate.getToolMetadata();
	}

	@Override
	public String call(String toolInput) {
		authorization.check(getToolDefinition().name());
		return delegate.call(toolInput);
	}

	@Override
	public String call(String toolInput, ToolContext toolContext) {
		authorization.check(getToolDefinition().name());
		return delegate.call(toolInput, toolContext);
	}
}
