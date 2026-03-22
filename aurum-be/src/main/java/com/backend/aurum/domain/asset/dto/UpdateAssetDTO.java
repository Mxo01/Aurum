package com.backend.aurum.domain.asset.dto;

import com.backend.aurum.domain.asset.model.LiabilityType;
import com.backend.aurum.domain.asset.model.PaymentFrequency;
import com.backend.aurum.domain.user.enums.Currency;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;

@Data
public class UpdateAssetDTO {

	private UUID id;
	private String name;
	private UUID categoryId;
	private Currency originalCurrency;
	private Boolean isActive;
	private Boolean isFavorite;
	private LiabilityType liabilityType;
	private PaymentFrequency paymentFrequency;
	private BigDecimal paymentAmount;
}
