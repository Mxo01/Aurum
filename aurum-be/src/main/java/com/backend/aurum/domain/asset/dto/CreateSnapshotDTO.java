package com.backend.aurum.domain.asset.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Data;

@Data
public class CreateSnapshotDTO {

	private UUID assetId;
	private LocalDate referenceDate;
	private BigDecimal amountOriginalCurrency;
	private BigDecimal exchangeRateToBase;
}
