package com.backend.aurum.domain.user.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record UserExportDTO(
	ProfileExport profile,
	List<CategoryExport> categories,
	List<AssetExport> assets,
	List<TargetExport> targets
) {
	public record ProfileExport(UUID id, String email, String currency, String locale) {}

	public record CategoryExport(UUID id, String name, String type) {}

	public record SnapshotExport(
		UUID id,
		LocalDate referenceDate,
		BigDecimal amountOriginalCurrency,
		BigDecimal exchangeRateToBase
	) {}

	public record AssetExport(
		UUID id,
		String name,
		String category,
		String originalCurrency,
		Boolean isActive,
		Boolean isFavorite,
		String liabilityType,
		String paymentFrequency,
		BigDecimal paymentAmount,
		LocalDateTime createdAt,
		List<SnapshotExport> snapshots
	) {}

	public record TargetExport(
		UUID id,
		String name,
		BigDecimal targetAmount,
		BigDecimal currentAmount,
		String type,
		LocalDate deadline,
		Boolean isCompleted
	) {}
}
