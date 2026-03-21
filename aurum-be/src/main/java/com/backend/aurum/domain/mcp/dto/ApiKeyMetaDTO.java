package com.backend.aurum.domain.mcp.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ApiKeyMetaDTO(
	UUID id,
	String name,
	LocalDateTime createdAt,
	LocalDateTime lastUsedAt
) {}
