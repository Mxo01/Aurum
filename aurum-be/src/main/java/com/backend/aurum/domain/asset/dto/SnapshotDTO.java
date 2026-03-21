package com.backend.aurum.domain.asset.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Data;

@Data
public class SnapshotDTO {

	private UUID id;
	private UUID assetId;
	private String assetName;
	private LocalDate referenceDate;
	private BigDecimal amountOriginalCurrency;
	private BigDecimal exchangeRateToBase;
	private BigDecimal amountBaseCurrency;
}
