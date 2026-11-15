package com.backend.aurum.infrastructure.mcp;

import com.backend.aurum.domain.mcp.error.ToolUserError;
import com.backend.aurum.domain.shared.exception.NotFoundException;
import com.backend.aurum.domain.shared.exception.ValidationException;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

/**
 * Checks the caller's scope before delegating to the real MCP tool callback, and normalises
 * every failure on the way out.
 *
 * <p>The check runs at invocation time and reads the security context of the HTTP
 * request, so it works with the stateless MCP transport where every request is
 * authenticated independently.
 *
 * <p>The MCP transport answers a failed tool by copying the exception message into the
 * model's context, so an uncaught domain exception is a disclosure: a SQL constraint, a
 * class name, a column list, or the difference between "no such item" and "somebody else's
 * item". Only curated sentences cross this boundary; the original stays in the server log.
 */
@Slf4j
public final class SecuredToolCallback implements ToolCallback {

	private static final String NOT_AVAILABLE = "This item is not available";
	private static final String FAILED = "The request could not be completed";

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
		return invoke(() -> delegate.call(toolInput));
	}

	@Override
	public String call(String toolInput, ToolContext toolContext) {
		return invoke(() -> delegate.call(toolInput, toolContext));
	}

	private String invoke(Supplier<String> invocation) {
		String toolName = getToolDefinition().name();
		try {
			authorization.check(toolName);
			return invocation.get();
		} catch (ToolUserError | ValidationException e) {
			log.warn("MCP tool {} rejected the request: {}", toolName, e.getMessage());
			throw new CuratedToolFailure(e.getMessage());
		} catch (
			NotFoundException
			| com.backend.aurum.domain.shared.exception.AccessDeniedException e
		) {
			// The ownership rule shares one sentence with the missing-item case, so a caller
			// cannot tell an id that does not exist from one that belongs to somebody else.
			log.warn("MCP tool {} denied or found no item: {}", toolName, e.getMessage());
			throw new CuratedToolFailure(NOT_AVAILABLE);
		} catch (org.springframework.security.access.AccessDeniedException e) {
			// Thrown by McpToolAuthorization before the delegate runs. Its sentence names the
			// tool and the scope the token lacks, never a stored value, so it crosses as written:
			// it is actionable for the caller without revealing anything it does not already own.
			log.warn("MCP tool {} denied for a missing scope: {}", toolName, e.getMessage());
			throw new CuratedToolFailure(e.getMessage());
		} catch (RuntimeException e) {
			log.warn("MCP tool {} failed", toolName, e);
			throw new CuratedToolFailure(FAILED);
		}
	}

	/**
	 * Carries a curated sentence and nothing else: no cause, so no later layer can format an
	 * internal message into the model's context.
	 */
	private static final class CuratedToolFailure extends RuntimeException {

		CuratedToolFailure(String message) {
			super(message);
		}
	}
}
