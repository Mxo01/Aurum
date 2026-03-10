package com.backend.aurum.domain.asset.dto;

import com.backend.aurum.domain.asset.model.AssetType;
import lombok.Data;

import java.util.UUID;

@Data
public class AssetCategoryDTO {
    private UUID id;
    private String name;
    private AssetType type;
}
