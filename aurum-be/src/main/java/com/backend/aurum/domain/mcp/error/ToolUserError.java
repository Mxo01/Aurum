package com.backend.aurum.domain.mcp.error;

/**
 * A failure the caller can fix.
 *
 * <p>The message is a curated sentence that reaches the caller unchanged, so it never carries
 * a class name, an id, a library message or a developer verb: the reader is a model that has
 * to be able to correct its own request from it.
 */
public class ToolUserError extends RuntimeException {

	public ToolUserError(String message) {
		super(message);
	}
}
