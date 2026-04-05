package com.backend.aurum.domain.asset.validation;

import com.backend.aurum.domain.asset.dto.CreateAssetCategoryDTO;
import com.backend.aurum.domain.asset.dto.UpdateAssetCategoryDTO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AssetCategoryValidationService {

	public void validate(CreateAssetCategoryDTO dto) {
		if (dto == null) {
			throw new IllegalArgumentException("Category data cannot be null");
		}
		if (!StringUtils.hasText(dto.getName())) {
			throw new IllegalArgumentException("Category name is required");
		}
		if (dto.getName().length() > 100) {
			throw new IllegalArgumentException("Category name must not exceed 100 characters");
		}
		if (dto.getIcon() != null && dto.getIcon().length() > 50) {
			throw new IllegalArgumentException("Category icon must not exceed 50 characters");
		}
		if (dto.getType() == null) {
			throw new IllegalArgumentException("Category type is required");
		}
	}

	public void validate(UpdateAssetCategoryDTO dto) {
		if (dto == null) {
			throw new IllegalArgumentException("Category data cannot be null");
		}
		if (!StringUtils.hasText(dto.getName())) {
			throw new IllegalArgumentException("Category name is required");
		}
		if (dto.getName().length() > 100) {
			throw new IllegalArgumentException("Category name must not exceed 100 characters");
		}
		if (dto.getIcon() != null && dto.getIcon().length() > 50) {
			throw new IllegalArgumentException("Category icon must not exceed 50 characters");
		}
		if (dto.getType() == null) {
			throw new IllegalArgumentException("Category type is required");
		}
	}
}
