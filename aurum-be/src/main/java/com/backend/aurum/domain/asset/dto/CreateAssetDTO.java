package com.backend.aurum.domain.asset.dto;

import com.backend.aurum.domain.asset.model.LiabilityType;
import com.backend.aurum.domain.asset.model.PaymentFrequency;
import com.backend.aurum.domain.user.enums.Currency;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Data;

@Data
public class CreateAssetDTO {

	private String name;
	private UUID categoryId;
	private Currency originalCurrency;
	private Boolean isFavorite;
	private BigDecimal initialValue;
	private LocalDate referenceDate;
	private LiabilityType liabilityType;
	private PaymentFrequency paymentFrequency;
	private BigDecimal paymentAmount;
}
