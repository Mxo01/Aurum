package com.backend.aurum.infrastructure.config;

import com.backend.aurum.domain.mcp.tools.AurumMcpTools;
import com.backend.aurum.infrastructure.mcp.McpToolAuthorization;
import com.backend.aurum.infrastructure.mcp.SecuredToolCallback;
import java.util.Arrays;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpConfig {

	@Bean
	public ToolCallbackProvider aurumMcpToolsProvider(
		AurumMcpTools tools,
		McpToolAuthorization authorization
	) {
		ToolCallbackProvider delegate = MethodToolCallbackProvider.builder().toolObjects(tools).build();

		return new ToolCallbackProvider() {
			@Override
			public ToolCallback[] getToolCallbacks() {
				return Arrays.stream(delegate.getToolCallbacks())
					.map(callback -> new SecuredToolCallback(callback, authorization))
					.toArray(ToolCallback[]::new);
			}
		};
	}
}
