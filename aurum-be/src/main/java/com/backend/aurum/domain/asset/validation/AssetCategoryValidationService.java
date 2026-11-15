package com.backend.aurum.domain.asset.validation;

import com.backend.aurum.domain.asset.dto.CreateAssetCategoryDTO;
import com.backend.aurum.domain.asset.dto.UpdateAssetCategoryDTO;
import com.backend.aurum.domain.shared.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AssetCategoryValidationService {

	public void validate(CreateAssetCategoryDTO dto) {
		if (dto == null) {
			throw new ValidationException(null, "Category data is required");
		}
		if (!StringUtils.hasText(dto.getName())) {
			throw new ValidationException("name", "Category name is required");
		}
		if (dto.getName().length() > 100) {
			throw new ValidationException("name", "Category name cannot exceed 100 characters");
		}
		if (dto.getIcon() != null && dto.getIcon().length() > 50) {
			throw new ValidationException("icon", "Category icon cannot exceed 50 characters");
		}
		if (dto.getType() == null) {
			throw new ValidationException("type", "Category type is required");
		}
	}

	public void validate(UpdateAssetCategoryDTO dto) {
		if (dto == null) {
			throw new ValidationException(null, "Category data is required");
		}
		if (!StringUtils.hasText(dto.getName())) {
			throw new ValidationException("name", "Category name is required");
		}
		if (dto.getName().length() > 100) {
			throw new ValidationException("name", "Category name cannot exceed 100 characters");
		}
		if (dto.getIcon() != null && dto.getIcon().length() > 50) {
			throw new ValidationException("icon", "Category icon cannot exceed 50 characters");
		}
		if (dto.getType() == null) {
			throw new ValidationException("type", "Category type is required");
		}
	}
}
