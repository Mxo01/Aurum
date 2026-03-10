package com.backend.aurum.domain.asset.mapper;

import com.backend.aurum.domain.asset.dto.AssetCategoryDTO;
import com.backend.aurum.domain.asset.model.AssetCategory;
import org.springframework.stereotype.Component;

@Component
public class AssetCategoryMapper {

    public AssetCategory toEntity(AssetCategoryDTO dto) {
        if (dto == null) return null;
        AssetCategory category = new AssetCategory();
        category.setId(dto.getId());
        category.setName(dto.getName());
        category.setType(dto.getType());
        return category;
    }

    public AssetCategoryDTO toDto(AssetCategory entity) {
        if (entity == null) return null;
        AssetCategoryDTO dto = new AssetCategoryDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setType(entity.getType());
        return dto;
    }
}
