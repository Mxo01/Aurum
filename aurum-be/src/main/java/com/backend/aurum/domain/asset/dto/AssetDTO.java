package com.backend.aurum.domain.asset.dto;

import com.backend.aurum.domain.asset.model.AssetType;
import com.backend.aurum.domain.asset.model.LiabilityType;
import com.backend.aurum.domain.asset.model.PaymentFrequency;
import com.backend.aurum.domain.user.enums.Currency;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
@Schema(
	requiredProperties = {
		"id",
		"name",
		"categoryId",
		"categoryName",
		"categoryIcon",
		"type",
		"originalCurrency",
		"isFavorite",
		"snapshots",
		"latestValue",
		"latestValueBase",
		"previousValue",
		"initialValue",
		"referenceDate",
		"liabilityType",
		"paymentFrequency",
		"paymentAmount"
	}
)
public class AssetDTO {

	private UUID id;
	private String name;
	private UUID categoryId;
	private String categoryName;

	@Schema(nullable = true)
	private String categoryIcon;

	private AssetType type;
	private Currency originalCurrency;
	private Boolean isFavorite;
	private List<SnapshotDTO> snapshots;

	@Schema(nullable = true)
	private BigDecimal latestValue;

	@Schema(nullable = true)
	private BigDecimal latestValueBase;

	@Schema(nullable = true)
	private BigDecimal previousValue;

	@Schema(nullable = true)
	private BigDecimal initialValue;

	@Schema(nullable = true)
	private LocalDate referenceDate;

	@Schema(nullable = true)
	private LiabilityType liabilityType;

	@Schema(nullable = true)
	private PaymentFrequency paymentFrequency;

	@Schema(nullable = true)
	private BigDecimal paymentAmount;
}
