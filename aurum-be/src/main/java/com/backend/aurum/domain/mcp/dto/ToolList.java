package com.backend.aurum.domain.mcp.dto;

import java.util.List;

/**
 * A bounded tool result.
 *
 * <p>{@code totalCount} always reports the full number of items the caller owns, so a model
 * that received {@code truncated: true} knows the list is partial and can narrow its question
 * instead of believing it has seen everything.
 */
public record ToolList<T>(List<T> items, int totalCount, boolean truncated) {
	public static <T> ToolList<T> cappedAt(List<T> all, int maxSize) {
		if (all.size() <= maxSize) {
			return new ToolList<>(all, all.size(), false);
		}
		return new ToolList<>(List.copyOf(all.subList(0, maxSize)), all.size(), true);
	}
}
