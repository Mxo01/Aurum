package com.backend.aurum.domain.asset.dto;

import com.backend.aurum.domain.asset.model.AssetType;
import com.backend.aurum.domain.user.enums.Currency;

import lombok.Data;

import java.util.UUID;

@Data
public class AssetDTO {
	private UUID id;
	private String name;
	private UUID categoryId;
	private String categoryName;
	private AssetType type;
	private Currency originalCurrency;
	private Boolean isActive;
	private Boolean isFavorite;
}
