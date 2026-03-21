package com.backend.aurum.domain.asset.dto;

import com.backend.aurum.domain.asset.model.AssetType;
import com.backend.aurum.domain.asset.model.LiabilityType;
import com.backend.aurum.domain.asset.model.PaymentFrequency;
import com.backend.aurum.domain.user.enums.Currency;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class AssetDTO {

	private UUID id;
	private String name;
	private UUID categoryId;
	private String categoryName;
	private String categoryIcon;
	private AssetType type;
	private Currency originalCurrency;
	private Boolean isActive;
	private Boolean isFavorite;
	private List<SnapshotDTO> snapshots;
	private BigDecimal initialValue;
	private LocalDate referenceDate;
	private LiabilityType liabilityType;
	private PaymentFrequency paymentFrequency;
	private BigDecimal paymentAmount;
}
