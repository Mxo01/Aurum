package com.backend.aurum.domain.asset.validation;

import com.backend.aurum.domain.asset.dto.AssetCategoryDTO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AssetCategoryValidationService {

    public void validate(AssetCategoryDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Category data cannot be null");
        }
        if (!StringUtils.hasText(dto.getName())) {
            throw new IllegalArgumentException("Category name is required");
        }
        if (dto.getType() == null) {
            throw new IllegalArgumentException("Category type is required");
        }
    }
}
