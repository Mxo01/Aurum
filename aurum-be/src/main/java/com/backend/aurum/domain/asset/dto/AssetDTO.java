package com.backend.aurum.domain.asset.dto;

import com.backend.aurum.domain.asset.model.AssetType;
import lombok.Data;

import java.util.UUID;

@Data
public class AssetDTO {
    private UUID id;
    private UUID userId;
    private String name;
    private UUID categoryId;
    private String categoryName;
    private AssetType type;
    private String originalCurrency;
    private Boolean isActive;
    private Boolean isFavorite;
}
