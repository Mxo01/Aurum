package com.backend.aurum.domain.asset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Data;

@Data
@Schema(
	requiredProperties = {
		"id",
		"assetId",
		"assetName",
		"referenceDate",
		"amountOriginalCurrency",
		"exchangeRateToBase",
		"amountBaseCurrency"
	}
)
public class SnapshotDTO {

	@Schema(nullable = true)
	private UUID id;

	private UUID assetId;

	@Schema(nullable = true)
	private String assetName;

	private LocalDate referenceDate;
	private BigDecimal amountOriginalCurrency;

	@Schema(nullable = true)
	private BigDecimal exchangeRateToBase;

	@Schema(nullable = true)
	private BigDecimal amountBaseCurrency;
}
