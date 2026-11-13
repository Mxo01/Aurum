package com.backend.aurum.infrastructure.mcp;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Enforces the MCP read/write scope split on every tool method.
 *
 * <p>The pointcut is the annotation itself, so a new tool cannot be added without
 * going through the check. {@link McpToolAuthorization} treats a missing scope
 * configuration as "audience-only" and anything that is not a {@code get*} tool as
 * a write.
 */
@Aspect
@Component
public class McpToolScopeAspect {

	private final McpToolAuthorization authorization;

	public McpToolScopeAspect(McpToolAuthorization authorization) {
		this.authorization = authorization;
	}

	@Around("@annotation(org.springframework.ai.mcp.annotation.McpTool)")
	public Object enforce(ProceedingJoinPoint joinPoint) throws Throwable {
		authorization.check(joinPoint.getSignature().getName());
		return joinPoint.proceed();
	}
}
