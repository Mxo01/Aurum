package com.backend.aurum.infrastructure.config;

import com.backend.aurum.domain.mcp.tools.AurumMcpTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpConfig {

	@Bean
	public ToolCallbackProvider aurumMcpToolsProvider(AurumMcpTools tools) {
		return MethodToolCallbackProvider.builder().toolObjects(tools).build();
	}
}
