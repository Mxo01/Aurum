package com.backend.aurum.domain.asset.dto;

import com.backend.aurum.domain.asset.model.LiabilityType;
import com.backend.aurum.domain.asset.model.PaymentFrequency;
import com.backend.aurum.domain.user.enums.Currency;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;

@Data
public class UpdateAssetDTO {

	private UUID id;
	private String name;
	private UUID categoryId;
	private Currency originalCurrency;
	private Boolean isFavorite;

	@Schema(
		nullable = true,
		description = "Omit to keep the stored liability; send null to clear the whole liability block"
	)
	private LiabilityType liabilityType;

	@Schema(nullable = true, description = "Only read when liabilityType is provided")
	private PaymentFrequency paymentFrequency;

	@PositiveOrZero
	@Schema(nullable = true, description = "Only read when liabilityType is provided")
	private BigDecimal paymentAmount;

	@JsonIgnore
	@Schema(hidden = true)
	private boolean liabilityTypeProvided;

	public void setLiabilityType(LiabilityType liabilityType) {
		this.liabilityType = liabilityType;
		this.liabilityTypeProvided = true;
	}
}
